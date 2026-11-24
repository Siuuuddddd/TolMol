package com.tolmol.deal.service

import com.tolmol.deal.domain.model.Deal
import java.math.BigDecimal
import java.util.UUID
interface DealStore {
    fun find(listing: UUID): Deal
    fun mine(user: UUID): List<Deal>
    fun change(listing: UUID,transition: (Deal)->Deal): Deal
    fun relist(deal: Deal,weight: BigDecimal): UUID
}
