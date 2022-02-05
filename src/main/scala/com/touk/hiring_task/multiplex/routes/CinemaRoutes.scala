package com.touk.hiring_task.multiplex.routes

import akka.http.scaladsl.server.{Directives, Route}
import com.touk.hiring_task.multiplex.dto.OrderDto
import com.touk.hiring_task.multiplex.service.CinemaService
import com.touk.hiring_task.multiplex.utils.SystemClock
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

class CinemaRoutes(cinemaService: CinemaService)
                  (implicit ec: ExecutionContext) extends Routes with Directives with FailFastCirceSupport with SystemClock {

  val routes: Route =
    concat(
      post {
        path("order") {
          entity(as[OrderDto]) { order =>
            val reservation = cinemaService.processOrder(order)
            completeEitherOrError(reservation)
          }
        }
      } ~
        get {
          path("movies") {
            parameters("start".as[Long], "finish".as[Long]) { (start, finish) =>
              val list = cinemaService.getMovies(start, finish)
              completeEitherOrError(list)
            }
          } ~
            get {
              path("details")
              parameter("id".as[String]) { id =>
                completeEitherOrError(cinemaService.getDetailsById(id))
              }
            }
        }
    )
}
