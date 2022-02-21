package com.touk.hiring_task.multiplex.service

import com.touk.hiring_task.multiplex.CinemaConstants
import com.touk.hiring_task.multiplex.commons._
import com.touk.hiring_task.multiplex.dto.{DetailsDto, MovieDto, PaymentDto, PlaceDto}
import com.touk.hiring_task.multiplex.model.{Key, Place, Projection, Reservation}
import com.touk.hiring_task.multiplex.repositories.{CinemaRepositoryImpl, FillCinemaRoomsServiceImpl}
import com.touk.hiring_task.multiplex.utils.ValidatorImpl
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext.Implicits.global

class CinemaServiceSpec extends AnyFreeSpec with Matchers with MockFactory {

  sealed trait FillRoomsServiceEmpty extends CinemaConstants {
    val fillRoomsService = new FillCinemaRoomsServiceImpl {
      override def getListProjections(): List[Projection] = List()

      override def getCinemaRooms(projections: List[Projection]): ConcurrentHashMap[Key, Reservation] = {
        val cinemaDB: ConcurrentHashMap[Key, Reservation] = new ConcurrentHashMap()
        cinemaDB
      }
    }
  }

  sealed trait FillRoomsService extends CinemaConstants {
    val fillRoomsService = new FillCinemaRoomsServiceImpl {
      override def getListProjections(): List[Projection] = List(projection1, projection2, projection3)

      override def getCinemaRooms(projections: List[Projection]): ConcurrentHashMap[Key, Reservation] = {
        val cinemaDB: ConcurrentHashMap[Key, Reservation] = new ConcurrentHashMap()
        cinemaDB.put(key1, reservation1)
        cinemaDB.put(key2, reservation2)
        cinemaDB
      }
    }
  }

  sealed trait TestEmptyListProjectionContext extends FillRoomsServiceEmpty {
    val cinemaRepo = new CinemaRepositoryImpl(fillRoomsService)
    val validator = mock[ValidatorImpl]
    val cinemaService = new CinemaServiceImpl(cinemaRepo, validator)
  }

  sealed trait TestContext extends FillRoomsService {
    val cinemaRepo = new CinemaRepositoryImpl(fillRoomsService)
    val validator = mock[ValidatorImpl]
    val cinemaService = new CinemaServiceImpl(cinemaRepo, validator)
  }

  "CinemaService should" - {
    "getMovies" - {
      "return exception when start date is  from past" in new TestEmptyListProjectionContext {
        (validator.isNotPastDate _).expects(123).returning(false)

        val result = cinemaService.getMovies(123, 1534).futureValue

        result shouldBe Left(InvalidStartProjectionDateException)
      }

      "return exception when finish date is too distant" in new TestEmptyListProjectionContext {
        (validator.isNotPastDate _).expects(123).returning(true)
        (validator.isNotTooDistantDate _).expects(1534).returning(false)

        val result = cinemaService.getMovies(123, 1534).futureValue

        result shouldBe Left(InvalidFinishProjectionDateException)
      }

      "return exception when start date is grater than finish" in new TestEmptyListProjectionContext {
        (validator.isNotPastDate _).expects(1534).returning(true)
        (validator.isNotTooDistantDate _).expects(123).returning(true)
        (validator.isStartAtLessFinishAt _).expects(1534, 123).returning(false)

        val result = cinemaService.getMovies(1534, 123).futureValue

        result shouldBe Left(InvalidRangeOfDateException)
      }

      "return exception when projection not found" in new TestEmptyListProjectionContext {
        (validator.isNotPastDate _).expects(1234).returning(true)
        (validator.isNotTooDistantDate _).expects(1555).returning(true)
        (validator.isStartAtLessFinishAt _).expects(1234, 1555).returning(true)

        val result = cinemaService.getMovies(1234, 1555).futureValue

        result shouldBe Left(ProjectionNotFoundException)
      }

      "return exception when projection cinema repo returns empty list" in new TestEmptyListProjectionContext {
        (validator.isNotPastDate _).expects(1234).returning(true)
        (validator.isNotTooDistantDate _).expects(1555).returning(true)
        (validator.isStartAtLessFinishAt _).expects(1234, 1555).returning(true)

        val result = cinemaService.getMovies(1234, 1555).futureValue

        result shouldBe Left(ProjectionNotFoundException)
      }

      "return list moviesDto" in new TestContext {
        val listMovieDto = List(
          MovieDto("id1", "Shrek 1", "01.01.1970 01:00 (Europe/Warsaw)"),
          MovieDto("id2", "Shrek 2", "01.01.1970 01:00 (Europe/Warsaw)"),
          MovieDto("id3", "Shrek 3", "01.01.1970 01:00 (Europe/Warsaw)"))

        (validator.isNotPastDate _).expects(1234).returning(true)
        (validator.isNotTooDistantDate _).expects(1555).returning(true)
        (validator.isStartAtLessFinishAt _).expects(1234, 1555).returning(true)

        val result = cinemaService.getMovies(1234, 1555).futureValue

        result shouldBe Right(listMovieDto)
      }
    }


    "getDetailsById" - {
      "return exception when projection not found" in new TestEmptyListProjectionContext {
        val result = cinemaService.getDetailsById(projection3.id).futureValue

        result shouldBe Left(ProjectionNotFoundException)
      }

      "return details od projection1" in new TestContext {

        val result = cinemaService.getDetailsById(projection1.id).futureValue

        result shouldBe Right(
          DetailsDto("id1", "01.01.1970 01:00 (Europe/Warsaw)", "Shrek 1", "A", List(
            Place(1, 3), Place(1, 4), Place(1, 5),
            Place(2, 1), Place(2, 2), Place(2, 3), Place(2, 4), Place(2, 5),
            Place(3, 1), Place(3, 2), Place(3, 3), Place(3, 4), Place(3, 5),
            Place(4, 1), Place(4, 2), Place(4, 3), Place(4, 4), Place(4, 5),
            Place(5, 1), Place(5, 2), Place(5, 3), Place(5, 4), Place(5, 5))
          ))
      }
    }

    "processOrder" - {
      "return exception when projection not found" in new TestEmptyListProjectionContext {
        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(ProjectionNotFoundException)
      }

      "return exception when list ordered place is empty" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(false)

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidPlaceCountException)
      }

