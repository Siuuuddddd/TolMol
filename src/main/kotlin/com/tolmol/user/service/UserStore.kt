package com.tolmol.user.service

import com.tolmol.user.domain.model.*
import java.util.UUID
interface UserStore {
    fun register(account: Account)
    fun find(email: String): Account?
    fun hasRole(id: UUID, role: Role): Boolean
    fun savePosition(id: UUID, role: Role, position: Position)
    fun recentPosition(id: UUID, role: Role): Position?
}
