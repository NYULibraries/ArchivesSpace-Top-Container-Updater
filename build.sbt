scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.json4s" %% "json4s-jackson" % "3.6.0",
  "com.typesafe" % "config" % "1.3.2",
  "org.rogach" %% "scallop" % "3.1.3"
)