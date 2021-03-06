import sbt._
import Keys._
import com.typesafe.sbt.pgp.PgpKeys._
// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


ThisBuild / scalafmtOnCompile := true

val commonSettings = Seq(
  organization := "io.suzaku",
  version := Version.library,
  crossScalaVersions := Seq("2.11.12", "2.12.8"),
  scalaVersion := "2.12.8",
  scalacOptions := Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-language:experimental.macros",
    "-language:existentials",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq("-Xlint:-unused")
    case _             => Nil
  }),
  Compile / scalacOptions -= "-Ywarn-value-discard",
  Compile / doc / scalacOptions -= "-Xfatal-warnings",
  testFrameworks += new TestFramework("utest.runner.Framework"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "utest" % "0.6.7" % "test"
  )
)

val publishSettings = Seq(
  scmInfo := Some(
    ScmInfo(url("https://github.com/suzaku-io/diode"),
            "scm:git:git@github.com:suzaku-io/diode.git",
            Some("scm:git:git@github.com:suzaku-io/diode.git"))),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomExtra :=
    <url>https://github.com/suzaku-io/diode</url>
      <licenses>
        <license>
          <name>Apache 2.0 license</name>
          <url>http://www.opensource.org/licenses/Apache-2.0</url>
        </license>
      </licenses>
      <developers>
        <developer>
          <id>ochrons</id>
          <name>Otto Chrons</name>
          <url>https://github.com/ochrons</url>
        </developer>
      </developers>,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

val sourceMapSetting =
  Def.setting(
    if (isSnapshot.value) Seq.empty
    else
      Seq({
        val a = baseDirectory.value.toURI.toString.replaceFirst("[^/]+/?$", "")
        val g = "https://raw.githubusercontent.com/suzaku-io/diode"
        s"-P:scalajs:mapSourceURI:$a->$g/v${version.value}/${name.value}/"
      })
  )

def preventPublication(p: Project) =
  p.settings(
    publish := (()),
    publishLocal := (()),
    publishSigned := (()),
    publishLocalSigned := (()),
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
    packagedArtifacts := Map.empty
  )

lazy val diodeCore = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("diode-core"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "diode-core",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
  )
  .jsSettings(
    scalacOptions ++= sourceMapSetting.value
  )
  .jvmSettings()

lazy val diodeCoreJS = diodeCore.js

lazy val diodeCoreJVM = diodeCore.jvm

lazy val diodeData = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("diode-data"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "diode-data"
  )
  .jsSettings(
    scalacOptions ++= sourceMapSetting.value
  )
  .jvmSettings()
  .dependsOn(diodeCore)

lazy val diodeDataJS = diodeData.js

lazy val diodeDataJVM = diodeData.jvm

lazy val diode = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("diode"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "diode"
  )
  .dependsOn(diodeCore, diodeData)

lazy val diodeJS = diode.js

lazy val diodeJVM = diode.jvm

lazy val diodeDevtools = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("diode-devtools"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "diode-devtools"
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.7"
    ),
    scalacOptions ++= sourceMapSetting.value
  )
  .jvmSettings()
  .dependsOn(diodeCore)

lazy val diodeDevToolsJS = diodeDevtools.js

lazy val diodeDevToolsJVM = diodeDevtools.jvm

lazy val diodeReact = project
  .in(file("diode-react"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "diode-react",
    version := s"${Version.library}.${Version.sjsReact.filterNot(_ == '.')}",
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % Version.sjsReact
    ),
    scalacOptions ++= sourceMapSetting.value
  )
  .dependsOn(diodeJS)
  .enablePlugins(ScalaJSPlugin)

lazy val root = preventPublication(project.in(file(".")))
  .settings(commonSettings: _*)
  .aggregate(diodeJS,
             diodeJVM,
             diodeCoreJS,
             diodeCoreJVM,
             diodeDataJS,
             diodeDataJVM,
             diodeReact,
             diodeDevToolsJS,
             diodeDevToolsJVM)
