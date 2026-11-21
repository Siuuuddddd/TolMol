package com.tolmol.deal.service

import com.tolmol.deal.domain.model.*
import com.tolmol.deal.domain.exception.*
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class DealService(private val store: DealStore) {
    private fun authorize(user: UUID,deal: Deal): Deal {
        if (user!=deal.seller && user!=deal.buyer) throw DealUnavailableException()
        return deal
    }
    fun find(user: UUID,id: UUID) = authorize(user,store.find(id))
    fun mine(user: UUID) = store.mine(user)

    fun inspect(user: UUID,id: UUID,weight: BigDecimal): Deal = store.change(id) { deal ->
        authorize(user,deal)
        if (user!=deal.buyer) throw DealAccessException()
        if (deal.state!=DealState.PENDING_PICKUP || deal.actualWeight!=null) throw DealConflictException()
        require(weight.signum()>0 && weight.scale()<=3) { "Weight must be positive with at most three decimals" }
        if (weight.compareTo(deal.estimatedWeight)==0) {
            deal.copy(actualWeight=weight,amount=deal.rate.multiply(weight).setScale(2,RoundingMode.HALF_UP),proposedBy=user)
        } else deal.copy(actualWeight=weight,state=DealState.WEIGHT_DISPUTE)
    }
    fun propose(user: UUID,id: UUID,amount: BigDecimal): Deal = store.change(id) { deal ->
        authorize(user,deal)
        if (deal.state!=DealState.WEIGHT_DISPUTE) throw DealConflictException()
        require(amount.signum()>0 && amount.scale()<=2) { "Enter a positive total with at most two decimals" }
        deal.copy(amount=amount,proposedBy=user)
    }
    fun confirm(user: UUID,id: UUID,revision: Long): Deal = store.change(id) { deal ->
        authorize(user,deal)
        if (deal.state !in setOf(DealState.PENDING_PICKUP,DealState.WEIGHT_DISPUTE) ||
            deal.amount==null || deal.proposedBy==user || deal.revision!=revision) throw DealConflictException()
        deal.copy(state=DealState.COMPLETED)
    }
    fun cancel(user: UUID,id: UUID): Deal = store.change(id) { deal ->
        authorize(user,deal)
        if (deal.state!=DealState.WEIGHT_DISPUTE) throw DealConflictException()
        val replacement = store.relist(deal,requireNotNull(deal.actualWeight))
        deal.copy(state=DealState.CANCELLED_RELISTED,relisted=replacement,amount=null,proposedBy=null)
    }
}
