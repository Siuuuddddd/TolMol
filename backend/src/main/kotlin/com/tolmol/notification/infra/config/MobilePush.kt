package com.tolmol.notification.infra.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jwt.*
import com.tolmol.notification.domain.model.*
import com.tolmol.notification.service.*
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.*
import java.security.KeyFactory
import java.security.interfaces.*
import java.security.spec.PKCS8EncodedKeySpec
import java.time.*
import java.util.*

interface DevicePushAdapter {
    val platform: Platform
    fun send(device: Device,alert: Alert)
}

@Component
@Primary
class MobilePush(private val devices: DeviceStore,adapters: List<DevicePushAdapter>,
    private val logging: LoggingPushProvider) : PushProvider {
    private val byPlatform = adapters.associateBy { it.platform }
    override fun send(user: UUID,alert: Alert) {
        val registered = devices.devices(user)
        if (registered.isEmpty()) logging.send(user,alert)
        for (device in registered) {
            val adapter = byPlatform[device.platform]
            if (adapter==null) logging.send(user,alert) else adapter.send(device,alert)
        }
    }
}

private fun signingKey(env: Environment,name: String,algorithm: String): java.security.PrivateKey {
    val pem = env.getRequiredProperty(name).replace("\\n","\n").replace("-----BEGIN PRIVATE KEY-----","")
        .replace("-----END PRIVATE KEY-----","").filterNot(Char::isWhitespace)
    return KeyFactory.getInstance(algorithm).generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)))
}

private val http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(8)).build()
private fun post(uri: String,body: String,headers: Map<String,String>): String {
    val req = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(15))
    headers.forEach { (k,v) -> req.header(k,v) }
    val response = http.send(req.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() in 200..299) { "Push provider returned HTTP ${response.statusCode()}" }
    return response.body()
}

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name=["tolmol.push.fcm.enabled"],havingValue="true")
class FcmAdapter(env: Environment,private val json: ObjectMapper) : DevicePushAdapter {
    override val platform = Platform.ANDROID
    private val project = env.getRequiredProperty("FCM_PROJECT_ID")
    private val email = env.getRequiredProperty("FCM_CLIENT_EMAIL")
    private val signer = RSASSASigner(signingKey(env,"FCM_PRIVATE_KEY","RSA") as RSAPrivateKey)
    private var cached = ""
    private var expires = Instant.EPOCH
    @Synchronized private fun bearer(): String {
        val now = Instant.now()
        if (now.isBefore(expires)) return cached
        val url = "https://oauth2.googleapis.com/token"
        val claims = JWTClaimsSet.Builder().issuer(email).audience(url).issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(3600))).claim("scope","https://www.googleapis.com/auth/firebase.messaging").build()
        val jwt = SignedJWT(JWSHeader(JWSAlgorithm.RS256),claims).apply { sign(signer) }
        val auth = json.readTree(post(url,"grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion="+jwt.serialize(),
            mapOf("Content-Type" to "application/x-www-form-urlencoded")))
        cached = auth.required("access_token").asText()
        expires = now.plusSeconds(auth.required("expires_in").asLong()-60)
        return cached
    }
    override fun send(device: Device,alert: Alert) {
        val message = mapOf("token" to device.token,"notification" to mapOf("title" to "TolMol","body" to alert.type),
            "data" to mapOf("eventId" to alert.eventId.toString(),"aggregate" to alert.aggregate.toString(),"type" to alert.type))
        post("https://fcm.googleapis.com/v1/projects/$project/messages:send",json.writeValueAsString(mapOf("message" to message)),
            mapOf("Content-Type" to "application/json","Authorization" to "Bearer "+bearer()))
    }
}

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name=["tolmol.push.apns.enabled"],havingValue="true")
class ApnsAdapter(env: Environment,private val json: ObjectMapper) : DevicePushAdapter {
    override val platform = Platform.IOS
    private val team = env.getRequiredProperty("APNS_TEAM_ID")
    private val topic = env.getRequiredProperty("APNS_BUNDLE_ID")
    private val keyId = env.getRequiredProperty("APNS_KEY_ID")
    private val signer = ECDSASigner(signingKey(env,"APNS_PRIVATE_KEY","EC") as ECPrivateKey)
    private val host = if (env.getProperty("APNS_SANDBOX","true").toBooleanStrict()) "api.sandbox.push.apple.com" else "api.push.apple.com"
    private var issued = Instant.EPOCH
    private var token = ""
    @Synchronized private fun bearer(): String {
        val now = Instant.now()
        if (now>=issued.plusSeconds(3000)) {
            val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(),
                JWTClaimsSet.Builder().issuer(team).issueTime(Date.from(now)).build())
            jwt.sign(signer)
            token = jwt.serialize()
            issued = now
        }
        return token
    }
    override fun send(device: Device,alert: Alert) {
        val body = mapOf("aps" to mapOf("alert" to mapOf("title" to "TolMol","body" to alert.type)),
            "eventId" to alert.eventId.toString(),"aggregate" to alert.aggregate.toString())
        post("https://$host/3/device/"+device.token,json.writeValueAsString(body),mapOf("authorization" to "bearer "+bearer(),
            "apns-topic" to topic,"apns-push-type" to "alert","content-type" to "application/json"))
    }
}
