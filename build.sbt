name := "secrets"

version := "0.1.0"

scalaVersion := "2.10.3"

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "com.google.code.gson" % "gson" % "2.2.4",
  "log4j" % "log4j" % "1.2.17",
  "io.spray" %%  "spray-json" % "1.2.5"
)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)
