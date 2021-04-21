import scala.io.StdIn
import scala.util.matching.Regex
import scala.util.Failure
import scala.util.Success
import scala.util.Random

class Cli(var dbc: DatabaseConnection) {
  val rng: Random = new Random()
  val parse_RE: Regex = raw"(\S+)\s*(.*)".r
  var pokedex: Vector[Pokemon] = null
  var player: Player = null

  /** The main function to run the CLI
    */
  def run(): Unit = {
    println("Welcome to the Pokémon catching simulator!")
    try {
      dbc.connect()
      var csv_load_successful = false
      if (!dbc.check_if_pokemon_table_is_populated()) {
        FileIO.read_pokedex(Cli.pokedex_file_location) match {
          case Failure(exception) =>
            println(s"Error loading Pokédex from CSV: $exception")
          case Success(loaded_value) =>
            pokedex = loaded_value
            csv_load_successful = true
        }
        dbc.store_pokedex(pokedex)
        println("Retrieved data from CSV!")
      } else {
        dbc.load_pokedex() match {
          case Failure(exception) =>
            println(s"Error loading Pokédex from SQL database: $exception")
          case Success(loaded_value) =>
            pokedex = loaded_value; csv_load_successful = true
        }
        println("Retrieved data from Postgres Database!")
      }

      if (csv_load_successful) {
        println()
        print("Please enter your name: ")
        var input: String = StdIn.readLine().trim()
        //TODO: if your name is already in the database, confirm link to old account
        //and also maybe list out all existing players

        dbc.get_player(input) match {
          case Failure(exception) =>
            println(s"Exception occurred! $exception")
          case Success(Some(value)) =>
            player = value
            println()
            println(s"Welcome back, $input!")
            println(s"Here's your current status:")
            println()
            trainer_card()
            println()
            main_loop()
          case Success(None) =>
            player = new Player(input, Cli.default_num_balls)
            dbc.add_player(player)
            println()
            println(s"Welcome to the Pokémon Safari, $input!")
            println(s"Here you can go around and catch any Pokémon you see!")
            println(
              s"Let me get you ${Cli.default_num_balls} Pokéballs to start."
            )
            println(
              s"If you need any more at any point, check out the Pokémart."
            )
            println(s"Also, I've printed out a Trainer Card for you.")
            println(s"It'll automatically keep track of your progress.")
            println(s"Have a good time!")
            println()
            trainer_card()
            println()
            main_loop()
        }
      }
    } finally {
      dbc.disconnect()
    }
  }

  /** main gameplay loop
    */
  private def main_loop(): Unit = {
    var do_exit: Boolean = false

    while (!do_exit) {
      println("You're standing in the Pokémon Safari Welcome Center.")
      println("For a full list of commands, type `help`")
      println("What would you like to do?")
      print(">> ")
      var input = StdIn.readLine()
      parse(input) match {
        case Some(("help", "")) => {
          println()
          println("`help`: Show this help prompt")
          println("`safari`: Go on a safari where all Pokémon are available")
          println(
            "`safari [Type]`: Go on a safari where the only Pokémon you can catch are of that type. The type name must be capitalized"
          )
          println(
            "`pokémart`: get more Pokéballs (also aliased as `pokemart` and `mart`)"
          )
          println(
            "`status`: show your trainer card (also aliased as `trainer card` and `trainer`)"
          )
          println(
            "`pokédex`: explore info on your caught Pokémon (also aliased as `pokedex`, `pokémon`, and `pokemon`)"
          )
          println("`exit`: exit the game")
        }
        case Some(("safari", "")) => println("\n"); run_safari("General")
        case Some(("safari", pkmn_type)) => {
          if (Cli.pokemon_types.contains(pkmn_type)) {
            println("\n")
            run_safari(
              pkmn_type,
              pkmn =>
                (pkmn.type_1.getOrElse("") == pkmn_type) || (pkmn.type_2
                  .getOrElse("") == pkmn_type)
            )
          } else {
            println(s"type $pkmn_type not recognized")
          }
        }
        case Some(("pokemart", "")) | Some(("pokémart", "")) | Some(
              ("mart", "")
            ) =>
          println(); pokemart()
        case Some(("status", "")) | Some(("trainer", "")) | Some(
              ("trainer", "card")
            ) =>
          println(); trainer_card()
        case Some(("pokedex", "")) | Some(("pokédex", "")) | Some(
              ("pokémon", "")
            ) | Some(("pokemon", "")) =>
          println(); start_pokedex()
        case Some(("exit", _)) => {
          println("Goodbye!")
          do_exit = true
        }
        case Some((_, _)) | None =>
          println("command not understood")
      }
      println()
    }
  }

