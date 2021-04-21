import java.sql.ResultSet

final case class Pokemon(
    pokemon_id: Int,
    pokedex_number: Int,
    name: String,
    generation: Int,
    status: String, // normal, legendary, etc.
    species: String,
    type_1: Option[String],
    type_2: Option[String],
    height_m: Double,
    weight_kg: Double,
    ability_1: Option[String],
    ability_2: Option[String],
    ability_hidden: Option[String],
    hp: Int,
    attack: Int,
    defense: Int,
    sp_attack: Int,
    sp_defense: Int,
    speed: Int,
    catch_rate: Option[Int],
    base_friendship: Option[Int],
    base_experience: Option[Int],
    growth_rate: String,
    egg_type_1: Option[String],
    egg_type_2: Option[String],
    percentage_male: Option[Double], // None means genderless
    egg_cycles: Int
)

object Pokemon {
  // database column names
  val POKEMON_ID = "pokemon_id"
  val POKEDEX_NUMBER = "pokedex_number"
  val NAME = "name"
  val GENERATION = "generation"
  val STATUS = "status"
  val SPECIES = "species"
  val TYPE_1 = "type1"
  val TYPE_2 = "type2"
  val HEIGHT_M = "height"
  val WEIGHT_KG = "weight"
  val ABILITY_1 = "ability1"
  val ABILITY_2 = "ability2"
  val ABILITY_HIDDEN = "ability_hidden"
  val HP = "hp"
  val ATTACK = "atk"
  val DEFENSE = "def"
  val SP_ATTACK = "spatk"
  val SP_DEFENSE = "spdef"
  val SPEED = "speed"
  val CATCH_RATE = "catch_rate"
  val BASE_FRIENDSHIP = "base_friendship"
  val BASE_EXPERIENCE = "base_experience"
  val GROWTH_RATE = "growth_rate"
  val EGG_TYPE_1 = "egg_type_1"
  val EGG_TYPE_2 = "egg_type_2"
  val PERCENTAGE_MALE = "percent_male"
  val EGG_CYCLES = "egg_cycles"

  def apply(rs: ResultSet): Pokemon = {
    apply(
      rs.getInt(Pokemon.POKEMON_ID),
      rs.getInt(Pokemon.POKEDEX_NUMBER),
      rs.getString(Pokemon.NAME),
      rs.getInt(Pokemon.GENERATION),
      rs.getString(Pokemon.STATUS),
      rs.getString(Pokemon.SPECIES),
      getOptionString(rs, Pokemon.TYPE_1),
      getOptionString(rs, Pokemon.TYPE_2),
      rs.getDouble(Pokemon.HEIGHT_M),
      rs.getDouble(Pokemon.WEIGHT_KG),
      getOptionString(rs, Pokemon.ABILITY_1),
      getOptionString(rs, Pokemon.ABILITY_2),
      getOptionString(rs, Pokemon.ABILITY_HIDDEN),
      rs.getInt(Pokemon.HP),
      rs.getInt(Pokemon.ATTACK),
      rs.getInt(Pokemon.DEFENSE),
      rs.getInt(Pokemon.SP_ATTACK),
      rs.getInt(Pokemon.SP_DEFENSE),
      rs.getInt(Pokemon.SPEED),
      getOptionInt(rs, Pokemon.CATCH_RATE),
      getOptionInt(rs, Pokemon.BASE_FRIENDSHIP),
      getOptionInt(rs, Pokemon.BASE_EXPERIENCE),
      rs.getString(Pokemon.GROWTH_RATE),
      getOptionString(rs, Pokemon.EGG_TYPE_1),
      getOptionString(rs, Pokemon.EGG_TYPE_2),
      getOptionDouble(rs, Pokemon.PERCENTAGE_MALE),
      rs.getInt(Pokemon.EGG_CYCLES)
    )
  }

  private def getOptionInt(rs: ResultSet, name: String): Option[Int] = {
    val intValue: Int = rs.getInt(name)
    if (rs.wasNull()) {
      None
    } else {
      Some(intValue)
    }
  }
  private def getOptionString(rs: ResultSet, name: String): Option[String] = {
    val stringValue: String = rs.getString(name)
    if (rs.wasNull()) {
      None
    } else {
      Some(stringValue)
    }
  }
  private def getOptionDouble(rs: ResultSet, name: String): Option[Double] = {
    val doubleValue: Double = rs.getDouble(name)
    if (rs.wasNull()) {
      None
    } else {
      Some(doubleValue)
    }
  }
}
