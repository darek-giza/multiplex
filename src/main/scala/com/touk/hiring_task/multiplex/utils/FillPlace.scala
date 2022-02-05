package com.touk.hiring_task.multiplex.utils

import com.touk.hiring_task.multiplex.model.{Place, Room}

object FillPlace {
  def takePlaceByRoom(room: Room): List[Place] = {
    def fillRoom(row: Int, seat: Int, places: List[Place]): List[Place] = {
      if (row < 1)
        places
      else {
        def fillRow(seat: Int, placeInRow: List[Place]): List[Place] = {
          if (seat < 1) placeInRow
          else {
            val newList = placeInRow ++ List(Place(row, seat))
            fillRow(seat - 1, newList)
          }
        }

        val newPlaces = places ++ fillRow(seat, Nil)
        fillRoom(row - 1, seat, newPlaces)
      }
    }

    fillRoom(room.row, room.placeInRow, Nil)
  }
}
