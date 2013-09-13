import sbt._
import Keys._

import org.scalatra.sbt.ScalatraPlugin._

import sbtassembly.Plugin._
import sbtassembly.Plugin.AssemblyKeys._

object EmbeddedJettyBuild extends Build {

  // using constants here
  val Organization = "org.scalatra"
  val Name = "scalatra-embedded-jetty"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.10.2"
  val ScalatraVersion = "2.2.1"

  // basic project settings
  val projectSettings = Seq(
    organization := Organization,
    name := Name,
    version := Version,
    scalaVersion := ScalaVersion,
    resolvers += Classpaths.typesafeReleases,
    libraryDependencies ++= Seq(
      "org.scalatra" %% "scalatra" % ScalatraVersion,
      "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
      "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
      "org.scalatra" %% "scalatra-json" % ScalatraVersion,
      "org.json4s"   %% "json4s-jackson" % "3.2.4",
      "ch.qos.logback" % "logback-classic" % "1.0.13",
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "compile;container",
      "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
    )
  )

  // sbt-assembly settings
  val myAssemblySettings = assemblySettings ++ Seq(

    // handle conflicts during assembly task, adjust as needed
    mergeStrategy in assembly <<= (mergeStrategy in assembly) {
      (old) => {
        case "about.html" => MergeStrategy.first
        case x => old(x)
      }
    },

    // copy web resources from <projectBase>/src/main/webapp to <resourceManaged>/webapp folder during build
    // they are available in the final .jar in the folder jar!/webapp
    resourceGenerators in Compile <+= (resourceManaged, baseDirectory) map {
      (managedBase, base) =>
        val webappBase = base / "src" / "main" / "webapp"
        for {
          (from, to) <- webappBase ** "*" x rebase(webappBase, managedBase / "main" / "webapp")
        } yield {
          Sync.copy(from, to)
          to
        }
    }
  )

  // create a Project with the previous settings
  lazy val project = Project("scalatra-embedded-jetty", file("."))
    .settings(scalatraWithJRebel :_*)
    .settings(projectSettings :_*)
    .settings(myAssemblySettings :_*)

}
