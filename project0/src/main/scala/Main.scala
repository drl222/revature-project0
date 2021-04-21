import scala.util.Failure
import scala.util.Success
object Main {
  def main(args: Array[String]): Unit = {
    var dbc: DatabaseConnection = new DatabaseConnection(
      "jdbc:postgresql://localhost:5432/pokemonsafari"
    )
    var cli: Cli = new Cli(dbc)
    cli.run()
  }
}
