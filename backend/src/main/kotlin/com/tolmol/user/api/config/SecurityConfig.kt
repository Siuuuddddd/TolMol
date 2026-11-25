package com.tolmol.user.api.config

import org.springframework.context.annotation.*
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
@Configuration
class SecurityConfig {
    @Bean fun encoder() = BCryptPasswordEncoder()
    @Bean fun chain(http: HttpSecurity): SecurityFilterChain = http.csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { it.requestMatchers("/health","/auth/google","/api/auth/register","/api/auth/login","/api/auth/social").permitAll().anyRequest().authenticated() }
        .oauth2ResourceServer { it.jwt {} }.build()
}
