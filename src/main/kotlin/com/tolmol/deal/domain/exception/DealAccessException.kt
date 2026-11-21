package com.tolmol.deal.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class DealAccessException : BusinessException(403, "Only the winning buyer inspects the lot")
