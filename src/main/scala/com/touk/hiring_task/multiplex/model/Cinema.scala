package com.touk.hiring_task.multiplex.model

import com.touk.hiring_task.multiplex.model.BookingStatus.BookingStatus
import com.touk.hiring_task.multiplex.model.RoomName.RoomName
import com.touk.hiring_task.multiplex.model.TicketType.TicketType

import scala.language.implicitConversions
import scala.util.Try

case class User(name: String)

case class Place(row: Int, seat: Int)

case class Room(name: RoomName, row: Int = 5, placeInRow: Int = 5)

case class Projection(id: String, room: Room, startAt: Long, title: String)

case class Reservation(user: User, ticket: TicketType, status: BookingStatus, expireAt: Long)

case class Key(projectionId: String, roomName: String, place: Place)

object RoomName extends Enumeration {
  type RoomName = Value
  val A, B, C, D = Value

  def getRoomByName(roomName: RoomName): Room = Room(roomName)
}

object BookingStatus extends Enumeration {
  type BookingStatus = Value
  val EXPIRED, PAID, UNPAID = Value
}

object TicketType extends Enumeration {
  type TicketType = Value
  val ADULT, STUDENT, CHILD = Value

  def getTicketPrice(ticketType: String): Double =
    getTicketType(ticketType) match {
      case ADULT => 25
      case STUDENT => 18
      case CHILD => 12.50
    }

  def getTicketType(ticketType: String): TicketType =
    Try(TicketType
      .withName(ticketType.trim.toUpperCase))
      .getOrElse(TicketType.ADULT)
}
