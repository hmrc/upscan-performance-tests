val scalaOptions = Seq(
  "-feature"
)

val projectSettings = Seq(
  name := "upscan-performance-tests",
  organization := "uk.gov.hmrc",
  version := "999-SNAPSHOT",
  scalaVersion := "2.13.12"
)

lazy val root = (project in file("."))
  .enablePlugins(GatlingPlugin, CorePlugin, JvmPlugin, IvyPlugin, SbtAutoBuildPlugin)
  .settings(projectSettings)
  .settings(showClasspath)
  .settings(scalacOptions ++= scalaOptions)
  .settings(libraryDependencies ++= Dependencies.test)
  .settings(
    retrieveManaged := true,
    initialCommands in console := "import uk.gov.hmrc._",
    parallelExecution in Test := false,
    // Enabling sbt-auto-build plugin provides DefaultBuildSettings with default `testOptions` from `sbt-settings` plugin.
    // These testOptions are not compatible with `sbt gatling:test`. So we have to override testOptions here.
    testOptions in Test := Seq.empty
  )


lazy val showClasspath = taskKey[Unit]("show-classpath") := println((fullClasspath in Test).value.files.absString)
