import scala.io.StdIn
import scala.util.matching.Regex
import scala.util.Failure
import scala.util.Success
import scala.util.Random

class Cli {
  val parse_RE: Regex = raw"(\w+)\s*(.*)".r
  val pokedex_file_location: String = "pokedex.csv"
  var pokedex: Vector[Pokemon] = null
  val rng: Random = new Random()

  /** The main function to run the CLI
    */
  def run(): Unit = {
    val load_successful: Boolean = load_csv(pokedex_file_location)
    var do_exit: Boolean = !load_successful

    while (!do_exit) {
      println("You're standing in the Pokémon Safari Welcome Center.")
      println("What would you like to do?")
      print(">> ")
      var input = StdIn.readLine()
      parse(input) match {
        case Some(("safari", "")) => println(); run_safari("General")
        // case Some(("catch", "")) => println(get_random_pokemon())
        // case Some(("catch", a)) => println(get_random_pokemon(_.type_1.getOrElse("") == a))
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

  private def run_safari(
      safari_name: String,
      predicate: Pokemon => Boolean = _ => true
  ): Unit = {
    var do_exit: Boolean = false
    println(s"Welcome to the $safari_name Pokémon Safari!")
    var pkmn: Pokemon = null
    get_random_pokemon(predicate) match {
      case Some(pokemon) => {
        pkmn = pokemon
      }
      case None => {
        println(
          "Hmm... you can't seem to find any Pokémon in this safari. Returning..."
        )
        return ()
      }
    }
    while (!do_exit) {
      println(s"You spot a ${pkmn.name}! What would [you] like to do?")
      println(s"You have [infinite] Pokéballs left.")

      print(">> ")
      var input = StdIn.readLine()
      parse(input) match {
        case Some(("catch", _)) => {
          println(s"You caught a ${pkmn.name}!")
          //because we've already run it once, we can be confident get_random_pokemon isn't None
          pkmn = get_random_pokemon(
            predicate
          ).get
        }
        case Some(("flee", _)) => {
          println(s"You ignore the ${pkmn.name} and continue searching")
          //because we've already run it once, we can be confident get_random_pokemon isn't None
          pkmn = get_random_pokemon(
            predicate
          ).get
        }
        case Some(("leave", _)) => {
          println(
            s"You decide you've explored enough in the $safari_name Pokémon Safari. You head back to the Welcome Center."
          )
          do_exit = true
        }
        case Some((_, _)) | None =>
          println("command not understood")
      }
      println()
    }
  }

  /** load the Pokedex from a CSV file into memory
    *
    * @param pokedex_file_location
    */
  private def load_csv(pokedex_file_location: String): Boolean = {
    println(s"Loading Pokédex from $pokedex_file_location...")
    FileIO.read_pokedex(pokedex_file_location) match {
      case Failure(exception) => {
        println("Error reading file. Exiting...")
        false
      }
      case Success(value) => {
        pokedex = value
        println("Success!")
        print_main_menu()
        true
      }
    }
  }

  /** Print the main menu to the screen
    */
  private def print_main_menu(): Unit = {
    println("Welcome to the Pokémon catching simulator!")
    // println("Please enter your name:")
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
    var cumulative_weights: Double = filtered_pokedex.foldLeft(0.0)({
      _ + _.catch_rate.getOrElse(0.0)
    })

    //This is O(n) - maybe inefficient because you can probably do some fancy binary search, but the .filter above is already O(n)
    for (pkmn <- filtered_pokedex) {
      var this_catch_rate: Double = pkmn.catch_rate.getOrElse(0.0)
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