      "return exception when projection start in less than 15 minutes" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(false)

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidMovieSelectedException)
      }

      "return exception when first name length not valid" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto.name).returning(Left(InvalidFirstNameLengthException))

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidFirstNameLengthException)
      }

      "return exception when family name length not valid" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto.name).returning(Left(InvalidFamilyNameLengthException))

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidFamilyNameLengthException)
      }

      "return exception when second family name length not valid" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto.name).returning(Left(InvalidSecondFamilyNameLengthException))

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidSecondFamilyNameLengthException)
      }

      "return exception when first name format not valid" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto.name).returning(Left(InvalidFirstNameFormatException))

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidFirstNameFormatException)
      }

      "return exception when family name format not valid" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto.name).returning(Left(InvalidFamilyNameFormatException))

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidFamilyNameFormatException)
      }

      "return exception when second family name format not valid" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto.name).returning(Left(InvalidSecondFamilyNameFormatException))

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(InvalidSecondFamilyNameFormatException)
      }

      "return exception when left empty space between seats" in new TestContext {
        (validator.isListPlaceNonEmpty _).expects(orderDto.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto.name).returning(Right(orderDto.name))

        val result = cinemaService.processOrder(orderDto).futureValue

        result shouldBe Left(SelectedPlaceNotAvailableException)
      }

      "return exception when selected place unavailable" in new TestContext {
        val orderDto2 = orderDto.copy(places = List(
          PlaceDto(Place(1, 1), "ADULT"),
          PlaceDto(Place(1, 4), "ADULT"),
          PlaceDto(Place(2, 1), "ADULT"),
        ))

        (validator.isListPlaceNonEmpty _).expects(orderDto2.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto2.name).returning(Right(orderDto2.name))

        val result = cinemaService.processOrder(orderDto2).futureValue

        result shouldBe Left(SelectedPlaceNotAvailableException)
      }

      "return paymentDto after successful store reservation" in new TestContext {
        val orderDto2 = orderDto.copy(places = List(
          PlaceDto(Place(1, 3), "ADULT"),
          PlaceDto(Place(1, 4), "ADULT"),
          PlaceDto(Place(2, 1), "ADULT"),
        ))

        (validator.isListPlaceNonEmpty _).expects(orderDto2.places).returning(true)
        (validator.isNotExpired _).expects(projection1.startAt).returning(true)
        (validator.isUserNameValid _).expects(orderDto2.name).returning(Right(orderDto2.name))

        val result = cinemaService.processOrder(orderDto2).futureValue

        result shouldBe Right(PaymentDto(75.0, 1235))
      }
    }
  }
}
