package com.tolmol.bidding.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class AuctionClosedException : BusinessException(409, "Auction closed")
