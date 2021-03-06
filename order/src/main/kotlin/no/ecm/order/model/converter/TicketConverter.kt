package no.ecm.order.model.converter

import no.ecm.order.model.entity.Ticket
import no.ecm.utils.dto.order.TicketDto

object TicketConverter {
	
	fun entityToDto(entity: Ticket) : TicketDto {
		return TicketDto(
			id = entity.id.toString(),
			price = entity.price,
			seat = entity.seat,
			invoiceId = entity.invoiceId.toString()
		)
	}
	
	fun dtoToEntity(dto: TicketDto) : Ticket {
		val ticket = Ticket(
			price = dto.price,
			invoiceId = dto.invoiceId!!.toLong(),
			seat = dto.seat
		)

		if (!dto.id.isNullOrBlank()){
			ticket.id = dto.id!!.toLong()
		}
		return ticket
	}
	
	fun entityListToDtoList(entities: Iterable<Ticket>): MutableList<TicketDto> {
		return entities.map { entityToDto(it) }.toMutableList()
	}
	
	fun dtoListToEntityList(dto: Iterable<TicketDto>): MutableList<Ticket> {
		return dto.map { dtoToEntity(it) }.toMutableList()
	}
	
}