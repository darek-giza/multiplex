package com.touk.hiring_task.multiplex.repositories

import com.touk.hiring_task.multiplex.model.{BookingStatus, Key, Place, Reservation, RoomName, TicketType, User}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap

class InitCinemaDBSpec extends AnyFreeSpec with Matchers {

  sealed trait TestContext {
    val roomService = new FillCinemaRoomsServiceImpl()
  }

  "FillCinemaRooms should" - {
    "create initial cinema" in new TestContext {

      val projectionsList = roomService.getListProjections()
      private val keyToReservation: ConcurrentHashMap[Key, Reservation] = roomService.getCinemaRooms(projectionsList)

      keyToReservation.forEach((k, v) => println("Klucz: " + k + " == " + v))
    }
  }
}
