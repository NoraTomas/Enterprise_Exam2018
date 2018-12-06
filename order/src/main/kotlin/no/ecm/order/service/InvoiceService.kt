package no.ecm.order.service

import no.ecm.order.model.converter.InvoiceConverter
import no.ecm.order.model.entity.Invoice
import no.ecm.order.repository.InvoiceRepository
import no.ecm.utils.dto.order.InvoiceDto
import no.ecm.utils.exception.NotFoundException
import no.ecm.utils.exception.UserInputValidationException
import no.ecm.utils.logger
import no.ecm.utils.messages.ExceptionMessages
import no.ecm.utils.messages.ExceptionMessages.Companion.invalidFieldCombination
import no.ecm.utils.messages.InfoMessages
import no.ecm.utils.validation.ValidationHandler
import no.ecm.utils.validation.ValidationHandler.Companion.validateId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class InvoiceService(
        private var invoiceRepository: InvoiceRepository,
        private var ticketService: TicketService,
        private var couponService: CouponService
) {

    @Value("\${movieService}")
    private lateinit var moviePath : String
    
    @Value("\${ticketPrice}")
    private lateinit var ticketPrice: String

    val logger = logger<InvoiceService>()

    fun findInvoice(username: String?, nowPlayingId: String?, isPaid: Boolean?): MutableList<InvoiceDto> {

        val invoices = when {
            !username.isNullOrBlank() && !nowPlayingId.isNullOrBlank() && isPaid != null -> {
                val errorMsg = invalidFieldCombination("username, nowPlayingId and paid")
                logger.warn(errorMsg)
                throw UserInputValidationException(errorMsg)}

            !username.isNullOrBlank() && !nowPlayingId.isNullOrBlank() -> invoiceRepository
                    .findAllByUsernameIgnoreCaseAndNowPlayingId(
                            username!!,
                            validateId(nowPlayingId, "nowPLayingId"))

            !username.isNullOrBlank() && isPaid != null -> {
                invoiceRepository.findAllByUsernameIgnoreCaseAndPaid(username!!, isPaid)}

            !username.isNullOrBlank() -> invoiceRepository.findAllByUsernameIgnoreCase(username!!).toMutableList()

            isPaid != null && !nowPlayingId.isNullOrBlank() -> invoiceRepository
                    .findAllByPaidAndNowPlayingId(
                            isPaid,
                            validateId(nowPlayingId, "nowPLayingId"))

            isPaid != null -> invoiceRepository.findAllByPaid(isPaid)

            !nowPlayingId.isNullOrBlank() -> {
                invoiceRepository.findAllByNowPlayingId(validateId(nowPlayingId, "nowPLayingId"))}

            else -> invoiceRepository.findAll().toMutableList()
        }
        return InvoiceConverter.entityListToDtoList(invoices)
    }

    fun findById(stringId: String?): Invoice {
        val id = checkFOrInvoiceInDatabase(stringId)

        return invoiceRepository.findById(id).get()
    }

    fun deleteById(stringId: String?): String? {

        val id = checkFOrInvoiceInDatabase(stringId)

        invoiceRepository.deleteById(id)
        val infoMsg = InfoMessages.entitySuccessfullyDeleted("invoice", "$id")
        logger.info(infoMsg)

        return id.toString()

    }
    
    fun createInvoice(dto: InvoiceDto): InvoiceDto {
        
        if (dto.id != null) {
            handleIllegalField("id")
        }
        
        validateInvoiceDto(dto)
    
        ticketService.checkIfTicketsExistsInDatabase(dto.tickets!!)
        
        dto.tickets!!.forEach {
            ticketService.create(it)
        }
    
        //TODO check Coupon
        if (dto.couponCode != null && !dto.couponCode!!.id.isNullOrBlank()){
            val couponDto = couponService.get(null, dto.couponCode!!.id).first()
            val discount = couponDto.percentage!! / 100 * (ticketPrice.toDouble() * dto.tickets!!.size)
            
            dto.totalPrice = (ticketPrice.toDouble() * dto.tickets!!.size) - discount
        } else {
            dto.totalPrice = ticketPrice.toDouble() * dto.tickets!!.size
        }
        
        
        
        
        //TODO check NowPlaying
        
        
        
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun checkFOrInvoiceInDatabase(stringId: String?): Long {
        val id = ValidationHandler.validateId(stringId, "id")

        when {
            !invoiceRepository.existsById(id) -> {
                val errorMsg = ExceptionMessages.notFoundMessage("Invoice", "id", id.toString())
                logger.warn(errorMsg)
                throw NotFoundException(errorMsg)
            }
            else -> return id
        }

    }
    
    private fun validateInvoiceDto(dto: InvoiceDto) {
        when {
            dto.username.isNullOrBlank() -> handleMissingField("cusername")
            dto.orderDate.isNullOrBlank() -> handleMissingField("orderDate")
            dto.nowPlayingId.isNullOrBlank() -> handleMissingField("nowPlayingId")
            dto.tickets == null -> handleMissingField("tickets")
            dto.totalPrice != null -> handleIllegalField("totlPrice")
        }
    }

    private fun handleUnableToParse(fieldName: String){
        val errorMsg = ExceptionMessages.unableToParse(fieldName)
        logger.warn(errorMsg)
        throw UserInputValidationException(errorMsg)
    }

    private fun handleIllegalField(fieldName: String) {
        val errorMsg = ExceptionMessages.illegalParameter(fieldName)
        logger.warn(errorMsg)
        throw UserInputValidationException(errorMsg)
    }

    private fun handleMissingField(fieldName: String){
        val errorMsg = ExceptionMessages.missingRequiredField(fieldName)
        logger.warn(errorMsg)
        throw UserInputValidationException(errorMsg)
    }
	
}