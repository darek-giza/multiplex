package com.touk.hiring_task.multiplex.repositories

import com.touk.hiring_task.multiplex.commons.{MultiplexException, SelectedPlaceUnavailableException}
import com.touk.hiring_task.multiplex.dto.OrderDto
import com.touk.hiring_task.multiplex.model._
import com.touk.hiring_task.multiplex.utils.SystemClock

import java.util
import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Future}

trait CinemaRepository {
  def fetchProjectionsByDate(startAt: Long, finishAt: Long): Future[Option[List[Projection]]]

  def takeProjectionById(id: String): Future[Option[Projection]]

  def fetchKeysDB(): Future[Option[Enumeration[Key]]]

  def storeReservations(o: OrderDto, expire: Long, roomName: String): Future[Either[MultiplexException, Unit]]
}

class CinemaRepositoryImpl(fillDB: FillCinemaRoomsService)(implicit ec: ExecutionContext) extends CinemaRepository with SystemClock {

  val projectionsList = fillDB.getListProjections()
  val cinemaDB = fillDB.getCinemaRooms(projectionsList)

  override def fetchProjectionsByDate(projectionStartAt: Long, projectionFinishAt: Long): Future[Option[List[Projection]]] =
    Future.successful(Option(projectionsList.filter(p => p.startAt > projectionStartAt && p.startAt < projectionFinishAt)))

  override def takeProjectionById(projectionId: String): Future[Option[Projection]] =
    Future.successful(projectionsList.find(_.id == projectionId))

  override def fetchKeysDB(): Future[Option[Enumeration[Key]]] =
    Future.successful(Option(cinemaDB.keys()))

  override def storeReservations(o: OrderDto, expire: Long, roomName: String): Future[Either[MultiplexException, Unit]] = {
    val projections: ConcurrentHashMap[Key, Reservation] = new ConcurrentHashMap()
    o.places.map(p => projections.put(
      Key(o.id, roomName, Place(p.place.row, p.place.seat)),
      Reservation(User(o.name), TicketType.getTicketType(p.ticket), BookingStatus.UNPAID, expire)))

    val keys = projections.keys()

    def existsAnyKey(keys: util.Enumeration[Key], exists: Boolean): Boolean = {
      if (!keys.hasMoreElements || exists)
        exists
      else {
        val key = keys.nextElement()
        val exists = cinemaDB.containsKey(key)

        existsAnyKey(keys, exists)
      }
    }

    val notExistAnyKey = !existsAnyKey(keys, false)

    if (notExistAnyKey) Future.successful(Right(cinemaDB.putAll(projections))) else Future.successful(Left(SelectedPlaceUnavailableException))
  }
}
