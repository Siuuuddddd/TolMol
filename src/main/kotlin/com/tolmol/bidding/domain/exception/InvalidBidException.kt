package com.tolmol.bidding.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class InvalidBidException : BusinessException(422, "Bid must meet the starting rate and exceed the current leader")
