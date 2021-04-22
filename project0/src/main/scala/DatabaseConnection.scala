import java.sql.DriverManager
import java.sql.Connection
import java.sql.ResultSet
import java.sql.PreparedStatement
import java.sql.SQLException
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
    conn.prepareStatement("SET SCHEMA 'public';").execute()

    make_tables_if_not_exist()
  }

  def make_tables_if_not_exist(): Unit = {
    if (!is_connected) throw new SQLException("Connection not open")
    // using direct string interpolation instead of prepared statement because you can't used prepared statements for column names
    val make_players = conn.prepareStatement(
      s"CREATE TABLE IF NOT EXISTS ${DatabaseConnection.TABLE_PLAYER} (${Player.NAME} TEXT PRIMARY KEY, ${Player.NUM_BALLS} INT NOT NULL);"
    )
    val make_pokemon = conn.prepareStatement(
      s"CREATE TABLE IF NOT EXISTS ${DatabaseConnection.TABLE_POKEMON} (${Pokemon.POKEMON_ID} INT PRIMARY KEY, ${Pokemon.POKEDEX_NUMBER} INT NOT NULL," +
        s"${Pokemon.NAME} TEXT NOT NULL, ${Pokemon.GENERATION} INT NOT NULL, ${Pokemon.STATUS} TEXT NOT NULL, ${Pokemon.SPECIES} TEXT NOT NULL," +
        s"${Pokemon.TYPE_1} TEXT, ${Pokemon.TYPE_2} TEXT, ${Pokemon.HEIGHT_M} DOUBLE PRECISION NOT NULL, ${Pokemon.WEIGHT_KG} DOUBLE PRECISION NOT NULL," +
        s"${Pokemon.ABILITY_1} TEXT, ${Pokemon.ABILITY_2} TEXT, ${Pokemon.ABILITY_HIDDEN} TEXT," +
        s"${Pokemon.HP} INT NOT NULL, ${Pokemon.ATTACK} INT NOT NULL, ${Pokemon.DEFENSE} INT NOT NULL," +
        s"${Pokemon.SP_ATTACK} INT NOT NULL, ${Pokemon.SP_DEFENSE} INT NOT NULL, ${Pokemon.SPEED} INT NOT NULL," +
        s"${Pokemon.CATCH_RATE} INT, ${Pokemon.BASE_FRIENDSHIP} INT, ${Pokemon.BASE_EXPERIENCE} INT, ${Pokemon.GROWTH_RATE} TEXT NOT NULL," +
        s"${Pokemon.EGG_TYPE_1} TEXT, ${Pokemon.EGG_TYPE_2} TEXT, ${Pokemon.PERCENTAGE_MALE} DOUBLE PRECISION, ${Pokemon.EGG_CYCLES} INT);"
    )
    val make_ownership = conn.prepareStatement(
      s"CREATE TABLE IF NOT EXISTS ${DatabaseConnection.TABLE_OWNERSHIP} (" +
        s"${DatabaseConnection.OWNERSHIP_PLAYER_ID} TEXT, ${DatabaseConnection.OWNERSHIP_POKEMON_ID} INT, ${DatabaseConnection.OWNERSHIP_NUMBER_OWNED} INT NOT NULL," +
        s"PRIMARY KEY (${DatabaseConnection.OWNERSHIP_PLAYER_ID}, ${DatabaseConnection.OWNERSHIP_POKEMON_ID})," +
        s"FOREIGN KEY (${DatabaseConnection.OWNERSHIP_PLAYER_ID}) REFERENCES ${DatabaseConnection.TABLE_PLAYER}(${Player.NAME})," +
        s"FOREIGN KEY (${DatabaseConnection.OWNERSHIP_POKEMON_ID}) REFERENCES ${DatabaseConnection.TABLE_POKEMON}(${Pokemon.POKEMON_ID})" +
        s");"
    )
    make_players.execute()
    make_pokemon.execute()
    make_ownership.execute()
  }

  def check_if_pokemon_table_is_populated(): Boolean = {
    if (!is_connected) throw new SQLException("Connection not open")
    val stmt = conn.prepareStatement(
      s"SELECT count(*) FROM ${DatabaseConnection.TABLE_POKEMON};"
    )
    try {
      val rs: ResultSet = stmt.executeQuery()
      rs.next()
      val num_pokemon: Int = rs.getInt(1)
      num_pokemon > 0
    } catch {
      case e: SQLException => println(s"Exception occurred! $e"); false
    }
  }

  def load_pokedex(): Try[Vector[Pokemon]] = {
    if (!is_connected) throw new SQLException("Connection not open")
    val stmt = conn.prepareStatement(
      s"SELECT * FROM ${DatabaseConnection.TABLE_POKEMON};"
    )
    Try {
      val rs = stmt.executeQuery()
      var to_return = Vector[Pokemon]()
      while (rs.next()) {
        to_return = Pokemon(rs) +: to_return
      }
      to_return
    }
  }

  /** try to store the given pokedex in the SQL dabase. Returns whether or not it was successful
    *
    * @param pokedex
    */
  def store_pokedex(pokedex: Vector[Pokemon]): Boolean = {
    conn.setAutoCommit(false);
    try {
      val stmt = conn.prepareStatement(
        s"INSERT INTO ${DatabaseConnection.TABLE_POKEMON} VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
      )
      pokedex.foreach(pkmn => {
        stmt.setInt(1, pkmn.pokemon_id)
        stmt.setInt(2, pkmn.pokedex_number)
        stmt.setString(3, pkmn.name)
        stmt.setInt(4, pkmn.generation)
        stmt.setString(5, pkmn.status)
        stmt.setString(6, pkmn.species)
        setStringOption(stmt, 7, pkmn.type_1)
        setStringOption(stmt, 8, pkmn.type_2)
        stmt.setDouble(9, pkmn.height_m)
        stmt.setDouble(10, pkmn.weight_kg)
        setStringOption(stmt, 11, pkmn.ability_1)
        setStringOption(stmt, 12, pkmn.ability_2)
        setStringOption(stmt, 13, pkmn.ability_hidden)
        stmt.setInt(14, pkmn.hp)
        stmt.setInt(15, pkmn.attack)
        stmt.setInt(16, pkmn.defense)
        stmt.setInt(17, pkmn.sp_attack)
        stmt.setInt(18, pkmn.sp_defense)
        stmt.setInt(19, pkmn.speed)
        setIntOption(stmt, 20, pkmn.catch_rate)
        setIntOption(stmt, 21, pkmn.base_friendship)
        setIntOption(stmt, 22, pkmn.base_experience)
        stmt.setString(23, pkmn.growth_rate)
        setStringOption(stmt, 24, pkmn.egg_type_1)
        setStringOption(stmt, 25, pkmn.egg_type_2)
        setDoubleOption(stmt, 26, pkmn.percentage_male)
        stmt.setInt(27, pkmn.egg_cycles)
        stmt.executeUpdate()
      })
      conn.commit()
      true
    } catch {
      case e: SQLException =>
        conn.rollback()
        println(s"Exception occurred! $e")
        false
    } finally {
      conn.setAutoCommit(true)
    }
  }
  private def setIntOption(
      stmt: PreparedStatement,
      i: Int,
      newValue: Option[Int]
  ) = {
    newValue match {
      case Some(value) => stmt.setInt(i, value)
      case None        => stmt.setNull(i, java.sql.Types.INTEGER)
    }
  }
  private def setStringOption(
      stmt: PreparedStatement,
      i: Int,
      newValue: Option[String]
  ) = {
    newValue match {
      case Some(value) => stmt.setString(i, value)
      case None        => stmt.setNull(i, java.sql.Types.VARCHAR)
    }
  }
  private def setDoubleOption(
      stmt: PreparedStatement,
      i: Int,
      newValue: Option[Double]
  ) = {
    newValue match {
      case Some(value) => stmt.setDouble(i, value)
      case None        => stmt.setNull(i, java.sql.Types.DOUBLE)
    }
  }

  def get_player(player_name: String): Try[Option[Player]] = {
    if (!is_connected) throw new SQLException("Connection not open")
    val stmt = conn.prepareStatement(
      s"SELECT * FROM ${DatabaseConnection.TABLE_PLAYER} WHERE ${Player.NAME} LIKE ?;"
    )
    Try {
      stmt.setString(1, player_name)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        Some(Player(rs))
      } else {
        None
      }
    }
  }

  def add_player(player: Player): Boolean = {
    if (!is_connected) throw new SQLException("Connection not open")
    val stmt = conn.prepareStatement(
      s"INSERT INTO ${DatabaseConnection.TABLE_PLAYER}(${Player.NAME}, ${Player.NUM_BALLS}) VALUES (?, ?);"
    )
    try {
      stmt.setString(1, player.name)
      stmt.setInt(2, player.num_balls)
      val rs = stmt.executeUpdate()
      true
    } catch {
      case e: SQLException =>
        println(s"Exception occurred! $e")
        false
    }
  }

  def update_player_num_balls(player: Player): Boolean = {
    if (!is_connected) throw new SQLException("Connection not open")
    val stmt = conn.prepareStatement(
      s"UPDATE ${DatabaseConnection.TABLE_PLAYER} SET ${Player.NUM_BALLS} = ? WHERE ${Player.NAME} = ?;"
    )
    try {
      stmt.setInt(1, player.num_balls)
      stmt.setString(2, player.name)
      val rs = stmt.executeUpdate()
      true
    } catch {
      case e: SQLException =>
        println(s"Exception occurred! $e")
        false
    }
  }

  def increment_ownership(player: Player, pkmn: Pokemon): Boolean = {
    if (!is_connected) throw new SQLException("Connection not open")
    val stmt = conn.prepareStatement(
      s"INSERT INTO ${DatabaseConnection.TABLE_OWNERSHIP}(${DatabaseConnection.OWNERSHIP_PLAYER_ID}, ${DatabaseConnection.OWNERSHIP_POKEMON_ID}, ${DatabaseConnection.OWNERSHIP_NUMBER_OWNED})" +
        s" VALUES(?, ?, ?) ON CONFLICT (${DatabaseConnection.OWNERSHIP_PLAYER_ID}, ${DatabaseConnection.OWNERSHIP_POKEMON_ID}) DO UPDATE SET" +
        s" ${DatabaseConnection.OWNERSHIP_NUMBER_OWNED} = ${DatabaseConnection.TABLE_OWNERSHIP}.${DatabaseConnection.OWNERSHIP_NUMBER_OWNED} + 1;"
    )
    try {
      stmt.setString(1, player.name)
      stmt.setInt(2, pkmn.pokemon_id)
      stmt.setInt(3, 1)
      val rs = stmt.executeUpdate()
      true
    } catch {
      case e: SQLException =>
        println(s"Exception occurred! $e")
        false
    }
  }

  def check_ownership(player: Player, pkmn: Pokemon): Int = {
    if (!is_connected) throw new SQLException("Connection not open")
    val stmt = conn.prepareStatement(
      s"SELECT ${DatabaseConnection.OWNERSHIP_NUMBER_OWNED} FROM ${DatabaseConnection.TABLE_OWNERSHIP} WHERE ${DatabaseConnection.OWNERSHIP_PLAYER_ID} = ? AND ${DatabaseConnection.OWNERSHIP_POKEMON_ID} = ?;"
    )
    try {
      stmt.setString(1, player.name)
      stmt.setInt(2, pkmn.pokemon_id)
      val rs: ResultSet = stmt.executeQuery()
      if(rs.next()){
        rs.getInt(1)
      } else {
        0
      }
    } catch {
      case e: SQLException => println(s"Exception occurred! $e"); 0
    }
  }

  def disconnect() = {
    try { conn.close() }
    catch {
      case e: Exception =>
        println(s"Exception occurred while disconnecting: $e")
    }
  }

}

object DatabaseConnection {
  // table names
  val TABLE_PLAYER = "Players"
  val TABLE_POKEMON = "Pokemon"
  val TABLE_OWNERSHIP = "Ownership"

  // ownership SQL column names
  val OWNERSHIP_PLAYER_ID = "player_name"
  val OWNERSHIP_POKEMON_ID = "pokemon_id"
  val OWNERSHIP_NUMBER_OWNED = "num_owned"
}
