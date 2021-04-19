import java.io.File
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import scala.util.Try

object FileIO {
  def read_pokedex(file_location: String): Try[Vector[Pokemon]] = {
    Try({
      val rawData: File = new File(file_location)
      rawData.readCsv[Vector, Pokemon_raw](rfc.withHeader).collect {
        case Right(a) => a.toPokemon()
        // collect throws out any malformed (Left) rows
      }
    })
  }
}

/** Only used for reading in. Should not be used except to convert to a normal Pokemon
  */
case class Pokemon_raw(
    pokemon_id: Int,
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
    total_points: Double,
    hp: Double,
    attack: Double,
    defense: Double,
    sp_attack: Double,
    sp_defense: Double,
    speed: Double,
    catch_rate: Option[Double],
    base_friendship: Option[Double],
    base_experience: Option[Double],
    growth_rate: String,
    egg_type_number: Int,
    egg_type_1: Option[String],
    egg_type_2: Option[String],
    percentage_male: Option[Double],
    egg_cycles: Double,
    against_normal: Double,
    against_fire: Double,
    against_water: Double,
    against_electric: Double,
    against_grass: Double,
    against_ice: Double,
    against_fight: Double,
    against_poison: Double,
    against_ground: Double,
    against_flying: Double,
    against_psychic: Double,
    against_bug: Double,
    against_rock: Double,
    against_ghost: Double,
    against_dragon: Double,
    against_dark: Double,
    against_steel: Double,
    against_fairy: Double
) {
  def toPokemon(): Pokemon = {
    Pokemon.apply(
      pokemon_id,
      pokedex_number,
      name,
      generation,
      status,
      species,
      type_1,
      type_2,
      height_m,
      weight_kg,
      ability_1,
      ability_2,
      ability_hidden,
      hp.toInt,
      attack.toInt,
      defense.toInt,
      sp_attack.toInt,
      sp_defense.toInt,
      speed.toInt,
      catch_rate.flatMap[Int](x => Some(x.toInt)),
      base_friendship.flatMap[Int](x => Some(x.toInt)),
      base_experience.flatMap[Int](x => Some(x.toInt)),
      growth_rate,
      egg_type_1,
      egg_type_2,
      percentage_male,
      egg_cycles.toInt
    )
  }

}
