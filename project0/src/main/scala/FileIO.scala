import java.io.File
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import scala.util.Try

object FileIO {
  def read_pokedex(file_location: String): Try[Vector[Pokemon]] = {
    Try({
      val rawData: File = new File(file_location)
      rawData.readCsv[Vector, Pokemon](rfc.withHeader).collect {
        case Right(a) => a
      }
    })
  }
}
