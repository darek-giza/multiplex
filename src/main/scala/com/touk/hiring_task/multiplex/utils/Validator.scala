package com.touk.hiring_task.multiplex.utils

import com.touk.hiring_task.multiplex.commons._
import com.touk.hiring_task.multiplex.dto.PlaceDto

trait Validator {
  def isNotPastDate(date: Long): Boolean

  def isNotTooDistantDate(date: Long): Boolean

  def isStartAtLessFinishAt(start: Long, finish: Long): Boolean

  def isNotExpired(startAt: Long): Boolean

  def isListPlaceNonEmpty(order: List[PlaceDto]): Boolean

  def isUserNameValid(name: String): Either[MultiplexException, String]
}

class ValidatorImpl extends Validator with SystemClock {

  def isNotPastDate(date: Long) = date >= currentMillis()

  def isNotTooDistantDate(date: Long) = date <= nowPlusWeek()

  def nowPlusWeek() = nowMillis().plusDays(7).toInstant.toEpochMilli

  def isStartAtLessFinishAt(start: Long, finish: Long) = start < finish

  def isNotExpired(startAt: Long) = expirationTime < startAt

  def expirationTime = nowMillis().plusMinutes(15).toInstant.toEpochMilli

  def isListPlaceNonEmpty(order: List[PlaceDto]) = order.nonEmpty

  def isUserNameValid(name: String): Either[MultiplexException, String] = {
    val firstName = name.split(" ").head
    val sureName = name.split(" ").last
    val hasSecondPart = sureName.contains("-")

    val tuplesSureName = if (hasSecondPart) (sureName.split("-").head, sureName.split("-").last) else (sureName, "")

    for {
      _ <- isLengthValid(firstName, InvalidFirstNameLengthException)
      _ <- isLengthValid(tuplesSureName._1, InvalidFamilyNameLengthException)
      _ <- if (hasSecondPart) isLengthValid(tuplesSureName._2, InvalidSecondFamilyNameLengthException) else Right()
      _ <- isFirstCharUpperAndRestLower(firstName, InvalidFirstNameFormatException)
      _ <- isFirstCharUpperAndRestLower(tuplesSureName._1, InvalidFamilyNameFormatException)
      _ <- if (hasSecondPart) isFirstCharUpperAndRestLower(tuplesSureName._2, InvalidSecondFamilyNameFormatException) else Right()
    } yield name
  }

  private def isLengthValid(s: String, ex: MultiplexException): Either[MultiplexException, Unit] =
    if (s.length > 3) Right(()) else Left(ex)

  private def isFirstCharUpperAndRestLower(s: String, ex: MultiplexException): Either[MultiplexException, Unit] = {
    val isFirstCharUpper = s.head.isUpper
    val isAnyUpperInRestOfChar = s.substring(1).toCharArray.exists(a => a.isUpper)
    if (isFirstCharUpper && !isAnyUpperInRestOfChar) Right() else Left(ex)
  }
}
