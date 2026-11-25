package com.tolmol.user.infra.security

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tolmol.platform.domain.exception.BusinessException
import com.tolmol.user.service.SocialIdentity
import com.tolmol.user.service.SocialIdentityVerifier
import org.springframework.core.env.Environment

import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class ProviderIdentityVerifier(private val env: Environment, private val json: ObjectMapper) : SocialIdentityVerifier {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()
    private val google by lazy {
        NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build().apply {
            setJwtValidator(JwtValidators.createDefaultWithIssuer("https://accounts.google.com"))
        }
    }

    override fun verify(provider: String, token: String): SocialIdentity = when(provider) {
        "GOOGLE" -> google(token)
        "FACEBOOK" -> facebook(token)
        else -> throw IllegalArgumentException("Unknown sign-in provider")
    }

    private fun setting(name: String): String = env.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: throw BusinessException(503,"Social sign-in needs provider configuration.")

    private fun google(token: String): SocialIdentity {
        val audience = setting("TOLMOL_GOOGLE_CLIENT_ID")
        val claims = try { google.decode(token) }
            catch (_: JwtException) { throw BusinessException(401,"Google sign-in could not be verified.") }
        if (audience !in claims.audience || claims.getClaimAsBoolean("email_verified") != true) {
            throw BusinessException(401,"Google sign-in could not be verified.")
        }
        return SocialIdentity("GOOGLE",claims.subject.orEmpty(),claims.getClaimAsString("email").orEmpty().trim().lowercase(),
            claims.getClaimAsString("name").orEmpty().take(100).ifBlank { "TolMol user" })
    }

    private fun facebook(token: String): SocialIdentity {
        val id = setting("TOLMOL_FACEBOOK_APP_ID")
        val secret = setting("TOLMOL_FACEBOOK_APP_SECRET")
        val debug = graph("debug_token",mapOf("input_token" to token),"$id|$secret").path("data")
        val now = Instant.now().epochSecond
        if (!debug.path("is_valid").asBoolean() || debug.path("app_id").asText() != id ||
            debug.path("type").asText() != "USER" || debug.path("expires_at").asLong() <= now ||
            debug.path("data_access_expires_at").asLong(Long.MAX_VALUE) <= now) {
            throw BusinessException(401,"Facebook sign-in could not be verified.")
        }
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(),"HmacSHA256")) }
        val proof = mac.doFinal(token.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        val profile = graph("me",mapOf("fields" to "id,name,email","appsecret_proof" to proof),token)
        if (profile.path("id").asText() != debug.path("user_id").asText() || profile.path("email").asText().isBlank()) {
            throw BusinessException(401,"Facebook must share an email to continue.")
        }
        return SocialIdentity("FACEBOOK",profile.path("id").asText(),profile.path("email").asText().lowercase(),
            profile.path("name").asText("TolMol user").take(100))
    }

    private fun graph(path: String, query: Map<String,String>, token: String): JsonNode {
        val version = env.getProperty("TOLMOL_FACEBOOK_GRAPH_VERSION","v24.0")
        require(version.matches(Regex("v[0-9]+\\.[0-9]+")))
        val params = query.entries.joinToString("&") { (key,value) -> "$key=" + URLEncoder.encode(value,Charsets.UTF_8) }
        val request = HttpRequest.newBuilder(URI("https://graph.facebook.com/$version/$path?$params"))
            .header("Authorization","Bearer $token").timeout(Duration.ofSeconds(10)).GET().build()
        val response = try { client.send(request,HttpResponse.BodyHandlers.ofString()) }
            catch (_: java.io.IOException) { throw BusinessException(503,"Provider unavailable. Retry sign-in.") }
            catch (_: InterruptedException) { Thread.currentThread().interrupt(); throw BusinessException(503,"Provider unavailable. Retry sign-in.") }
        if (response.statusCode() != 200) throw BusinessException(401,"Facebook sign-in could not be verified.")
        return json.readTree(response.body())
    }
}
