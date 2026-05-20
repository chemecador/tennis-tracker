package com.chemecador.tennistracker.data.profile

data class UserProfile(
    val uid: String,
    val displayName: String,
    val username: String,
    val eloTennis: Int,
    val eloPadel: Int,
    val matchesPlayed: Int,
)
