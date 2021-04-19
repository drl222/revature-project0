import java.sql.ResultSet

class Player(var name: String, var num_balls: Int) {
    override def toString(): String = {
        s"<Player $name ($num_balls Pokéballs)>"
    }
}

object Player {
  // SQL column names
  val NAME = "player_name"
  val NUM_BALLS = "num_balls"

  def apply(rs: ResultSet): Player = {
    new Player(rs.getString(NAME), rs.getInt(NUM_BALLS))
  }
}