  /** safari loop
    *
    * @param safari_name
    * @param predicate
    */
  private def run_safari(
      safari_name: String,
      predicate: Pokemon => Boolean = _ => true
  ): Unit = {
    var do_exit: Boolean = false
    println(s"Welcome to the $safari_name Pokémon Safari!")
    println(
      "At any time during the safari, type `help` for a full list of commands."
    )
    var pkmn: Pokemon = null
    get_random_pokemon(predicate) match {
      case Some(pokemon) => {
        pkmn = pokemon
      }
      case None => {
        // No Pokémon exist under this filter
        println(
          "Hmm... you can't seem to find any Pokémon in this safari. Returning..."
        )
        return ()
      }
    }
    while (!do_exit) {
      if (player.num_balls <= 0) {
        println(
          "You have no more Pokéballs left! Let's head back to the welcome center."
        )
        do_exit = true
      } else {
        // make the grammar a bit more natural
        // might be slightly off if there are silent consonants or an initial U sounding as "Yoo"
        // But I don't think that applies to any Pokémon that currently exists
        val a_or_an_pokemon: String =
          if (Cli.vowels.contains(pkmn.name(0))) "an" else "a"
        val pokeball_or_pokeballs: String =
          if (player.num_balls == 1) "Pokéball" else "Pokéballs"

        println(
          s"You spot ${a_or_an_pokemon} ${pkmn.name}! What would you like to do?"
        )
        println(s"You have ${player.num_balls} $pokeball_or_pokeballs left.")

        print(">> ")
        var input = StdIn.readLine()
        parse(input) match {
          case Some(("help", "")) => {
            println()
            println(
              "`catch`: use a Pokéball to catch the Pokémon (also aliased as `ball`)"
            )
            println(
              "`flee`: flee from this Pokémon and search for another Pokémon (also aliased as `ignore` and `pass`)"
            )
            println("`leave`: leave this Safari Zone (also aliased as `exit`)")
            println()
          }
          case Some(("catch", _)) | Some(("ball", _)) => {
            println(s"You caught ${a_or_an_pokemon} ${pkmn.name}!")
            player.num_balls -= 1
            // TODO: add this Pokémon to the database
            //because we've already run it once, we can be confident get_random_pokemon isn't None
            pkmn = get_random_pokemon(
              predicate
            ).get
          }
          case Some(("flee", _)) | Some(("ignore", _)) | Some(("pass", _)) => {
            println(s"You ignore the ${pkmn.name} and continue searching")
            //because we've already run it once, we can be confident get_random_pokemon isn't None
            pkmn = get_random_pokemon(
              predicate
            ).get
          }
          case Some(("leave", _)) | Some(("exit", _)) => {
            println(
              s"You decide you've explored enough in the $safari_name Pokémon Safari. You head back to the Welcome Center."
            )
            do_exit = true
          }
          case Some((_, _)) | None =>
            println("command not understood")
        }
      }
      println()
    }
  }

