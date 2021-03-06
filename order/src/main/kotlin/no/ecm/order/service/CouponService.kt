package no.ecm.order.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.ecm.order.model.converter.CouponConverter
import no.ecm.order.model.entity.Coupon
import no.ecm.order.repository.coupon.CouponRepository
import no.ecm.utils.converter.ConvertionHandler
import no.ecm.utils.dto.order.CouponDto
import no.ecm.utils.exception.NotFoundException
import no.ecm.utils.exception.UserInputValidationException
import no.ecm.utils.logger
import no.ecm.utils.messages.ExceptionMessages
import no.ecm.utils.messages.InfoMessages
import no.ecm.utils.validation.ValidationHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CouponService {
	
	@Autowired
	private lateinit var repository: CouponRepository
	
	val logger = logger<CouponService>()
	
	fun get(paramCode: String?) : MutableList<CouponDto> {
		
		//If paramCode are present, return coupon with given code
		return if (!paramCode.isNullOrBlank()) {
			
			try {
				mutableListOf(CouponConverter.entityToDto(repository.findByCode(paramCode!!)))
			} catch (e: Exception) {
				val errorMsg = ExceptionMessages.notFoundMessage("coupon", "code", paramCode!!)
				logger.warn(errorMsg)
				throw NotFoundException(errorMsg, 404)
			}
		}
		
		//If NOT paramCode or paramId are present, return all coupons in DB
		else {
			CouponConverter.entityListToDtoList(repository.findAll())
		}
	}
	
	fun getById(paramId: String?): Coupon {
		val id = ValidationHandler.validateId(paramId, "id")
		checkIfCouponExistInDb(id)
		
		return repository.findById(id).get()
	}
	
	fun create(dto: CouponDto): String {
		
		if (dto.id != null) {
			val errorMsg = ExceptionMessages.illegalParameter("id")
			logger.warn(errorMsg)
			throw UserInputValidationException(errorMsg, 400)
		}
		
		when {
			dto.code.isNullOrEmpty() -> {
				val errorMsg = ExceptionMessages.missingRequiredField("code")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			dto.description.isNullOrEmpty() -> {
				val errorMsg = ExceptionMessages.missingRequiredField("description")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			dto.expireAt == null -> {
				val errorMsg = ExceptionMessages.missingRequiredField("expireAt")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			dto.percentage == null -> {
				val errorMsg = ExceptionMessages.missingRequiredField("percentage")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			
			// New format for input = yyyy-MM-dd HH:mm:ss
			else -> {
				val formattedTime = "${dto.expireAt!!}.000000"
				val validatedTimeStamp: String = ValidationHandler.validateTimeFormat(formattedTime)
				val parsedDateTime = ConvertionHandler.convertTimeStampToZonedTimeDate(validatedTimeStamp)
				
				val id = repository.createCoupon(dto.code!!, dto.description!!, parsedDateTime!!, dto.percentage!!)
				logger.info(InfoMessages.entityCreatedSuccessfully("coupon", id.toString()))
				
				return id.toString()
			}
		}
	}
	
	fun delete(paramId: String): String {
		
		val id= ValidationHandler.validateId(paramId, "id")
		
		if (!repository.existsById(id)) {
			val errorMsg = ExceptionMessages.notFoundMessage("coupon", "id", paramId)
			logger.warn(errorMsg)
			throw NotFoundException(errorMsg, 404)
		}
		
		try {
			repository.deleteById(id)
		} finally {
			logger.info(InfoMessages.entitySuccessfullyDeleted("coupon", paramId))
		}
		
		return id.toString()
	}
	
	fun put(paramId: String, updatedCouponDto: CouponDto): String {
		
		val id = ValidationHandler.validateId(paramId, "id")
		
		when {
			updatedCouponDto.id.isNullOrEmpty() -> {
				val errorMsg = ExceptionMessages.missingRequiredField("id")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			updatedCouponDto.code.isNullOrEmpty() -> {
				val errorMsg = ExceptionMessages.missingRequiredField("code")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			updatedCouponDto.description.isNullOrEmpty() -> {
				val errorMsg = ExceptionMessages.missingRequiredField("description")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			updatedCouponDto.expireAt.isNullOrEmpty() -> {
				val errorMsg = ExceptionMessages.missingRequiredField("expireAt")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}
			updatedCouponDto.percentage == null -> {
				val errorMsg = ExceptionMessages.missingRequiredField("percentage")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 400)
			}

			!updatedCouponDto.id.equals(id.toString()) -> {
				val errorMsg = ExceptionMessages.notMachingIds("id")
				logger.warn(errorMsg)
				throw UserInputValidationException(errorMsg, 409)
			}
			!repository.existsById(id) -> {
				val errorMsg = ExceptionMessages.notFoundMessage("coupon", "id", paramId)
				logger.warn(errorMsg)
				throw NotFoundException(errorMsg, 404)
			}
			
			else -> {
				val formattedTime = "${updatedCouponDto.expireAt!!}.000000"
				val validatedTimeStamp: String = ValidationHandler.validateTimeFormat(formattedTime)
				val parsedDateTime = ConvertionHandler.convertTimeStampToZonedTimeDate(validatedTimeStamp)
				
				try {
					repository.updateCoupon(id, updatedCouponDto.code!!, updatedCouponDto.description!!, parsedDateTime!!, updatedCouponDto.percentage!!)
				} finally {
					//logger.info(InfoMessages.entitySuccessfullyUpdated("coupon"))
				}
				
				return id.toString()
			}
		}
	}
	
	fun patchDescription(paramId: String, jsonPatch: String): String {
		
		val id = ValidationHandler.validateId(paramId, "id")
		
		//if the given is is not registred in the DB
		if (!repository.existsById(id)) {
			val errorMsg = ExceptionMessages.notFoundMessage("coupon", "id", paramId)
			logger.warn(errorMsg)
			throw NotFoundException(errorMsg, 404)
		}
		
		val jacksonObjectMapper = ObjectMapper()
		
		val jsonNode = try {
			
			jacksonObjectMapper.readValue(jsonPatch, JsonNode::class.java)
			
		} catch (e: Exception) {
			val errorMsg = ExceptionMessages.invalidJsonFormat()
			logger.warn(errorMsg)
			throw UserInputValidationException(errorMsg, 409)
		}
		
		if (jsonNode.has("id")) {
			val errorMsg = ExceptionMessages.illegalParameter("id")
			logger.warn(errorMsg)
			throw UserInputValidationException(errorMsg, 400)
		}
		
		if (!jsonNode.has("description")) {
			val errorMsg = ExceptionMessages.missingRequiredField("description")
			logger.warn(errorMsg)
			throw UserInputValidationException(errorMsg, 400)
		}
		
		val descNodeValue = jsonNode.get("description").asText()
		
		try {
			repository.updateDescription(id, descNodeValue)
		} finally {
			logger.info(InfoMessages.entityFieldUpdatedSuccessfully("coupon", paramId, "description"))
		}
		
		return id.toString()
	}
	
	private fun checkIfCouponExistInDb(id: Long) {
		if (!repository.existsById(id)) {
			val errorMsg = ExceptionMessages.notFoundMessage("Coupon", "id", id.toString())
			logger.warn(errorMsg)
			throw NotFoundException(errorMsg)
		}
	}
	
}