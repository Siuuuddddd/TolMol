package com.tolmol.user.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class RoleRequiredException : BusinessException(403, "Role unavailable")
