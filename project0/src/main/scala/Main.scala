import scala.util.Failure
import scala.util.Success
object Main {
  def main(args: Array[String]): Unit = {
    var dbc: DatabaseConnection = new DatabaseConnection(
      "jdbc:postgresql://localhost:5432/pokemonsafari"
    )
    try {
      dbc.connect()
    } finally {
      dbc.disconnect()
    }
  }

  // var cli:Cli = new Cli()
  // cli.run()
  // FileIO.read_pokedex("pokedex2.csv") match {
  //   case Failure(exception) => println(s"OH NO $exception")
  //   case Success(value) =>
  //     value.foreach(pkmn => {
  //       val typestring = (pkmn.type_1, pkmn.type_2) match{
  //         case (None, None) => "None"
  //         case (Some(a), None) => a
  //         case (Some(a), Some(b)) => a + "/" + b
  //         case (None, Some(b)) => b
  //       }

  //       println(
  //         s"${pkmn.name} (${pkmn.japanese_name.getOrElse("unknown Japanese name")}): Type ${typestring}, Catch rate ${pkmn.catch_rate.getOrElse("None")}"
  //       )
  //     })
  // }
}
