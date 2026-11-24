package com.tolmol.user.api.exception_handling

import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import com.tolmol.platform.domain.exception.BusinessException

@RestControllerAdvice(basePackages = ["com.tolmol.user.api.controllers"])
class UserExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun business(e: BusinessException) = ResponseEntity.status(e.status).body(mapOf("error" to e.message))
    @ExceptionHandler(IllegalArgumentException::class)
    fun invalid(e: IllegalArgumentException) = ResponseEntity.badRequest().body(mapOf("error" to e.message))
}
