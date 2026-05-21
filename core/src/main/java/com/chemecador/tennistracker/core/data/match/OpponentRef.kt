package com.chemecador.tennistracker.core.data.match

sealed interface OpponentRef {
    val token: String

    data class Registered(val uid: String) : OpponentRef {
        override val token: String = uid
    }

    data class Guest(val name: String) : OpponentRef {
        override val token: String = "guest:$name"
    }
}
