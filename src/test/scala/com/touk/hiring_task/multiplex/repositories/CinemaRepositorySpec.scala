package com.touk.hiring_task.multiplex.repositories

import com.touk.hiring_task.multiplex.CinemaConstants
import com.touk.hiring_task.multiplex.commons.SelectedPlaceUnavailableException
import com.touk.hiring_task.multiplex.dto.{OrderDto, PlaceDto}
import com.touk.hiring_task.multiplex.model.{Key, Place, Projection, Reservation}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext.Implicits.global

class CinemaRepositorySpec extends AnyFreeSpec with Matchers {

  sealed trait TestContext extends CinemaConstants {
    val fillRoomsService = new FillCinemaRoomsServiceImpl {
      override def getListProjections(): List[Projection] = List(projection1, projection2, projection3)

      override def getCinemaRooms(projections: List[Projection]): ConcurrentHashMap[Key, Reservation] = {
        val cinemaDB: ConcurrentHashMap[Key, Reservation] = new ConcurrentHashMap()
        cinemaDB.put(key1, reservation1)
        cinemaDB.put(key2, reservation2)
        cinemaDB
      }
    }
    val cinemaRepo = new CinemaRepositoryImpl(fillRoomsService)
  }

  "CinemaRepository should" - {
    "fetch projection by date" - {
      "return Nil" in new TestContext {
        val result = cinemaRepo.fetchProjectionsByDate(1200, 1220).futureValue

        result shouldBe Some(Nil)
      }

      "return one projection" in new TestContext {
        val result = cinemaRepo.fetchProjectionsByDate(1200, 1240).futureValue

        result shouldBe Some(List(projection1))
      }

      "return three projections" in new TestContext {
        val result = cinemaRepo.fetchProjectionsByDate(1200, 1450).futureValue

        result shouldBe Some(List(projection1, projection2, projection3))
      }
    }

    "take projection by id" - {
      "return None when projection not exists" in new TestContext {
        val result = cinemaRepo.takeProjectionById("id0").futureValue

        result shouldBe None
      }

      "return projection" in new TestContext {
        val result = cinemaRepo.takeProjectionById(projection1.id).futureValue

        result shouldBe Some(projection1)
      }
    }

    "fetchKeysDB" - {
      "return all keys" in new TestContext {
        val result = cinemaRepo.fetchKeysDB().futureValue

        result should be
      }
    }

    "storeReservation" - {
      "return exception when place is unavailable" in new TestContext {
        override val orderDto = OrderDto(projection1.id, List(PlaceDto(Place(1, 1), ticketType)), userName)

        val result = cinemaRepo.storeReservations(orderDto, expireAt, roomName).futureValue

        result shouldBe Left(SelectedPlaceUnavailableException)
      }


      "return () when reservation one completed" in new TestContext {
        override val orderDto = OrderDto(projection1.id, List(PlaceDto(Place(1, 3), ticketType)), userName)

        val result = cinemaRepo.storeReservations(orderDto, expireAt, roomName).futureValue

        result shouldBe Right(())
      }

      "return () when reservation three place completed " in new TestContext {
        override val orderDto = OrderDto(projection1.id, List(PlaceDto(Place(1, 3), ticketType), PlaceDto(Place(1, 4), ticketType), PlaceDto(Place(1, 5), ticketType)), userName)

        val result = cinemaRepo.storeReservations(orderDto, expireAt, roomName).futureValue
        result shouldBe Right(())
      }
    }
  }
}
