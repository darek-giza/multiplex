package com.touk.hiring_task.multiplex

import akka.actor.ActorSystem
import com.touk.hiring_task.multiplex.repositories.{CinemaRepositoryImpl, FillCinemaRoomsServiceImpl}
import com.touk.hiring_task.multiplex.routes.CinemaRoutes
import com.touk.hiring_task.multiplex.server.ServerRunner
import com.touk.hiring_task.multiplex.service.CinemaServiceImpl
import com.touk.hiring_task.multiplex.utils.{SystemClock, ValidatorImpl}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

object Main extends App with ServerRunner with SystemClock {

  val appName = "multiplex-api-system"
  implicit val system: ActorSystem = ActorSystem(appName)

  val serverPool = ExecutionContext.fromExecutor(Executors.newWorkStealingPool())

  val validator = new ValidatorImpl
  val fillCinemaService = new FillCinemaRoomsServiceImpl
  val cinemaRepository = new CinemaRepositoryImpl(fillCinemaService)(serverPool)
  val cinemaService = new CinemaServiceImpl(cinemaRepository, validator)(serverPool)
  val cinemaRoutes = new CinemaRoutes(cinemaService)(serverPool)

  val bindingFuture = runServer(cinemaRoutes.routes, 8080)(system)

  val startNow = nowMillis.plusMinutes(3).toInstant.toEpochMilli
  val finish1D = nowMillis.plusDays(1).minusMinutes(10).toInstant.toEpochMilli
  val finish7D = nowMillis.plusDays(7).minusMinutes(10).toInstant.toEpochMilli

  println("Server now online. \n")
  println("Get movies by start date [Long] and finish date [Long] as url parameters. ")
  println(s"today's - please navigate to http://localhost:8080/movies?start=${startNow}&finish=${finish1D}")
  println(s"week's - please navigate to http://localhost:8080/movies?start=${startNow}&finish=${finish7D} \n")
  println("take movie details - please navigate to http://localhost:8080/movies/details?id=  and insert as parameter id [String] of selected movie \n")
  println("to reserve places for movie go to POSTMAN and make POST on http://localhost:8080/movies with [OrderDto] as Body \n")


  println("to run example script execute in new terminal window:  ./example.sh")

  println("\n\nPress RETURN to stop...")

  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
