import sbt._
import Keys._

import org.scalatra.sbt.ScalatraPlugin._

import classpath.ClasspathUtilities
import Project.Initialize
import Keys._
import Defaults._

object DistPlugin2 extends Plugin {

  object DistKeys {
    val dist = TaskKey[File]("dist", "Build a distribution, assemble the files, create a launcher and make an archive.")
    val assembleJarsAndClasses = TaskKey[Seq[File]]("assemble-jars-and-classes", "Assemble jars and classes")
    val memSetting = SettingKey[String]("mem-setting", "The maximium and initial heap size.")
    val permGenSetting = SettingKey[String]("perm-gen-setting", "The PermGen size.")
    val envExports = SettingKey[Seq[String]]("env-exports", "The exports which will be expored in the launcher script.")
  }

  import DistKeys._
  val Dist = config("dist")

  private def assembleJarsAndClassesTask: Initialize[Task[Seq[File]]] =
    (fullClasspath in Runtime, excludeFilter in Dist, target in Dist) map { (cp, excl, tgt) =>
      IO.delete(tgt)
      val (libs, dirs) = cp.map(_.data).toSeq partition ClasspathUtilities.isArchive
      val jars = libs.descendantsExcept("*", excl) x flat(tgt / "lib")
      val classesAndResources = dirs flatMap { dir =>
        val files = dir.descendantsExcept("*", excl)
        files x rebase(dir, tgt / "lib")
      }

      (IO.copy(jars) ++ IO.copy(classesAndResources)).toSeq
    }


  private def createLauncherScriptTask(base: File, name: String, libFiles: Seq[File], mainClass: Option[String], javaOptions: Seq[String], envExports: Seq[String], logger: Logger): File = {
    val f = base / "bin" / name
    if (!f.getParentFile.exists()) f.getParentFile.mkdirs()
    IO.write(f, createScriptString(base, name, libFiles, mainClass, javaOptions, envExports))
    "chmod +x %s".format(f.getAbsolutePath) ! logger
    f
  }

  private def createScriptString(base: File, name: String, libFiles: Seq[File], mainClass: Option[String], javaOptions: Seq[String], envExports: Seq[String]): String = {
    """
      |#!/bin/env bash
      |
      |export CLASSPATH="lib:%s"
      |export JAVA_OPTS="%s"
      |%s
      |
      |java $JAVA_OPTS -cp $CLASSPATH %s
      |
    """.stripMargin.format(classPathString(base, libFiles), javaOptions.mkString(" "), envExports.map(e => "export %s".format(e)).mkString("\n"), mainClass.getOrElse(""))
  }

  private def classPathString(base: File, libFiles: Seq[File]) = {
    (libFiles filter ClasspathUtilities.isArchive map (_.relativeTo(base))).flatten mkString java.io.File.pathSeparator
  }

  private[this] val webappResources = SettingKey[Seq[File]]("webapp-resources")

  private def distTask: Initialize[Task[File]] =
    (webappResources in Compile, excludeFilter in Dist, assembleJarsAndClasses in Dist, target in Dist, name in Dist, version in Dist, mainClass in Dist, javaOptions in Dist, envExports in Dist, streams) map { (webRes, excl, libFiles, tgt, nm, ver, mainClass, javaOptions, envExports, s) =>
      val launch = createLauncherScriptTask(tgt, nm, libFiles, mainClass, javaOptions, envExports, s.log)
      val logsDir = tgt / "logs"
      if (!logsDir.exists()) logsDir.mkdirs()

      val resourceFiles = webRes flatMap { wr =>
        s.log.info("Adding " + wr + " to dist in " + tgt + "/webapp")
        val files = wr.descendantsExcept("*", excl)
        IO.copy(files x rebase(wr, tgt / "webapp"))
      }

      val files = libFiles ++ Seq(launch, logsDir) ++ resourceFiles

      val zipFile = tgt / ".." / (nm + "-" + ver + ".zip")
      val paths = files x rebase(tgt, nm)
      IO.zip(paths, zipFile)
      zipFile
    }

  val distSettings = Seq(
    excludeFilter in Dist := HiddenFileFilter,
    target in Dist <<= (target in Compile)(_ / "dist"),
    assembleJarsAndClasses in Dist <<= assembleJarsAndClassesTask,
    dist in Dist <<= distTask,
    dist <<= dist in Dist,
    name in Dist <<= name,
    mainClass in Dist := Some("ScalatraLauncher"),
    memSetting in Dist := "1g",
    permGenSetting in Dist := "128m",
    envExports in Dist := Seq(),
    javaOptions in Dist <++= (memSetting in Dist, permGenSetting in Dist) map { (mem, perm) =>
      val rr = Seq(
        "-Xms" + mem,
        "-Xmx" + mem,
        "-XX:PermSize="+perm,
        "-XX:MaxPermSize="+perm)
      rr
    }
  )

}

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

  // dist settings
  import org.scalatra.sbt.DistPlugin
  import org.scalatra.sbt.DistPlugin._
  import org.scalatra.sbt.DistPlugin.DistKeys._

  val myDistSettings = DistPlugin.distSettings ++ Seq(
    mainClass in Dist := Some("ScalatraLauncher"),
    memSetting in Dist := "2g",
    permGenSetting in Dist := "256m",
    envExports in Dist := Seq("LC_CTYPE=en_US.UTF-8", "LC_ALL=en_US.utf-8"),
    javaOptions in Dist ++= Seq("-Xss4m",
      "-Dfile.encoding=UTF-8",
      "-Dlogback.configurationFile=logback.prod.xml",
      "-Dorg.scalatra.environment=production")
  )

  // create a Project with the previous settings
  lazy val project = Project("scalatra-embedded-jetty", file("."))
    .settings(scalatraWithJRebel :_*)
    .settings(projectSettings :_*)
    .settings(myDistSettings :_*)
    // .settings(com.typesafe.sbt.SbtNativePackager.packagerSettings :_*)

}
