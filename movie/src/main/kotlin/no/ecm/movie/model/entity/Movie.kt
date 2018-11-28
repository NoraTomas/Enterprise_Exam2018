package no.ecm.movie.model.entity

import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
class Movie (

        @get:Id
        @get:GeneratedValue
        var id: Long? = null,

        @get:NotBlank
        var title: String? = null,

        @get:NotBlank
        var posterUrl: String? = null,

        @get:ManyToMany(mappedBy = "movies", fetch = FetchType.EAGER) // cascade??
        var genre: MutableSet<Genre> = mutableSetOf(),

        @get:NotNull
        var movieDuration: Int? = null,

        @get:NotNull
        var ageLimit: Int? = null,

        @get:OneToOne(fetch = FetchType.LAZY)
        @get:JoinColumn(name = "nowplaying_id")
        var nowPlaying: NowPlaying? = null
)