  /** get extra Pokéballs, as necessary */
  private def pokemart(): Unit = {
    println("Welcome to the Safari Zone Pokémart! Do you need extra Pokéballs?")
    if (player.num_balls <= 0) {
      println("Oh, you're completely out! Here, let me get you some!")
      println("(You received 10 Pokéballs!)")
      player.num_balls = Cli.default_num_balls
    } else if (player.num_balls < Cli.default_num_balls - 1) {
      println(
        "You're starting to get a little bit low. I'll get you a few more."
      )
      println(
        s"(You received ${Cli.default_num_balls - player.num_balls} Pokéballs!)"
      )
      player.num_balls = Cli.default_num_balls
    } else {
      println("Hm... It looks like you still have plenty.")
      println("Come back when you start to run low.")
    }
  }

  private def start_pokedex(): Unit = {
    println("NOT YET IMPLEMENTED") 
  }

  /** show trainer card
    */
  private def trainer_card(): Unit = {
    println("============TRAINER CARD============")
    println(s"TRAINER NAME: ${player.name}")
    println(s"NUMBER OF POKÉBALLS: ${player.num_balls}")
    println(s"NUMBER OF POKÉMON CAUGHT: (unknown)") //TODO!
    println("====================================")
  }

  /** run pokedex loop
    */
  private def pokedex_loop(): Unit = {
    println("NOT YET IMPLEMENTED")
  }

  // /** load the Pokedex from a CSV file into memory, printing the status as you go
  //   * returns a boolean of whether or not this was successful
  //   *
  //   * @param pokedex_file_location
  //   * @return
  //   */
  // private def load_csv(pokedex_file_location: String): Boolean = {
  //   println(s"Loading Pokédex from $pokedex_file_location...")
  //   FileIO.read_pokedex(pokedex_file_location) match {
  //     case Failure(exception) => {
  //       println("Error reading file. Exiting...")
  //       false
  //     }
  //     case Success(value) => {
  //       pokedex = value
  //       println("Load successful!")
  //       true
  //     }
  //   }
  // }

  /** Return a random Pokemon from the pokedex, weighted by their catch rate
    * If the parameter predicate is passed, only Pokemon that satisfy the predicate are possible
    * Pokemon without known catch rates cannot be returned
    * If no Pokemon pass the predicate (or if they all have catch rate zero), return None
    *
    * @param predicate
    * @return
    */
  private def get_random_pokemon(
      predicate: Pokemon => Boolean = _ => true
  ): Option[Pokemon] = {
    val filtered_pokedex = pokedex.filter(predicate)
    var cumulative_weights: Int = filtered_pokedex.foldLeft(0)({
      _ + _.catch_rate.getOrElse(0)
    })

    //This is O(n) - maybe inefficient because you can probably do some fancy binary search, but the .filter above is already O(n)
    for (pkmn <- filtered_pokedex) {
      var this_catch_rate: Int = pkmn.catch_rate.getOrElse(0)
      var random_number: Double = rng.between(0.0, cumulative_weights)
      if (random_number < this_catch_rate) {
        return Some(pkmn)
      }
      cumulative_weights -= this_catch_rate
    }
    None
  }

  /** Takes in the raw string input from a user and returns a (command, arguments) tuple
    * This is wrapped in an Option; it returns None if it cannot be parsed
    *
    * @param input
    * @return
    */
  private def parse(input: String): Option[(String, String)] = {
    input match {
      case parse_RE(cmd, arg) => Some((cmd, arg))
      case _                  => None
    }
  }
}

object Cli {
  val default_num_balls = 10
  val pokedex_file_location: String = "pokedex.csv"
  val pokemon_types: Set[String] = Set(
    "Normal",
    "Fighting",
    "Flying",
    "Poison",
    "Ground",
    "Rock",
    "Bug",
    "Ghost",
    "Steel",
    "Fire",
    "Water",
    "Grass",
    "Electric",
    "Psychic",
    "Ice",
    "Dragon",
    "Dark",
    "Fairy"
  )
  val vowels: Set[Char] = Set('A', 'E', 'I', 'O', 'U')
}
