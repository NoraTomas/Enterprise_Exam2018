package no.ecm.order.model.entity

import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
class Ticket (

        @get:Id
        @get:GeneratedValue
        var id: Long? = null,

        @get:NotNull
        var price: Double? = null,

        @get:NotBlank
        var seat: String? = null,

        @get:NotNull
        var invoiceId: Long? = null
)