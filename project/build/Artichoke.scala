import sbt._
import java.io.File
import java.lang.System

class Artichoke(info: ProjectInfo) extends DefaultWebProject(info){
	val liftVersion = "2.1-M1"
	val scalaSnapshotsRepo = ScalaToolsSnapshots
	val scalaRepo = ScalaToolsReleases
	val mavenRepo = DefaultMavenRepository
	val rabitmqDep = "com.rabbitmq" % "amqp-client" % "1.7.2"  % "compile"
	val liftBaseDep = "net.liftweb" %% "lift-webkit" % liftVersion 
	val time = "org.scala-tools" % "time" % "2.8.0-SNAPSHOT-0.2-SNAPSHOT" 
	val servletApiDep = "javax.servlet" % "servlet-api" % "2.5" % "provided"
	val junitDep = "junit" % "junit" % "4.5" % "test"
	val slf4jDep = "org.slf4j" % "slf4j-log4j12" % "[1.5.6,)"
	val jetty7WebApp = "org.eclipse.jetty" % "jetty-webapp" % "7.0.2.RC0" % "test"
	val jetty7Server = "org.eclipse.jetty" % "jetty-server" % "7.0.2.RC0" % "test"
	val log4j = "org.slf4j" % "slf4j-log4j12" % "[1.5.6,)" % "test"
	val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test"

	//System.setProperty("file", """file:logfile""")

	// Don't compile before jetty-run
	override lazy val jettyRun = jettyRunTask(jettyInstance) describedAs("JettyRunDescription")

	// read html files from src dir
	override def jettyWebappPath  = webappPath

	// don't reload on change - works well with jrebel
	override def scanDirectories = Nil

}


