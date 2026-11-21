package com.tolmol.user.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class InvalidCredentialsException : BusinessException(401, "Invalid credentials")
