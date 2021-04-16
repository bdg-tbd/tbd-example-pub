
name := "tbd-example-project"

version := "0.0.1"

scalaVersion := "2.12.10"
val sparkVersion = "3.0.1"
val sequilaVersion = "0.6.8"

libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
libraryDependencies += "org.apache.spark" %% "spark-hive" % "3.0.1" % "provided"
libraryDependencies += "org.biodatageeks" % "sequila_2.12" % sequilaVersion
libraryDependencies += "com.github.mrpowers" %% "spark-daria" % "1.0.0"
libraryDependencies += "ch.cern.sparkmeasure" %% "spark-measure" % "0.17"
libraryDependencies += "org.rogach" %% "scallop" % "4.0.2"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "com.github.mrpowers" %% "spark-fast-tests" % "1.0.0" % "test"
libraryDependencies += "com.holdenkarau" %% "spark-testing-base" % s"${sparkVersion}_1.0.0" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"


// test suite settings
fork in Test := true
parallelExecution in Test := false
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
// Show runtime of tests
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")

test in assembly := {}

// JAR file settings

// don't include Scala in the JAR file
//assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

// Add the JAR file naming conventions described here: https://github.com/MrPowers/spark-style-guide#jar-files
// You can add the JAR file naming conventions by running the shell script
assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", xs@_*) => MergeStrategy.first
  case PathList("org", xs@_*) => MergeStrategy.first
  case PathList("jakarta", xs@_*) => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("javax", xs@_*) => MergeStrategy.first
  case PathList("com", xs@_*) => MergeStrategy.first
  case PathList("shadeio", xs@_*) => MergeStrategy.first
  case PathList("au", xs@_*) => MergeStrategy.first
  case PathList("htsjdk", xs@_*) => MergeStrategy.first
  case ("META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat") => MergeStrategy.first
  case ("images/ant_logo_large.gif") => MergeStrategy.first
  case "overview.html" => MergeStrategy.rename
  case "mapred-default.xml" => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "parquet.thrift" => MergeStrategy.last
  case "plugin.xml" => MergeStrategy.last
  case "codegen/config.fmpp" => MergeStrategy.last
  case "git.properties" => MergeStrategy.last
  case "jetty-dir.css" => MergeStrategy.last
  case "module-info.class" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}