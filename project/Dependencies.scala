import sbt._

object Dependencies {
  val ScalaTest = "org.scalatest" %% "scalatest" % "3.2.11"
  val scalamock = "org.scalamock" %% "scalamock" % "5.2.0" % Test
  val akkaVersion = "2.6.18"
  val akkaHttpVersion = "10.2.8"
  val akkaHttpJsonSerializersVersion = "1.39.2"


  val circe = {
    val v = "0.14.1"
    Seq(
      "io.circe" %% "circe-core" % v,
      "io.circe" %% "circe-generic" % v,
      "io.circe" %% "circe-parser" % v,
    )
  }
  val http4s = {
    val v = "1.0.0-M23"
    Seq(
      "org.http4s" %% "http4s-dsl" % v,
      "org.http4s" %% "http4s-blaze-server" % v
    )
  }

  def akka: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "de.heikoseeberger" %% "akka-http-circe" % akkaHttpJsonSerializersVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test

  )
}
