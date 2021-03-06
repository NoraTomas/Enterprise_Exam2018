package no.ecm.creditcard


import io.restassured.RestAssured
import io.restassured.RestAssured.basic
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import no.ecm.creditcard.repository.CreditCardRepository
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class TestBase {
	
	@LocalServerPort
	protected var port = 0
	
	@field:Autowired
	private lateinit var creditCardRepository: CreditCardRepository
	
	@Before
	fun clean() {
		// RestAssured configs shared by all the tests
		RestAssured.baseURI = "http://localhost"
		RestAssured.port = port
		RestAssured.basePath = "/graphql"
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
		RestAssured.authentication = basic("admin", "admin")
		
		creditCardRepository.deleteAll()
	}
	
	fun createCreditcard(username: String, creditcardNumber: String, expDate: String, cvc: Int) : String? {
		val createQuery = """
                    { "query" :
                         "mutation{createCreditCard(creditCard:{expirationDate:\"$expDate\",cvc: $cvc,username:\"$username\",cardNumber:\"$creditcardNumber\"})}"
                    }
                    """.trimIndent()
		
		return given()
				//.auth().basic("admin", "admin")
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(createQuery)
				.post()
			.then()
			.statusCode(200)
			.extract().body().path<String>("data.createCreditCard")
	}
	
	fun invalidUserQuery(query: String): ValidatableResponse? {
		return given()
			.accept(ContentType.JSON)
			.contentType(ContentType.JSON)
			.body(query)
			.post()
			.then()
			.statusCode(200)
	}
	
	fun getCreditcardById(username: String): ValidatableResponse? {
		
		val getQuery = """
			{
  				creditcardById(id: "$username") {
    				id, username, cardNumber, cvc, expirationDate
  				}
			}
		""".trimIndent()
		
		return given()
			.accept(ContentType.JSON)
			.contentType(ContentType.JSON)
			.queryParam("query", getQuery)
			.get()
			.then()
	}
}