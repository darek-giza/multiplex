package com.touk.hiring_task.multiplex.service

import cats.data.EitherT
import com.touk.hiring_task.multiplex.commons._
import com.touk.hiring_task.multiplex.dto._
import com.touk.hiring_task.multiplex.model.RoomName.{RoomName, getRoomByName}
import com.touk.hiring_task.multiplex.model._
import com.touk.hiring_task.multiplex.repositories.CinemaRepository
import com.touk.hiring_task.multiplex.utils.{DateUtils, FillPlace, Validator}

import java.util
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

trait CinemaService {
  def getMovies(startAt: Long, finishAt: Long): Future[Either[MultiplexException, List[MovieDto]]]

  def getDetailsById(id: String): Future[Either[MultiplexException, DetailsDto]]

  def processOrder(order: OrderDto): Future[Either[MultiplexException, PaymentDto]]
}

class CinemaServiceImpl(cinemaRepo: CinemaRepository, validator: Validator)(implicit ec: ExecutionContext) extends CinemaService {

  override def getMovies(startAt: Long, finishAt: Long): Future[Either[MultiplexException, List[MovieDto]]] = {

    def fetchProjections(startAt: Long, finishAt: Long): Future[Either[MultiplexException, List[Projection]]] =
      cinemaRepo.fetchProjectionsByDate(startAt, finishAt).map {
        case Some(a) if (a.nonEmpty) => Right(a)
        case _ => Left(ProjectionNotFoundException)
      }

    def takeMoviesDto(movies: List[Projection]): Future[Either[MultiplexException, List[MovieDto]]] =
      Future(Right(
        movies.sortBy(p => (p.title, p.startAt))
          .map(p => MovieDto(p.id, p.title, DateUtils.format(p.startAt)))
      ))

    (for {
      _ <- EitherT.cond[Future](validator.isNotPastDate(startAt), (), InvalidStartProjectionDateException)
      _ <- EitherT.cond[Future](validator.isNotTooDistantDate(finishAt), (), InvalidFinishProjectionDateException)
      _ <- EitherT.cond[Future](validator.isStartAtLessFinishAt(startAt, finishAt), (), InvalidRangeOfDateException)
      movies <- EitherT(fetchProjections(startAt, finishAt))
      moviesDto <- EitherT(takeMoviesDto(movies))
    } yield moviesDto).value
  }

  override def getDetailsById(id: String): Future[Either[MultiplexException, DetailsDto]] = {
    def getPlaces(keys: util.Enumeration[Key], id: String, name: RoomName): Future[Either[MultiplexException, List[Place]]] = {
      val reservedPlaces = getPlace(keys, Nil)(id, name)
      val availablePlace = takeAvailablePlace(reservedPlaces, getRoomByName(name))

      if (availablePlace.nonEmpty) Future(Right(availablePlace)) else Future(Left(NoVacanciesForMovieException))
    }

    (for {
      keys <- EitherT.fromOptionF(cinemaRepo.fetchKeysDB(), InternalServerError)
      proj <- EitherT.fromOptionF(cinemaRepo.takeProjectionById(id), ProjectionNotFoundException)
      places <- EitherT(getPlaces(keys, proj.id, proj.room.name))
    } yield DetailsDto(proj.id, DateUtils.format(proj.startAt), proj.title, proj.room.name.toString, places)).value
  }


  override def processOrder(order: OrderDto): Future[Either[MultiplexException, PaymentDto]] = {
    def takePayment(places: List[PlaceDto]) = places.map(p => TicketType.getTicketPrice(p.ticket)).sum

    (for {
      keys <- EitherT.fromOptionF(cinemaRepo.fetchKeysDB(), InternalServerError)
      pr <- EitherT.fromOptionF(cinemaRepo.takeProjectionById(order.id), ProjectionNotFoundException)
      _ <- EitherT.cond[Future](validator.isListPlaceNonEmpty(order.places), (), InvalidPlaceCountException)
      _ <- EitherT.cond[Future](validator.isNotExpired(pr.startAt), (), InvalidMovieSelectedException)
      _ <- EitherT(Future(validator.isUserNameValid(order.name)))
      _ <- EitherT(checkNoFreeSpaceLeft(keys, pr.id, pr.room.name, order))
      _ <- EitherT(cinemaRepo.storeReservations(order, pr.startAt, pr.room.name.toString))
    } yield PaymentDto(takePayment(order.places), pr.startAt)).value
  }

  @tailrec
  final def getPlace(keys: util.Enumeration[Key], places: List[Place])(implicit id: String, name: RoomName): List[Place] = {
    if (!keys.hasMoreElements)
      places
    else {
      val key = keys.nextElement()
      val isId = key.projectionId == id
      val isRoom = key.roomName == name.toString

      if (isId && isRoom) {
        val newList = places ++ List(key.place)
        getPlace(keys, newList)
      }
      else
        getPlace(keys, places)
    }
  }

  def checkNoFreeSpaceLeft(keys: util.Enumeration[Key], id: String, name: RoomName, order: OrderDto): Future[Either[MultiplexException, Unit]] = {
    val orderedPlace = order.places
      .map(_.place)
      .sortBy(p => (p.row, p.seat))

    val reservedPlaces = getPlace(keys, Nil)(id, name).sortBy(p => (p.row, p.seat))
    val availablePlaces = takeAvailablePlace(reservedPlaces, getRoomByName(name))

    def checkPlaceInRows(orderedPlace: List[Place], availablePlace: List[Place]): Either[MultiplexException, Unit] = {
      if (orderedPlace.isEmpty) {
        Right()
      } else {
        val thisPlace = orderedPlace.head

        val firstAvailableSeatNumber = availablePlace.filter(_.row == thisPlace.row).head.seat

        if (firstAvailableSeatNumber == thisPlace.seat) {
          val newList = availablePlace.filter(p => p != thisPlace).sortBy(p => (p.row, p.seat))
          checkPlaceInRows(orderedPlace.tail, newList)
        } else
          Left(SelectedPlaceNotAvailableException)
      }
    }

    Future(checkPlaceInRows(orderedPlace, availablePlaces))
  }

  def takeAvailablePlace(reservedPlaces: List[Place], room: Room): List[Place] =
    FillPlace.takePlaceByRoom(room)
      .filter(p => !reservedPlaces.contains(p))
      .sortBy(p => (p.row, p.seat))
}
