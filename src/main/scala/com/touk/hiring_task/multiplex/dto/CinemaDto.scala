package com.touk.hiring_task.multiplex.dto

import com.touk.hiring_task.multiplex.model.Place
import scala.language.implicitConversions

case class PaymentDto(amount: Double, expireAt: Long)

case class PlaceDto(place: Place, ticket: String)

case class DetailsDto(id: String, startAt: String, title: String, roomName: String, places: List[Place])

case class MovieDto(id: String, title: String, startAt: String)

case class OrderDto(id: String, places: List[PlaceDto], name: String)