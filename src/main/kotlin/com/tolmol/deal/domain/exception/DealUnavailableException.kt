package com.tolmol.deal.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class DealUnavailableException : BusinessException(404, "Deal unavailable")
