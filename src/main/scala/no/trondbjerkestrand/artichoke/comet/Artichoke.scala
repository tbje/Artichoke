package no.trondbjerkestrand.artichoke.comet

import net.liftweb.http.{CometActor,S}
import scala.xml._
import scala.io.Source
import org.scala_tools.time.Imports._


import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.parsing.combinator._

import net.liftweb.actor._
import java.io.FileReader
import net.liftweb.util.Helpers._

case object Update
case class Parse(comet: Artichoke, file: String)
case class Parsed(lst: List[((DateTime, String, String, String), NodeSeq)])

object ReportLineParser extends RegexParsers {
	def mapXml[T](lst: Seq[T], fun : T=>NodeSeq, sep : NodeSeq = NodeSeq.Empty, ifEmpty: NodeSeq = NodeSeq.Empty) = {
		lst match {
			case Nil => ifEmpty
			case head::Nil => fun(head)
			case _ => lst.tail.foldLeft(fun(lst.head))(_++ sep ++ fun(_))
		}
	}

	override def skipWhitespace = false

    def regexMatch(r: Regex): Parser[Match] = new Parser[Regex.Match] {
      def apply(in: Input) = {
        val source = in.source
        val offset = in.offset
		val start = handleWhiteSpace(source, offset)
        (r findPrefixMatchOf (source.subSequence(start, source.length))) match {
          case Some(matched) =>
            Success(matched, in.drop(start + matched.end - offset))
          case None =>
            Failure("string matching regex `"+r+"' expected but `"+in.first+"' found", in.drop(start - offset))
        }
      }
    }
	val lineBreak = "\r\n" | "\n"
	val date : Parser[DateTime]  = regexMatch("""(\d{4}-\d{2}-\d{2}) (\d{2}):(\d{2}):(\d{2})""".r) ^^ 
		(x=> new DateTime(x.group(1)).hour(x.group(2)).minute(x.group(3)).second(x.group(4)))
	val level : Parser[String] = "[^|]*".r 
	val logger : Parser[String] = "[^|]*".r 
	val user : Parser[String] = "[^|]*".r 
	val sessionId : Parser[String] = "[^|]*".r 
	val any : Parser[String] = not(date) ~ ".*".r ^^ {case a~b=>b}
	val logLine : Parser[(DateTime, String, String, String)] = (date <~" | ") ~ (level <~"| ") ~ (logger <~ "| ") ~ (any <~ (lineBreak | """\z""".r)) ^^ {case ((date~level)~logger)~any => (date, level, logger, any)}
	val logEntry : Parser[((DateTime, String, String, String), NodeSeq)]= (logLine ~ rep(any <~ lineBreak )) <~ (guard(date) | """\z""".r) ^^ {
		case (line@(date, level, logger, msg))~list => (line, mapXml[String](logger+":"::msg::list, Text(_), <br/>):NodeSeq)
	}	  
	val list  = rep(logEntry)
	import java.io.Reader
	def parseFile(file: Reader) = parseAll(list, file) match {
		case Success(res, _) => Some(res) 
		case Failure(f, _) => error(f ); None
		case Error(f, _) => error(f); None
	}
	def parseText(file: String) = parseAll(list, file) match {
		case Success(res, _) => Some(res) 
		case Failure(f, _) => error(f ); None
		case Error(f, _) => error(f); None
	}
}


class Artichoke extends CometActor {
    val Line = """(\d{4}-\d{2}-\d{2}) () \| (.*?) \| (.*?) \| (.*?)""".r// \| (.*?) \| (.*?)""".r
    val besk = "\\[([^\\]]*)\\] \\[([^\\]]*)\\] ([^\\n]*)".r
    val rammeLinje = "(\\S{3}) (\\d{1,2}), (\\d{4}) (\\d{1,2}:\\d{2}:\\d{2} \\S{2}) [^ ]* (\\S*)".r
    val rammeLinje2 = "([^\\:]*): ([^\\n]*)".r	

	val file = System.getProperty("file")
	var i = 1

    def shorten(text: String, maxLength: Int = 150, tail: Boolean = false) = {
        if (text.length > maxLength) {
			if (tail) {
				"..." + text.substring(text.length-maxLength+3)
			} else {
	            text.substring(0, maxLength - 3) + "..."
			}
        } else {
            text
        }
	}

	var parsed : List[(((DateTime, String, String, String), NodeSeq), Int)] = Nil

	override def lowPriority = {
		case Update => 
			new ParseServer ! Parse(this, file)
		case Parsed(list) => 
			import net.liftweb.util.ActorPing
			ActorPing.schedule(new ParseServer, Parse(this, file), intToTimeSpanBuilder(10) seconds)
			if (parsed.length != list.length) {
				parsed = list.zipWithIndex
				reRender(false)
			}
	}

	new ParseServer ! Parse(this, file)

    def render = {
        <h2 style="display:inline;">{file}</h2> ++
        (<table width="100%">
		{parsed.foldLeft(NodeSeq.Empty){
			case (x,(((date, level, logger, msg),b), id))=> x ++ 
				(<tr onclick={"Javascript: toggleDisplay(" + id + ");"} class={if (id % 2 == 0) "even" else "odd"}>
					<td style="width: 60px;">{date.toString("hh:mm:ss")}</td>
					<td style="width: 60px;">{level}</td>
					<td style="width: 100px;">{shorten(logger,30,true)}</td>
					<td>{shorten(msg)}</td>
				</tr> ++
				<tr id={"el_" + id} class="content" style="display: none;"><td colspan="4" class="content">{b}</td></tr>)
		}}
		</table>)
	}
}
 
class ParseServer extends LiftActor {
  override def messageHandler = {
    case Parse(comet, file) => {
      comet ! Parsed(ReportLineParser.parseFile(new FileReader(file)).get.reverse)
	}
    case _ =>  
  }
}  

