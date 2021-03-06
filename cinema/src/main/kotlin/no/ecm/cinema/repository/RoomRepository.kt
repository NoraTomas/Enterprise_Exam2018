package no.ecm.cinema.repository

import no.ecm.cinema.model.entity.Room
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RoomRepository : CrudRepository<Room, Long> {

    fun findAllByCinemaId(cinemaId: Long): Iterable<Room>

    fun findByIdAndCinemaId(id: Long, cinema_Id: Long): Room

    fun existsByCinema_IdAndName(cinema_Id: Long, name: String): Boolean
}