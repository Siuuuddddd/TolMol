package com.tolmol

import com.fasterxml.jackson.databind.JsonNode
import com.tolmol.user.domain.model.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import java.time.Instant
import java.util.UUID

class MarketplaceJourneyTest : SocketFixture() {
    @Autowired lateinit var http: TestRestTemplate

    private fun request(method: HttpMethod,path: String,token: String?=null,body: Any?=null): JsonNode? {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            if (token!=null) setBearerAuth(token)
        }
        val result = http.exchange("/api$path",method,HttpEntity(body,headers),String::class.java)
        assertTrue(result.statusCode.is2xxSuccessful,"$method $path returned ${result.statusCode}: ${result.body}")
        return result.body?.takeIf { it.isNotBlank() }?.let(json::readTree)
    }
    private fun signIn(role: Role): String {
        val credentials = mapOf("email" to "${UUID.randomUUID()}@example.org","password" to "test-credentials")
        request(HttpMethod.POST,"/auth/register",body=credentials + ("roles" to listOf(role.name)))
        val token = request(HttpMethod.POST,"/auth/login",body=credentials)!!.get("accessToken").asText()
        request(HttpMethod.PUT,"/me/location/$role",token,mapOf("latitude" to 18.9,"longitude" to 72.8))
        return token
    }
    @Test fun authenticatedMarketplaceThroughPickup() {
        val seller = signIn(Role.SELLER)
        val buyer = signIn(Role.BUYER)
        val listing = request(HttpMethod.POST,"/listings",seller,mapOf("category" to "paper","weight" to 20,
            "startingRate" to 12,"deadline" to Instant.now().plusSeconds(3600).toString()))!!.get("id").asText()
        assertTrue(request(HttpMethod.GET,"/listings",buyer)!!.any { it["id"].asText()==listing })
        val bid = request(HttpMethod.POST,"/listings/$listing/bids",buyer,mapOf("rate" to 14,"requestKey" to UUID.randomUUID()))!!
        request(HttpMethod.POST,"/listings/$listing/accept",seller,mapOf("bidId" to bid["id"].asText()))
        val thread = request(HttpMethod.POST,"/listings/$listing/chat",buyer)!!
        assertEquals(listing,thread["listing"].asText())
        val inspected = request(HttpMethod.POST,"/deals/$listing/inspect",buyer,mapOf("weight" to 20))!!
        val closed = request(HttpMethod.POST,"/deals/$listing/confirm",seller,mapOf("revision" to inspected["revision"].asLong()))!!
        assertEquals("COMPLETED",closed["state"].asText())
        assertEquals(280,closed["amount"].asInt())
    }
}
