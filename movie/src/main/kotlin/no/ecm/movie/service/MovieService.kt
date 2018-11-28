package no.ecm.movie.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.ecm.movie.model.converter.MovieConverter
import no.ecm.movie.repository.MovieRepository
import no.ecm.utils.dto.movie.GenreDto
import no.ecm.utils.dto.movie.MovieDto
import no.ecm.utils.exception.ConflictException
import no.ecm.utils.exception.ExceptionMessages
import no.ecm.utils.exception.NotFoundException
import no.ecm.utils.exception.UserInputValidationException
import no.ecm.utils.logger
import no.ecm.utils.validation.ValidationHandler
import no.ecm.utils.validation.ValidationHandler.Companion.validateId
import org.springframework.stereotype.Service

@Service
class MovieService (
        private var movieRepository: MovieRepository,
        private var genreService: GenreService){

    val logger = logger<MovieService>()

    fun getMovies(title: String?, offset: Int, limit: Int): MutableList<MovieDto> {

        ValidationHandler.validateLimitAndOffset(offset, limit)

        val movies = if (!title.isNullOrEmpty()){
            try {
                movieRepository.findByTitleContainsIgnoreCase(title!!).toMutableList()
            } catch (e: Exception){
                val errorMsg = ExceptionMessages.notFoundMessage("Movie", "title", title!!)
                logger.warn(errorMsg)
                throw NotFoundException(errorMsg)
            }
        } else {
            movieRepository.findAll().toMutableList()
        }

        return MovieConverter.entityListToDtoList(movies)
    }

    fun getMovie(stringId: String?): MovieDto {

        val id = validateId(stringId)

        if (!movieRepository.existsById(id)){
            val errorMsg = ExceptionMessages.notFoundMessage("Movie", "id", stringId!!)
            logger.warn(errorMsg)
            throw NotFoundException(errorMsg)
        }

        val movie = movieRepository.findById(id).get()

        return MovieConverter.entityToDto(movie)
    }

    private fun handleMissingField(fieldName: String){
        val errorMsg = ExceptionMessages.missingRequiredField(fieldName)
        logger.warn(errorMsg)
        throw UserInputValidationException(errorMsg)
    }

    fun createMovie(movieDto: MovieDto): String {

        if (movieDto.title.isNullOrEmpty()) {
            handleMissingField("title")
        }  else if (movieDto.movieDuration == null){
            handleMissingField("movieDuration")
        } else if (movieDto.posterUrl.isNullOrEmpty()){
            handleMissingField("posterUrl")
        } else if (!movieDto.id.isNullOrEmpty()){
            val errorMsg = ExceptionMessages.illegalParameter("id")
            logger.warn(errorMsg)
            throw UserInputValidationException(errorMsg)
        }

        if (movieRepository.existsByTitleAndPosterUrlIgnoreCase(movieDto.title!!, movieDto.posterUrl!!)){
            val errorMsg = (ExceptionMessages
                    .resourceAlreadyExists("Movie", "title and posterUrl", "${movieDto.title} and ${movieDto.posterUrl}"))
            logger.error(errorMsg)
            throw ConflictException(errorMsg)
        }

        movieDto.title = movieDto.title!!.capitalize()

        val movie = MovieConverter.dtoToEntity(movieDto)

        if (movieDto.genre != null){
            movieDto.genre!!.forEach { genreService.getGenre(it.id).movies.add(movie) }
        }

        return movieRepository.save(movie).id.toString()
    }

    fun updateMovie(stringId: String?, body: String?): MovieDto {

        val id = validateId(stringId)

        if (!movieRepository.existsById(id)){
            val errorMsg = ExceptionMessages.notFoundMessage("Movie", "id", stringId!!)
            logger.warn(errorMsg)
            throw NotFoundException(errorMsg)
        }

        val jackson = ObjectMapper()

        val jsonNode: JsonNode

        try {
            jsonNode = jackson.readValue(body, JsonNode::class.java)
        } catch (e: Exception) {
            val errorMsg = ExceptionMessages.invalidParameter("JSON", "invalid JSON object")
            logger.error(errorMsg)
            throw UserInputValidationException(errorMsg)
        }

        val movie = movieRepository.findById(id).get()

        if (jsonNode.has("title")) {
            val title = jsonNode.get("title")
            if (title.isTextual){
                movie.title = title.asText()
            } else {
                val errorMsg = ExceptionMessages.unableToParse("title")
                logger.warn(errorMsg)
                throw UserInputValidationException(errorMsg)
            }
        }

        if (jsonNode.has("posterUrl")) {
            val posterUrl = jsonNode.get("posterUrl")
            if (posterUrl.isTextual){
                movie.posterUrl = posterUrl.asText()
            } else {
                val errorMsg = ExceptionMessages.unableToParse("posterUrl")
                logger.warn(errorMsg)
                throw UserInputValidationException(errorMsg)
            }
        }



        if (jsonNode.has("genre")) {
            val genre = jsonNode.get("genre")
            when {
                genre.isNull -> movie.genre = mutableSetOf()
                genre.isArray -> {
                    val mapper = jacksonObjectMapper()
                    val genreDtos: Set<GenreDto> = mapper.readValue(genre.toString())
                    //movie.genre = genreDtos.asSequence().map { genreService.getGenre(it.id) }.toMutableSet()
                    //TODO need to replase not add
                    genreDtos.forEach { genreService.getGenre(it.id).movies.add(movie) }

                }
                else -> throw UserInputValidationException("Unable to handle field: 'movies'")
            }
        }
        movieRepository.save(movie)

        return MovieConverter.entityToDto(movie)
    }

    fun deleteMovie(stringId: String?): String? {

        val id = validateId(stringId)

        if (!movieRepository.existsById(id)){
            val errorMsg = ExceptionMessages.notFoundMessage("Movie", "id", stringId!!)
            logger.warn(errorMsg)
            throw NotFoundException(errorMsg)
        }

        val movie = movieRepository.findById(id).get()
        movie.genre.forEach { it.movies.remove(movie) }

        movieRepository.deleteById(id)

        return id.toString()
    }
}