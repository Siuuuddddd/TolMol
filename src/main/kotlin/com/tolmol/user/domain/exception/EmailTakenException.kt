package com.tolmol.user.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class EmailTakenException : BusinessException(409, "Email already registered")
