package com.tolmol.deal.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class DealConflictException : BusinessException(409, "Action unavailable for the current deal or proposal")
