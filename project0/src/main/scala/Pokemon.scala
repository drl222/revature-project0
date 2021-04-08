final case class Pokemon(
    key: Int,
    pokedex_number: Int,
    name: String,
    german_name: Option[String],
    japanese_name: Option[String],
    generation: Int,
    status: String, // normal, legendary, etc.
    species: String,
    type_number: Int,
    type_1: Option[String],
    type_2: Option[String],
    height_m: Double,
    weight_kg: Double,
    abilities_number: Int,
    ability_1: Option[String],
    ability_2: Option[String],
    ability_hidden: Option[String],
    total_points: BigDecimal,
    hp: BigDecimal,
    attack: BigDecimal,
    defense: BigDecimal,
    sp_attack: BigDecimal,
    sp_defense: BigDecimal,
    speed: BigDecimal,
    catch_rate: Option[BigDecimal],
    base_friendship: Option[BigDecimal],
    base_experience: Option[BigDecimal],
    growth_rate: String,
    egg_type_number: Int,
    egg_type_1: Option[String],
    egg_type_2: Option[String],
    percentage_male: Option[BigDecimal],
    egg_cycles: BigDecimal,
    against_normal: BigDecimal,
    against_fire: BigDecimal,
    against_water: BigDecimal,
    against_electric: BigDecimal,
    against_grass: BigDecimal,
    against_ice: BigDecimal,
    against_fight: BigDecimal,
    against_poison: BigDecimal,
    against_ground: BigDecimal,
    against_flying: BigDecimal,
    against_psychic: BigDecimal,
    against_bug: BigDecimal,
    against_rock: BigDecimal,
    against_ghost: BigDecimal,
    against_dragon: BigDecimal,
    against_dark: BigDecimal,
    against_steel: BigDecimal,
    against_fairy: BigDecimal
)