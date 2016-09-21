name := "hello"
version := "1.0"
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
  "ch.qos.logback" %  "logback-classic" % "1.1.7", // used by scala logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
)

lazy val root = (project in file("."))

