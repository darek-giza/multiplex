package com.touk.hiring_task.multiplex

import com.touk.hiring_task.multiplex.dto._
import com.touk.hiring_task.multiplex.model._

trait CinemaConstants {
  val startAt: Long = 12345
  val finishAt: Long = 1237
  val expireAt = 1234
  val projectionId = "id1"
  val roomName = "A"
  val userName = "John Black"
  val ticketType = "ADULT"

  val place11 = Place(1, 1)
  val place12 = Place(1, 2)
  val listPlaceDto = List(PlaceDto(place11, "ADULT"), PlaceDto(place12, "CHILD"))
  val detailsDto = DetailsDto("id1", startAt.toString, "Movie title", "A", List(place11, place12))
  val orderDto = OrderDto("id1", listPlaceDto, "Dariusz Giza")
  val paymentDto = PaymentDto(37.5, 123456)

  val key1 = Key("id1", "A", Place(1, 1))
  val key2 = Key("id1", "A", Place(1, 2))

  val reservation1 = Reservation(User("Darek Giza"), TicketType.ADULT, BookingStatus.UNPAID, 1234)
  val reservation2 = Reservation(User("Marek Miza"), TicketType.ADULT, BookingStatus.UNPAID, 1234)

  val projection1 = Projection("id1", Room(RoomName.A), 1235, "Shrek 1")
  val projection2 = Projection("id2", Room(RoomName.A), 1321, "Shrek 2")
  val projection3 = Projection("id3", Room(RoomName.A), 1411, "Shrek 3")
}
