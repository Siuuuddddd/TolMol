package com.tolmol.bidding.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class AuctionAccessException : BusinessException(403, "Auction access denied")
