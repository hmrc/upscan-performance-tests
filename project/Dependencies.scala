import sbt._

object Dependencies {

  val test = Seq(
    "uk.gov.hmrc"  %% "performance-test-runner"  % "6.2.0" % Test,
    "org.json4s"   %% "json4s-jackson"           % "4.0.7" % Test,
    "org.json4s"   %% "json4s-native"            % "4.0.7" % Test
  )
}
