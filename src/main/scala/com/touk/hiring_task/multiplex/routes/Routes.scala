package com.touk.hiring_task.multiplex.routes

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server._
import com.touk.hiring_task.multiplex.commons._
import com.touk.hiring_task.multiplex.routes.ErrorCode.ErrorCode
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto.exportEncoder
import io.circe.{Decoder, Encoder}

import scala.concurrent.Future

trait Routes extends FailFastCirceSupport {

  protected def completeEitherOrError[A](fe: Future[Either[MultiplexException, A]], returnBody: Boolean = true)(implicit marshaller: ToEntityMarshaller[A]): Route =
    Directives.onSuccess(fe) {
      case Right(a) => if (returnBody) complete(StatusCodes.OK, a) else complete(StatusCodes.OK)
      case Left(t) => completeHttpError(t)
    }

  protected def completeError(httpError: HttpErrorDto) =
    complete(httpError.status, httpError.error)

  protected def completeHttpError(ex: MultiplexException): StandardRoute =
    ex match {
      case InternalServerError => completeError(InternalServerErrorDto())
      case ProjectionNotFoundException => completeError(NotFoundErrorDto("Projection not found", ErrorCode.PROJECTION_NOT_FOUND))
      case InvalidStartProjectionDateException => completeError(BadRequestErrorDto("Invalid date, should not be past", ErrorCode.INVALID_START_DATE))
      case InvalidFinishProjectionDateException => completeError(BadRequestErrorDto("Invalid date, screenings are only available for the next week, should be less then now plus 7 days", ErrorCode.INVALID_FINISH_DATE))
      case InvalidRangeOfDateException => completeError(BadRequestErrorDto("Invalid date, start date should be less than finish date", ErrorCode.INVALID_DATE_RANGE))
      case NoVacanciesForMovieException => completeError(NotFoundErrorDto("No vacancies for this movie", ErrorCode.NO_VACANCIES_FOR_MOVIE))
      case SelectedPlaceUnavailableException => completeError(BadRequestErrorDto("Selected place unavailable", ErrorCode.UNAVAILABLE_PLACE))
      case InvalidRoomNameException => completeError(BadRequestErrorDto("Invalid room name, room with this name not exists", ErrorCode.INVALID_ROOM_NAME))
      case InvalidMovieSelectedException => completeError(BadRequestErrorDto("Invalid movie selected, movie start in less than 15 minutes", ErrorCode.INVALID_MOVIE_SELECTED))
      case InvalidPlaceCountException => completeError(BadRequestErrorDto("Invalid place count, list of place must not be empty", ErrorCode.INVALID_PLACE_COUNT))
      case InvalidFirstNameLengthException => completeError(BadRequestErrorDto("Invalid first name length, should contains more than three char", ErrorCode.INVALID_USERNAME_LENGTH))
      case InvalidFirstNameFormatException => completeError(BadRequestErrorDto("Invalid first name format, first character should be uppercase and rest lowercase", ErrorCode.INVALID_USERNAME_FORMAT))
      case InvalidFamilyNameLengthException => completeError(BadRequestErrorDto("Invalid family name length, should contains more than three char", ErrorCode.INVALID_USERNAME_LENGTH))
      case InvalidFamilyNameFormatException => completeError(BadRequestErrorDto("Invalid family name format, first character should be uppercase and rest lowercase", ErrorCode.INVALID_USERNAME_FORMAT))
      case InvalidSecondFamilyNameLengthException => completeError(BadRequestErrorDto("Invalid second family name length, should contains more than three char", ErrorCode.INVALID_USERNAME_LENGTH))
      case InvalidSecondFamilyNameFormatException => completeError(BadRequestErrorDto("Invalid second family name format, first character should be uppercase and rest lowercase", ErrorCode.INVALID_USERNAME_FORMAT))
      case SelectedPlaceNotAvailableException => completeError(BadRequestErrorDto("Selected place not available, should be no free space left", ErrorCode.INVALID_SELECTED_PLACE))
    }
}

case class ErrorWithCodeDto(message: String, errorCode: ErrorCode)

sealed trait HttpErrorDto {
  val error: ErrorWithCodeDto
  val status: StatusCode
  val message: String
}

object ErrorCode extends Enumeration {
  type ErrorCode = Value
  val PROJECTION_NOT_FOUND, NO_VACANCIES_FOR_MOVIE, INVALID_MOVIE_SELECTED,
  UNAVAILABLE_PLACE, INVALID_PLACE_COUNT, INVALID_SELECTED_PLACE,
  INVALID_ROOM_NAME,
  INVALID_START_DATE, INVALID_FINISH_DATE, INVALID_DATE_RANGE,
  INVALID_USERNAME_LENGTH, INVALID_USERNAME_FORMAT,
  INTERNAL_ERROR
  = Value

  implicit val errorCodeEncoder: Encoder[ErrorCode] =
    Encoder.encodeEnumeration(ErrorCode)

  implicit val errorCodeDecoder: Decoder[ErrorCode] =
    Decoder.decodeEnumeration(ErrorCode)
}

case class NotFoundErrorDto(msg: String, errorCode: ErrorCode) extends HttpErrorDto {
  override val message = msg
  override val status = StatusCodes.NotFound
  override val error = ErrorWithCodeDto(msg, errorCode)
}

case class BadRequestErrorDto(msg: String, errorCode: ErrorCode) extends HttpErrorDto {
  override val message = msg
  override val status = StatusCodes.BadRequest
  override val error = ErrorWithCodeDto(msg, errorCode)
}

case class InternalServerErrorDto() extends HttpErrorDto {
  override val message = "Internal server error"
  override val status = StatusCodes.InternalServerError
  override val error = ErrorWithCodeDto(message, ErrorCode.INTERNAL_ERROR)
}
