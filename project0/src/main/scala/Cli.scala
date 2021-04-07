import scala.io.StdIn
import scala.util.matching.Regex

class Cli {
    val parse_RE:Regex = raw"(\w+).*(.*)".r

    def run():Unit = {
        var do_exit:Boolean = false
        while(!do_exit) {
            print(">> ")
            var input = StdIn.readLine()
            if(input == "exit"){
                do_exit = true
            }
        }
    }
}
