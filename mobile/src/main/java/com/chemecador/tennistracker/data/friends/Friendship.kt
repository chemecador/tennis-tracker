package com.chemecador.tennistracker.data.friends

enum class FriendshipStatus { PENDING, ACCEPTED }

data class Friendship(
    val id: String,
    val participants: List<String>,
    val status: FriendshipStatus,
    val requestedBy: String,
) {
    fun otherUid(myUid: String): String = participants.first { it != myUid }
}
