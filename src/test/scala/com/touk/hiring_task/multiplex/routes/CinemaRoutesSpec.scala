package com.touk.hiring_task.multiplex.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import com.touk.hiring_task.multiplex.CinemaConstants
import com.touk.hiring_task.multiplex.commons._
import com.touk.hiring_task.multiplex.dto.MovieDto
import com.touk.hiring_task.multiplex.service.CinemaService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.generic.auto.exportEncoder
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import scala.concurrent.Future

class CinemaRoutesSpec extends AnyFreeSpecLike with Matchers with MockFactory with ScalatestRouteTest with FailFastCirceSupport {

  sealed trait TestContext extends CinemaConstants {
    implicit val customTimeout = RouteTestTimeout(2.seconds)

    val cinemaService: CinemaService = mock[CinemaService]

    val routes: Route =
      pathPrefix("test") {
        new CinemaRoutes(cinemaService).routes
      }
  }

  "CinemaRoutes" - {
    "in GET /movies" - {
      "fail - when start projection date is past date" in new TestContext {
        (cinemaService.getMovies _).expects(startAt, finishAt).returning(Future.successful(Left(InvalidStartProjectionDateException)))

        Get(s"/test/movies?start=$startAt&finish=$finishAt") ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid date, should not be past","errorCode":"INVALID_START_DATE"}"""
        }
      }

      "fail - when finish date is too distant" in new TestContext {
        (cinemaService.getMovies _).expects(startAt, finishAt).returning(Future.successful(Left(InvalidFinishProjectionDateException)))

        Get(s"/test/movies?start=$startAt&finish=$finishAt") ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid date, screenings are only available for the next week, should be less then now plus 7 days","errorCode":"INVALID_FINISH_DATE"}"""
        }
      }

      "fail - when start date is les than finish date" in new TestContext {
        (cinemaService.getMovies _).expects(startAt, finishAt).returning(Future.successful(Left(InvalidRangeOfDateException)))

        Get(s"/test/movies?start=$startAt&finish=$finishAt") ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid date, start date should be less than finish date","errorCode":"INVALID_DATE_RANGE"}"""
        }
      }

      "return empty list when no projection exists" in new TestContext {
        (cinemaService.getMovies _).expects(startAt, finishAt).returning(Future.successful(Right(Nil)))

        Get(s"/test/movies?start=$startAt&finish=$finishAt") ~> routes ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Json].noSpaces shouldBe """[]"""
        }
      }

      "successful - return list movieDto " in new TestContext {
        val listMovieDto = List(MovieDto("1", "title", "1234"))

        (cinemaService.getMovies _).expects(startAt, finishAt).returning(Future.successful(Right(listMovieDto)))

        Get(s"/test/movies?start=$startAt&finish=$finishAt") ~> routes ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Json].noSpaces shouldBe """[{"id":"1","title":"title","startAt":"1234"}]"""
        }
      }
    }

    "in GET /movies/details" - {
      "fail - when no connection with data base" in new TestContext {
        (cinemaService.getDetailsById _).expects(projectionId).returning(Future.successful(Left(InternalServerError)))

        Get(s"/test/movies/details?id=$projectionId") ~> routes ~> check {
          status shouldBe StatusCodes.InternalServerError
          responseAs[Json].noSpaces shouldBe """{"message":"Internal server error","errorCode":"INTERNAL_ERROR"}"""
        }
      }

      "fail - when projection not found" in new TestContext {
        (cinemaService.getDetailsById _).expects(projectionId).returning(Future.successful(Left(ProjectionNotFoundException)))

        Get(s"/test/movies/details?id=$projectionId") ~> routes ~> check {
          status shouldBe StatusCodes.NotFound
          responseAs[Json].noSpaces shouldBe """{"message":"Projection not found","errorCode":"PROJECTION_NOT_FOUND"}"""
        }
      }

      "success - returns movie details" in new TestContext {
        (cinemaService.getDetailsById _).expects(projectionId).returning(Future.successful(Right(detailsDto)))

        Get(s"/test/movies/details?id=$projectionId") ~> routes ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Json].noSpaces shouldBe """{"id":"id1","startAt":"12345","title":"Movie title","roomName":"A","places":[{"row":1,"seat":1},{"row":1,"seat":2}]}"""
        }
      }
    }

    "in POST /order" - {
      "fail - when no connection with data base" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InternalServerError)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.InternalServerError
          responseAs[Json].noSpaces shouldBe """{"message":"Internal server error","errorCode":"INTERNAL_ERROR"}"""
        }
      }

      "fail - when projection not found" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(ProjectionNotFoundException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.NotFound
          responseAs[Json].noSpaces shouldBe """{"message":"Projection not found","errorCode":"PROJECTION_NOT_FOUND"}"""
        }
      }

      "fail - when list selected place is empty" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidPlaceCountException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid place count, list of place must not be empty","errorCode":"INVALID_PLACE_COUNT"}"""
        }
      }

      "fail - when movie start in less than 15 minutes" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidMovieSelectedException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid movie selected, movie start in less than 15 minutes","errorCode":"INVALID_MOVIE_SELECTED"}"""
        }
      }

      "fail - when invalid first name length" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidFirstNameLengthException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid first name length, should contains more than three char","errorCode":"INVALID_USERNAME_LENGTH"}"""
        }
      }

      "fail - when invalid family name length" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidFamilyNameLengthException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid family name length, should contains more than three char","errorCode":"INVALID_USERNAME_LENGTH"}"""
        }
      }

      "fail - when invalid second family name length" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidSecondFamilyNameLengthException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid second family name length, should contains more than three char","errorCode":"INVALID_USERNAME_LENGTH"}"""
        }
      }

      "fail - when invalid first name format" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidFirstNameFormatException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid first name format, first character should be uppercase and rest lowercase","errorCode":"INVALID_USERNAME_FORMAT"}"""
        }
      }

      "fail - when invalid family name format" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidFamilyNameFormatException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid family name format, first character should be uppercase and rest lowercase","errorCode":"INVALID_USERNAME_FORMAT"}"""
        }
      }

      "fail - when invalid second family name format" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(InvalidSecondFamilyNameFormatException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Invalid second family name format, first character should be uppercase and rest lowercase","errorCode":"INVALID_USERNAME_FORMAT"}"""
        }
      }

      "fail - when selected place not available" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(SelectedPlaceNotAvailableException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Selected place not available, should be no free space left","errorCode":"INVALID_SELECTED_PLACE"}"""
        }
      }

      "fail - when selected place unavailable" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Left(SelectedPlaceUnavailableException)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Json].noSpaces shouldBe """{"message":"Selected place unavailable","errorCode":"UNAVAILABLE_PLACE"}"""
        }
      }

      "success - store reservation and return paymentDto" in new TestContext {
        (cinemaService.processOrder _).expects(orderDto).returning(Future.successful(Right(paymentDto)))

        Post("/test/order", orderDto) ~> routes ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Json].noSpaces shouldBe """{"amount":37.5,"expireAt":123456}"""
        }
      }
    }
  }
}
