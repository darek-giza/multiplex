package com.touk.hiring_task.multiplex.utils

import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.{Date, TimeZone}

trait Clock {
  def currentMillis(): Long

  def nowDate(): LocalDate

  def nowDateTime(): LocalDateTime

  def nowMillis(): ZonedDateTime
}

trait SystemClock extends Clock {
  override def nowDate(): LocalDate = LocalDate.now(ZoneId.systemDefault())

  override def nowDateTime() = LocalDateTime.now(ZoneId.systemDefault())

  override def currentMillis(): Long = System.currentTimeMillis()

  override def nowMillis(): ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
}

object DateUtils {
  def format(millis: Long, pattern: String = "dd.MM.yyyy HH:mm"): String = {
    new SimpleDateFormat(pattern).format(new Date(millis)) + " (" + TimeZone.getDefault.toZoneId.toString + ")"
  }
}
