package com.tolmol.platform.domain.exception

open class BusinessException(val status: Int, message: String) : RuntimeException(message)
