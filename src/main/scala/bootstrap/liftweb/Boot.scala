package bootstrap.liftweb

import net.liftweb.util._
import net.liftweb.common.{Logger=>LiftLogger, _}
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.http.provider.HTTPRequest
import net.liftweb.http.js.jquery.JQuery14Artifacts

import net.liftweb.http._
import provider.servlet.containers.Jetty7AsyncProvider
import scala.xml._
import net.liftweb.util.PCDataXmlParser

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */

class Boot extends LiftLogger {

  def boot {
	
	// where to search snippet
    LiftRules.addToPackages("no.trondbjerkestrand.artichoke")

	LiftRules.jsArtifacts=JQuery14Artifacts
	LiftRules.localizeStringToXml= (str:String) => PCDataXmlParser("<xml:group>"+str+"</xml:group>").map(_.asInstanceOf[Document].docElem.child).getOrElse(Text(str)) : NodeSeq
	LiftRules.resourceNames = "lift" :: LiftRules.resourceNames    

    // Build SiteMap
    val entries = 
      (Menu("Home") / "index") ::
      Nil 

    LiftRules.setSiteMap(SiteMap(entries:_*))

    LiftRules.early.append(makeUtf8)

    // Jetty 7 continuations to power Lift comet support
    LiftRules.servletAsyncProvider = (req) => 
        new Jetty7AsyncProvider(req)
	}

	/**
	* Disable lifts autologging config if log4j.configuration is provided
	*/
	import java.net.URL
	try {
		Option(System.getProperty("log4j.configuration")).map(new URL(_)) match {
			case Some(loggerUrl) => 
				LiftLogger.setup = Full(Log4j.withFile(loggerUrl))
			case _ => 
		}
	} catch {
		case e:java.net.MalformedURLException => 
			error("Value of log4j.configuration is not a valid url: " + System.getProperty("log4j.configuration"))
			error(e.getMessage)
	} 

	/**
	* Force the request to be UTF-8
	*/
	private def makeUtf8(req: HTTPRequest) {
		req.setCharacterEncoding("UTF-8")
	}
}
