package com.touk.hiring_task.multiplex.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route

import scala.concurrent.Future

trait ServerRunner {
  def runServer(route: Route, port: Int)(implicit as: ActorSystem) = new WebServer(route, port).run()
}

class WebServer(route: Route, port: Int)(implicit as: ActorSystem) {
  def run(): Future[ServerBinding] = {
    val interface = "localhost"
    lazy val binding = Http().newServerAt(interface, port).bind(route)
    binding
  }
}
