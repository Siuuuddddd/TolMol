package com.tolmol.bidding.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class RequestConflictException : BusinessException(409, "Request key already used for a different bid")
