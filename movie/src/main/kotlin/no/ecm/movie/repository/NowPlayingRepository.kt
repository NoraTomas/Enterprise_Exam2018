package no.ecm.movie.repository

import no.ecm.movie.model.entity.NowPlaying
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface NowPlayingRepository : CrudRepository<NowPlaying, Long> {

    fun findAllByMovie_TitleContainsIgnoreCase(title: String): Iterable<NowPlaying>

    fun findTopByTimeWhenMoviePlayBetweenOrderByTimeWhenMoviePlayDesc(start: ZonedDateTime, end: ZonedDateTime): Iterable<NowPlaying>
}