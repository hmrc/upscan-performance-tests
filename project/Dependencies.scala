import sbt._

object Dependencies {

  val test = Seq(
    "io.gatling"             % "gatling-test-framework"    % "3.5.1"  % Test,
    "io.gatling.highcharts"  % "gatling-charts-highcharts" % "3.5.1"  % Test,
    "com.typesafe"           % "config"                    % "1.3.0"  % Test,
    "uk.gov.hmrc"            %% "performance-test-runner"  % "5.6.0"  % Test,
    "io.gatling.vtd"         % "gatling-vtd"               % "2.2.0"  % Test,
    "org.json4s"             %% "json4s-jackson"           % "4.0.7" % Test,
    "org.json4s"             %% "json4s-native"            % "4.0.7" % Test
  )
}
