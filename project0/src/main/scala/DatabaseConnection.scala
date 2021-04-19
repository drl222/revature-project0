import java.sql.DriverManager
import java.sql.Connection
import java.sql.ResultSet
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

class DatabaseConnection(val connection_URL: String) {
  var is_connected: Boolean = false
  var conn: Connection = null

  def connect() = {
    //load a driver
    classOf[org.postgresql.Driver].newInstance()

    //use JDBC's DriverManager to get a connection. JDBC is DB agnostic
    // arguments are DB location, username, and password
    conn = DriverManager.getConnection(
      connection_URL,
      DatabaseCredentials.username,
      DatabaseCredentials.password
    ) //DANGER: hardcoded pasword! (If you see this on Git, hopefully I didn't actually put the DatabaseCredentials.scala file out there)
    is_connected = true

    make_players_table_if_not_exists()
    get_all_players() match {
      case Failure(exception) =>
        println(s"Error connecting to database: $exception")
      case Success(value) =>
        for (i <- value) {
          println(i)
        }
    }
  }

  def make_players_table_if_not_exists(): Unit = {
    if (!is_connected) throw new java.sql.SQLException("Connection not open")
    // using direct string interpolation instead of prepared statement because you can't used prepared statements for column names
    val make_players = conn.prepareStatement(
      s"CREATE TABLE IF NOT EXISTS Players (${Player.NAME} TEXT PRIMARY KEY, ${Player.NUM_BALLS} INT NOT NULL);"
    )
    val make_pokemon = conn.prepareStatement(
      s"CREATE TABLE IF NOT EXISTS Pokemon (${Pokemon.POKEMON_ID} INT PRIMARY KEY, ${Pokemon.POKEDEX_NUMBER} INT NOT NULL," +
        s"${Pokemon.NAME} TEXT NOT NULL, ${Pokemon.GENERATION} INT NOT NULL, ${Pokemon.STATUS} TEXT NOT NULL, ${Pokemon.SPECIES} TEXT NOT NULL," +
        s"${Pokemon.TYPE_1} TEXT, ${Pokemon.TYPE_2} TEXT, ${Pokemon.HEIGHT_M} DOUBLE PRECISION NOT NULL, ${Pokemon.WEIGHT_KG} DOUBLE PRECISION NOT NULL," +
        s"${Pokemon.ABILITY_1} TEXT, ${Pokemon.ABILITY_2} TEXT, ${Pokemon.ABILITY_HIDDEN} TEXT," +
        s"${Pokemon.HP} INT NOT NULL, ${Pokemon.ATTACK} INT NOT NULL, ${Pokemon.DEFENSE} INT NOT NULL," +
        s"${Pokemon.SP_ATTACK} INT NOT NULL, ${Pokemon.SP_DEFENSE} INT NOT NULL, ${Pokemon.SPEED} INT NOT NULL," +
        s"${Pokemon.CATCH_RATE} INT, ${Pokemon.BASE_FRIENDSHIP} INT, ${Pokemon.BASE_EXPERIENCE} INT, ${Pokemon.GROWTH_RATE} TEXT NOT NULL," +
        s"${Pokemon.EGG_TYPE_1} TEXT, ${Pokemon.EGG_TYPE_2} TEXT, ${Pokemon.PERCENTAGE_MALE} DOUBLE PRECISION, ${Pokemon.EGG_CYCLES} INT);"
    )
    make_players.execute()
    make_pokemon.execute()
  }

  def get_all_players(): Try[mutable.Seq[Player]] = {
    if (!is_connected) throw new java.sql.SQLException("Connection not open")
    val stmt = conn.prepareStatement("SELECT * FROM Players;")
    Try {
      val a = stmt.executeQuery()
      val to_return = ArrayBuffer[Player]()
      while (a.next()) {
        to_return.addOne(Player(a))
      }
      to_return
    }
  }

  def disconnect() = {
    if (is_connected) conn.close() //TODO: try-catch and throw out exceptions
  }

}
