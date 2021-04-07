import scala.io.StdIn
import scala.util.matching.Regex

class Cli {
    val parse_RE:Regex = raw"(\w+)\s*(.*)".r

    /**
      * The main function to run the CLI
      */
    def run():Unit = {
        var do_exit:Boolean = false
        while(!do_exit) {
            print(">> ")
            var input = StdIn.readLine()
            parse(input) match {
                case Some(("exit", _)) => {
                    println("Goodbye!")
                    do_exit = true
                }
                case Some((cmd, arg)) => println(s"""command "$cmd" not understood with arguments "$arg"""")
                case None => println("command unable to be parsed")
            }
        }
    }

    /**
      * Takes in the raw string input from a user and returns a (command, arguments) tuple
      * This is wrapped in an Option; it returns None if it cannot be parsed
      *
      * @param input
      * @return
      */
    def parse(input: String):Option[(String, String)] = {
        input match {
            case parse_RE(cmd, arg) => Some ((cmd, arg))
            case _ => None
        }
    }
}
