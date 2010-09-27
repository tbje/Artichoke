package no.trondbjerkestrand.artichoke.snippet

import no.trondbjerkestrand.artichoke.comet._

import net.liftweb.http.{S,SHtml}
import scala.xml._
import net.liftweb.http.js.JsCmds._

class Controls {
	def render = SHtml.ajaxButton("Update", ()=>{S.session.map(_.findComet("Artichoke").foreach(_ ! Update)); Noop})
}
