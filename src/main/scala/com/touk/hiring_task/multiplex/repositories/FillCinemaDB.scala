package com.touk.hiring_task.multiplex.repositories

import com.touk.hiring_task.multiplex.model.RoomName.RoomName
import com.touk.hiring_task.multiplex.model._
import com.touk.hiring_task.multiplex.utils.SystemClock

import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.tailrec
import scala.util.Random

trait FillCinemaRoomsService {
  def getListProjections(): List[Projection]

  def getCinemaRooms(list: List[Projection]): ConcurrentHashMap[Key, Reservation]
}

class FillCinemaRoomsServiceImpl extends FillCinemaRoomsService with SystemClock {
  private val expireAt = nowMillis().plusHours(12).toInstant.toEpochMilli

  val reservations = List(
    Reservation(User("John Green"), TicketType.ADULT, BookingStatus.PAID, expireAt),
    Reservation(User("Sam Creig"), TicketType.STUDENT, BookingStatus.PAID, expireAt),
    Reservation(User("Marry Montoya"), TicketType.CHILD, BookingStatus.PAID, expireAt),
    Reservation(User("David Barey"), TicketType.CHILD, BookingStatus.PAID, expireAt)
  )
  val places = List(Place(1, 1), Place(2, 1), Place(3, 1), Place(4, 1), Place(5, 1))
  private val titleRoomA = List("Shrek 1", "Shrek 2", "Shrek 3", "Shrek 4")
  private val titleRoomB = List("Toy Story 1", "Toy Story 2", "Toy Story 3", "Toy Story 4")
  private val titleRoomC = List("Tarzan 1", "Tarzan 2", "Tarzan 3", "Tarzan 4")
  private val titleRoomD = List("The Lion King 1", "The Lion King 2", "The Lion King 3", "The Lion King 4")

  def setProjectionHourForNextDays(hour: Int, plusDays: Int): Long =
    nowDate().atTime(hour, 0, 0, 0).plusDays(plusDays).atZone(ZoneId.systemDefault()).toInstant.toEpochMilli

  def setProjection(id: String, roomName: RoomName, startAt: Int, plusDays: Int, title: String) = {
    val startL = setProjectionHourForNextDays(startAt, plusDays)
    Projection(id, Room(roomName), startL, title)
  }

  @tailrec
  final def setProjectionForNextDays(roomName: RoomName, nextDay: Int, projections: List[Projection], title: List[String]): List[Projection] =
    if (nextDay < 0)
      projections
    else {
      val pr2pm = setProjection(UUID.randomUUID().toString, roomName, 14, nextDay, title(random.nextInt(title.size)))
      val pr4pm = setProjection(UUID.randomUUID().toString, roomName, 16, nextDay, title(random.nextInt(title.size)))
      val pr6pm = setProjection(UUID.randomUUID().toString, roomName, 18, nextDay, title(random.nextInt(title.size)))
      val pr8pm = setProjection(UUID.randomUUID().toString, roomName, 20, nextDay, title(random.nextInt(title.size)))

      val newList = projections ++ List(pr2pm, pr4pm, pr6pm, pr8pm)
      setProjectionForNextDays(roomName, nextDay - 1, newList, title)
    }

  override def getListProjections(): List[Projection] = {
    val projectionsA = setProjectionForNextDays(RoomName.A, 6, Nil, titleRoomA)
    val projectionsB = setProjectionForNextDays(RoomName.B, 6, Nil, titleRoomB)
    val projectionsC = setProjectionForNextDays(RoomName.C, 6, Nil, titleRoomC)
    val projectionsD = setProjectionForNextDays(RoomName.D, 6, Nil, titleRoomD)

    projectionsA ++ projectionsB ++ projectionsC ++ projectionsD
  }

  override def getCinemaRooms(projections: List[Projection]) = {
    val mapProjections: ConcurrentHashMap[Key, Reservation] = new ConcurrentHashMap()

    def getKey(p: Projection) = Key(p.id, p.room.name.toString, places(random.nextInt(places.size)))

    def getReservation() = reservations(random.nextInt(reservations.size))

    projections.foreach(p => {
      mapProjections.put(getKey(p), getReservation())
      mapProjections.put(getKey(p), getReservation())
      mapProjections.put(getKey(p), getReservation())
    })

    mapProjections
  }

  def random = new Random()
}
