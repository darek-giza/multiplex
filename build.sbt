import Dependencies._

name := "multiplex"
version := "0.1"
scalaVersion := "2.13.8"

lazy val root = project
  .in(file("."))
  .settings(
    name := "multiplex",
    libraryDependencies ++= Seq(ScalaTest, scalamock) ++ circe ++ akka ++ http4s
  )
