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
}
