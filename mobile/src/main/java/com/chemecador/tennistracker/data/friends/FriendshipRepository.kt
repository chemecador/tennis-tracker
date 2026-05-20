package com.chemecador.tennistracker.data.friends

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendshipAlreadyExistsException : Exception("Friendship already exists")

class FriendshipRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    fun observeFriendships(myUid: String): Flow<List<Friendship>> = callbackFlow {
        val registration = firestore.collection(FRIENDSHIPS)
            .where(Filter.arrayContains("participants", myUid))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.toFriendship() } ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendRequest(fromUid: String, toUid: String) {
        require(fromUid != toUid) { "Cannot send a friend request to yourself" }
        val id = friendshipId(fromUid, toUid)
        val ref = firestore.collection(FRIENDSHIPS).document(id)
        val existing = ref.get().await()
        if (existing.exists()) throw FriendshipAlreadyExistsException()
        ref.set(
            mapOf(
                "participants" to listOf(fromUid, toUid).sorted(),
                "status" to "pending",
                "requestedBy" to fromUid,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun acceptRequest(friendshipId: String) {
        firestore.collection(FRIENDSHIPS).document(friendshipId).update(
            mapOf(
                "status" to "accepted",
                "acceptedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun deleteFriendship(friendshipId: String) {
        firestore.collection(FRIENDSHIPS).document(friendshipId).delete().await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFriendship(): Friendship? {
        @Suppress("UNCHECKED_CAST")
        val participants = get("participants") as? List<String> ?: return null
        if (participants.size != 2) return null
        val statusStr = getString("status") ?: return null
        val status = when (statusStr) {
            "pending" -> FriendshipStatus.PENDING
            "accepted" -> FriendshipStatus.ACCEPTED
            else -> return null
        }
        val requestedBy = getString("requestedBy") ?: return null
        return Friendship(
            id = id,
            participants = participants,
            status = status,
            requestedBy = requestedBy,
        )
    }

    companion object {
        private const val FRIENDSHIPS = "friendships"

        fun friendshipId(uidA: String, uidB: String): String {
            val (lo, hi) = listOf(uidA, uidB).sorted()
            return "${lo}_${hi}"
        }
    }
}
