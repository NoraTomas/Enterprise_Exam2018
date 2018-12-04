package no.ecm.movie.service

import no.ecm.movie.model.converter.NowPlayingConverter
import no.ecm.movie.model.entity.NowPlaying
import no.ecm.movie.repository.NowPlayingRepository
import no.ecm.utils.converter.ConvertionHandler.Companion.convertTimeStampToZonedTimeDate
import no.ecm.utils.dto.movie.NowPlayingDto
import no.ecm.utils.exception.UserInputValidationException
import no.ecm.utils.logger
import no.ecm.utils.messages.ExceptionMessages
import no.ecm.utils.messages.ExceptionMessages.Companion.inputFilterInvalid
import no.ecm.utils.validation.ValidationHandler
import org.springframework.stereotype.Service
import java.util.*

@Service
class NowPlayingService(
        private var nowPlayingRepository: NowPlayingRepository
) {

    val logger = logger<NowPlayingService>()


    fun find(title: String?, date: String?): MutableList<NowPlayingDto> {

        val nowPlaying = if (!title.isNullOrBlank() && date != null){
            logger.warn(inputFilterInvalid())
            throw UserInputValidationException(ExceptionMessages.inputFilterInvalid())
        }else if (!title.isNullOrBlank()){
            nowPlayingRepository.findAllByMovie_TitleContainsIgnoreCase(title!!).toMutableList()
        }else if (date != null){
            val start = convertTimeStampToZonedTimeDate(ValidationHandler.validateTimeFormat("$date 00:00:00.000000"))
            val end = convertTimeStampToZonedTimeDate(ValidationHandler.validateTimeFormat("$date 23:59:59.000000"))
            //val start = convertTimeStampToZonedTimeDate("${ValidationHandler.validateTimeFormat(date)} 00:00:00.000000")
            //val end = convertTimeStampToZonedTimeDate("${ValidationHandler.validateTimeFormat(date)} 23:59:59.000000")

            nowPlayingRepository.findTopByTimeWhenMoviePlayBetweenOrderByTimeWhenMoviePlayDesc(start!!, end!!).toMutableList()
        } else {
            nowPlayingRepository.findAll().toMutableList()
        }

        return NowPlayingConverter.entityListToDtoList(nowPlaying)

    }
}