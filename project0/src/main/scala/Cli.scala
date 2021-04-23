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

  /** Run the Cli
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
        main_menu_loop()
      }
    } finally {
      dbc.disconnect()
    }
  }

  private def main_menu_loop(): Unit = {
    var do_exit: Boolean = false

    while (!do_exit) {
      println("MAIN MENU")
      println("To start a new game, type `new`")
      println("To continue a saved game, type `continue`")
      println("To delete a saved game, type `delete`")
      println("To exit, type `exit` (also aliased as `leave` or `quit`)")
      print("🔵 >> ")
      var input = StdIn.readLine()
      parse(input) match {
        case Some(("new", "")) =>
          start_new_game()
          if (player != null) {
            welcome_center_loop()
            do_exit = true
          }
        case Some(("continue", "")) =>
          continue_game()
          if (player != null) {
            welcome_center_loop()
            do_exit = true
          }
        case Some(("delete", "")) =>
          delete_game()
        case Some(("exit", "")) | Some(("leave", "")) | Some(("quit", "")) => {
          println("Goodbye!")
          do_exit = true
        }
        case Some((_, _)) | None =>
          println("command not understood")
      }
      println()
    }
  }

  private def start_new_game(): Unit = {
    var do_exit: Boolean = false
    while (!do_exit) {
      print(
        "Please enter your name (or \"exit\" to go back to the main menu): "
      )
      var input: String = StdIn.readLine().trim()
      if (input == "exit" || input == "leave" || input == "quit") {
        do_exit = true
      } else {
        dbc.get_player(input) match {
          case Failure(exception) =>
            println(s"Exception occurred! $exception")
          case Success(Some(value)) =>
            println()
            println(
              s"""A player with the name "$input" already exists. Choose a different name."""
            )
          case Success(None) =>
            val temp_player: Player =
              new Player(input, Player.num_balls_default)
            if (dbc.add_player(temp_player)) {
              player = temp_player
              println()
              println(s"Welcome to the Pokémon Safari, $input!")
              println(s"Here you can go around and catch any Pokémon you see!")
              println(
                s"Let me get you ${Player.num_balls_default} Pokéballs to start."
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
              do_exit = true
            }
        }
      }
    }
  }

  private def continue_game(): Unit = {
    var do_exit: Boolean = false
    while (!do_exit) {
      println()
      println("LIST OF PLAYERS")
      dbc.get_all_player_names().foreach(x => println(s"  $x"))

      print(
        "Please enter your name from the list above (or \"exit\" to go back to the main menu): "
      )
      var input: String = StdIn.readLine().trim()
      if (input == "exit" || input == "leave" || input == "quit") {
        do_exit = true
      } else {
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
            do_exit = true
          case Success(None) =>
            println()
            println(
              s"""No player with the name "$input" exists."""
            )
        }
      }
    }
  }

  private def delete_game(): Unit = {
    var do_exit: Boolean = false
    while (!do_exit) {
      println()
      println("LIST OF PLAYERS")
      dbc.get_all_player_names().foreach(x => println(s"  $x"))

      println()
      println("⚠️ WARNING ⚠️")
      println("You are about to delete a save file.")
      print(
        "Type a name from the list above to delete, or type \"exit\" to go back to the main menu: "
      )
      var input: String = StdIn.readLine().trim()
      if (input == "exit" || input == "leave" || input == "quit") {
        do_exit = true
      } else {
        dbc.get_player(input) match {
          case Failure(exception) =>
            println(s"Exception occurred! $exception")
          case Success(Some(value)) =>
            println()
            println(
              s"You are about to delete all save data for the player $input."
            )
            println(s"⚠️ This cannot be undone! ⚠️")
            print(
              s"To continue, type the name again exactly. To cancel, type anything else: "
            )
            var new_input: String = StdIn.readLine().trim()
            if (new_input == input) {
              if (dbc.delete_player(input)) {
                println("Save file has been successfully deleted.")
              } else {
                println(
                  "Something has gone wrong while deleting the save file. Returning to main menu."
                )
              }
              do_exit = true
            } else {
              println(
                s"You have decided not to delete the save file for $input."
              )
            }
          case Success(None) =>
            println()
            println(
              s"""No player with the name "$input" exists."""
            )
        }
      }
    }
  }

  /** main gameplay loop
    */
  private def welcome_center_loop(): Unit = {
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
            "`pokédex`: explore info on your caught Pokémon (also aliased as `pokedex`, `pokémon`, `pokemon`, and `dex`)"
          )
          println("`exit`: exit the game (also aliased as `leave` and `quit`)")
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
            ) | Some(("pokemon", "")) | Some(("dex", "")) =>
          println(); pokedex_loop()
        case Some(("exit", "")) | Some(("leave", "")) | Some(("quit", "")) => {
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
        val num_owned: Int = dbc.check_ownership(player, pkmn)

        println(
          s"You spot ${a_or_an_pokemon} ${pkmn.name}! What would you like to do?"
        )
        if (num_owned > 0)
          println(s"You have already caught $num_owned ${pkmn.name} before.")
        println(s"You have ${player.num_balls} $pokeball_or_pokeballs left.")

        print("🌴 >> ")
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
            println(
              "`exit`: leave this Safari Zone (also aliased as `leave` and `quit`)"
            )
            println()
          }
          case Some(("catch", _)) | Some(("ball", _)) => {
            println(s"You caught ${a_or_an_pokemon} ${pkmn.name}!")
            player.num_balls -= 1
            dbc.update_player_num_balls(player)
            dbc.increment_ownership(player, pkmn)
            //generate a new Pokémon
            //because we've already run it once, we can be confident get_random_pokemon isn't None
            pkmn = get_random_pokemon(
              predicate
            ).get
          }
          case Some(("flee", _)) | Some(("ignore", _)) | Some(("pass", _)) => {
            println(s"You ignore the ${pkmn.name} and continue searching")
            //generate a new Pokémon
            //because we've already run it once, we can be confident get_random_pokemon isn't None
            pkmn = get_random_pokemon(
              predicate
            ).get
          }
          case Some(("leave", "")) | Some(("exit", "")) |
              Some(("quit", "")) => {
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
      player.num_balls = Player.num_balls_default
    } else if (player.num_balls < Player.num_balls_default - 1) {
      println(
        "You're starting to get a little bit low. I'll get you a few more."
      )
      println(
        s"(You received ${Player.num_balls_default - player.num_balls} Pokéballs!)"
      )
      player.num_balls = Player.num_balls_default
    } else {
      println("Hm... It looks like you still have plenty.")
      println("Come back when you start to run low.")
    }
    dbc.update_player_num_balls(player)
  }

  /** show trainer card
    */
  private def trainer_card(): Unit = {
    println("============TRAINER CARD============")
    println(s"TRAINER NAME: ${player.name}")
    println(s"NUMBER OF POKÉBALLS: ${player.num_balls}")
    println(
      s"NUMBER OF UNIQUE POKÉMON CAUGHT: ${dbc.get_all_owned(player).count(_ => true)}"
    )
    println("====================================")
  }

  /** run pokedex loop
    */
  private def pokedex_loop(): Unit = {
    println(
      "Welcome to the Pokédex. Here, you can learn a bit more about the Pokémon you've caught!"
    )
    println("Type `help` for a full list of commands")
    var do_exit: Boolean = false

    while (!do_exit) {
      print("🔎 >> ")
      var input = StdIn.readLine()
      parse(input) match {
        case Some(("help", "")) => {
          println()
          println("`help`: Show this help prompt")
          println(
            "`list`: List all Pokémon you own and how many you have caught"
          )
          println(
            "`info [Pokémon name]`: List information about the Pokémon. You'll only get basic information if you haven't previously captured it."
          )
          println(
            "`exit`: exit the Pokédex (also aliased as `leave` and `quit`)"
          )
        }
        case Some(("list", "")) =>
          println()
          dbc.get_all_owned(player).foreach { case (pkmn, num_caught) =>
            println(s"${pkmn.name}: ${num_caught}")
          }
        case Some(("info", pkmn_name)) =>
          println()
          dbc.get_one_owned(player, pkmn_name) match {
            case Failure(exception) =>
              println(s"Exception occurred: $exception")
            case Success(None) =>
              println(
                s"""Pokémon "$pkmn_name" not found. Did you spell it right?"""
              )
            case Success(Some((pkmn, num_owned))) =>
              // helper printing functions to clean up boilerplate code used in this section
              def print_if_caught(
                  category: String,
                  caught_text: String
              ): Unit = {
                println(f"    ${category}%-20s: ${if (num_owned > 0) caught_text
                else "(Pokémon not yet caught)"}")
              }
              def print_always(
                  category: String,
                  text: String
              ): Unit = {
                println(f"    ${category}%-20s: ${text}")
              }
              def string_of_two_options(
                  opt1: Option[String],
                  opt2: Option[String]
              ): String = {
                (opt1, opt2) match {
                  case (Some(a1), None)     => a1
                  case (None, Some(a2))     => a2
                  case (Some(a1), Some(a2)) => a1 + "/" + a2
                  case (None, None)         => "Unknown"
                }
              }

              println(f"#${pkmn.pokedex_number}%03d ${pkmn.name}:")
              print_always("Number caught", num_owned.toString())

              println("  BASIC INFORMATION")
              print_always("Generation", pkmn.generation.toString())
              print_always(
                "Type",
                string_of_two_options(pkmn.type_1, pkmn.type_2)
              )
              print_if_caught("Species", pkmn.species)

              println("  PHYSICAL INFORMATION")
              print_if_caught("Height", f"${pkmn.height_m}%.1f m")
              print_if_caught("Weight", f"${pkmn.weight_kg}%.1f kg")
              print_if_caught(
                "Gender Ratio",
                pkmn.percentage_male match {
                  case None => "Genderless"
                  case Some(value) =>
                    f"${value}%% male/${100 - value}%% female"
                }
              )
              print_if_caught(
                "Egg Group",
                string_of_two_options(pkmn.egg_type_1, pkmn.egg_type_2)
              )

              println("  GAME INFORMATION")
              print_if_caught(
                "Abilities",
                string_of_two_options(pkmn.ability_1, pkmn.ability_2)
              )
              print_if_caught(
                "Hidden Ability",
                pkmn.ability_hidden.getOrElse("None")
              )
              print_if_caught(
                "Catch Rate",
                f"${pkmn.catch_rate.getOrElse("Unknown")}"
              )
              print_if_caught(
                "Base Friendship",
                f"${pkmn.base_friendship.getOrElse("Unknown")}"
              )
              print_if_caught(
                "Base Experience",
                f"${pkmn.base_experience.getOrElse("Unknown")}"
              )
              print_if_caught(
                "Growth Rate",
                f"${pkmn.base_experience.getOrElse("Unknown")}"
              )

              println("  BASE STATS")
              print_if_caught("HP", s"${pkmn.hp}")
              print_if_caught("Attack", s"${pkmn.attack}")
              print_if_caught("Defense", s"${pkmn.defense}")
              print_if_caught("Speed", s"${pkmn.speed}")
              print_if_caught("Sp. Attack", s"${pkmn.sp_attack}")
              print_if_caught("Sp. Defense", s"${pkmn.sp_defense}")
          }
        case Some(("exit", "")) | Some(("leave", "")) | Some(("quit", "")) => {
          println(
            "You put the Pokédex down and look back up at the Pokémon Safari Welcome Center."
          )
          do_exit = true
        }
        case Some((_, _)) | None =>
          println("command not understood")
      }
      println()
    }
  }

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
