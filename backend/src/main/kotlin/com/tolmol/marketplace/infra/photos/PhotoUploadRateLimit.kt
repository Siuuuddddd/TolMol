package com.tolmol.marketplace.infra.photos

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class PhotoUploadRateLimit(private val redis: StringRedisTemplate) : OncePerRequestFilter() {
    private val script = DefaultRedisScript(
        "local n = redis.call('INCR',KEYS[1]); if n == 1 then redis.call('EXPIRE',KEYS[1],60) end; return n",
        Long::class.javaObjectType)
    override fun shouldNotFilter(request: HttpServletRequest) =
        request.method != "POST" || !request.servletPath.matches(Regex("/api/listings/[a-fA-F0-9-]+/photos"))

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        // Trust the socket peer, never an arbitrary client-supplied X-Forwarded-For header.
        val ip = MessageDigest.getInstance("SHA-256").digest(request.remoteAddr.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val attempts = try { redis.execute(script,listOf("photos:upload:ip:$ip")) ?: Long.MAX_VALUE }
            catch (_: org.springframework.dao.DataAccessException) {
                response.status = 503
                response.contentType = "application/json"
                response.writer.write("""{"error":"Photo upload is temporarily unavailable. Please retry."}""")
                return
            }
        if (attempts > 30) {
            response.status = 429
            response.setHeader("Retry-After","60")
            response.contentType = "application/json"
            response.writer.write("""{"error":"Too many photo uploads. Try again in a minute."}""")
        } else chain.doFilter(request,response)
    }
}
