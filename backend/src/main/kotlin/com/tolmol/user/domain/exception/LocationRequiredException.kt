package com.tolmol.user.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class LocationRequiredException : BusinessException(422, "A recent location is required")
