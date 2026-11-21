package com.tolmol.marketplace.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class ListingUnavailableException : BusinessException(404, "Listing unavailable")
