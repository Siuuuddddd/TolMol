package com.tolmol.app.core.common

sealed interface LiveUpdate {
    data object Connected : LiveUpdate
    data object Disconnected : LiveUpdate
    data class Event(val id: String, val type: String, val aggregate: String, val data: Map<String, String>) : LiveUpdate
}
