package no.ecm.order.repository.coupon

import no.ecm.order.model.entity.Coupon
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import javax.persistence.EntityManager

@Repository
@Transactional
class CouponRepositoryCustomImpl : CouponRepositoryCustom {
	
	@Autowired
	private lateinit var em: EntityManager
	
	override fun createCoupon(code: String, description: String, expireAt: ZonedDateTime, percentage: Int): Long {
		
		val entity = Coupon(null, code, description, expireAt, percentage)
		
		em.persist(entity)
		return entity.id!!
	}
	
	override fun updateCoupon(id: Long, code: String, description: String, expireAt: ZonedDateTime, percentage: Int): Boolean {
		
		val entity = em.find(Coupon::class.java, id) ?: return false
		
		entity.code = code
		entity.description = description
		entity.expireAt = expireAt
		entity.percentage = percentage
		
		return true
	}
	
	override fun updateDescription(id: Long, description: String): Boolean {
		
		val entity = em.find(Coupon::class.java, id) ?: return false
		
		entity.description = description
		
		return true
		
	}
}