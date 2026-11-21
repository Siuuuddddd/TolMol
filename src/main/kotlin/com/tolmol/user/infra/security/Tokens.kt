package com.tolmol.user.infra.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.tolmol.user.service.TokenIssuer
import org.springframework.context.annotation.*
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import java.time.Instant
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@Configuration
class Tokens(env: Environment) {
    private val key = SecretKeySpec(env.getRequiredProperty("tolmol.jwt.secret").toByteArray().also { require(it.size >= 32) }, "HmacSHA256")
    @Bean fun decoder(): JwtDecoder = NimbusJwtDecoder.withSecretKey(key).build().apply {
        setJwtValidator(JwtValidators.createDefaultWithIssuer("tolmol"))
    }
    @Bean fun issuer(): TokenIssuer {
        val encoder = NimbusJwtEncoder(ImmutableSecret(key))
        return object : TokenIssuer {
            override fun issue(id: UUID): String {
                val now = Instant.now()
                val claims = JwtClaimsSet.builder().issuer("tolmol").subject(id.toString()).issuedAt(now).expiresAt(now.plusSeconds(3600)).build()
                return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).tokenValue
            }
        }
    }
}
