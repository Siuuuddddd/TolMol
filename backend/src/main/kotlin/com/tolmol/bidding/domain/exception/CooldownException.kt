package com.tolmol.bidding.domain.exception

import com.tolmol.platform.domain.exception.BusinessException
import java.time.Instant
class CooldownException(next: Instant) : BusinessException(429,"Next bid allowed at $next")
