package com.chemecador.tennistracker.data.profile

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UsernameTakenException : Exception("Username already in use")

class InvalidUsernameException : Exception("Invalid username format")

class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUserProfile(uid))
            }
        awaitClose { registration.remove() }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val normalized = username.normalizeUsername()
        if (!normalized.isValidUsername()) throw InvalidUsernameException()
        if (normalized in RESERVED_USERNAMES) return false
        val snapshot = firestore.collection(USERNAMES).document(normalized).get().await()
        return !snapshot.exists()
    }

    suspend fun findUserByUsername(username: String): UserProfile? {
        val normalized = username.normalizeUsername()
        if (!normalized.isValidUsername()) return null
        val usernameDoc = firestore.collection(USERNAMES).document(normalized).get().await()
        val uid = usernameDoc.getString("uid") ?: return null
        val userDoc = firestore.collection(USERS).document(uid).get().await()
        return userDoc.toUserProfile(uid)
    }

    suspend fun getProfileOnce(uid: String): UserProfile? {
        val doc = firestore.collection(USERS).document(uid).get().await()
        return doc.toUserProfile(uid)
    }

    suspend fun claimUsernameAndCreateProfile(
        uid: String,
        username: String,
        displayName: String,
    ) {
        val normalized = username.normalizeUsername()
        if (!normalized.isValidUsername() || normalized in RESERVED_USERNAMES) {
            throw InvalidUsernameException()
        }
        val usernameRef = firestore.collection(USERNAMES).document(normalized)
        val userRef = firestore.collection(USERS).document(uid)

        try {
            firestore.runTransaction { tx ->
                if (tx.get(usernameRef).exists()) throw UsernameTakenException()
                if (tx.get(userRef).exists()) throw UsernameTakenException()
                tx.set(usernameRef, mapOf("uid" to uid))
                tx.set(
                    userRef,
                    mapOf(
                        "displayName" to displayName.ifBlank { normalized },
                        "username" to normalized,
                        "elo" to mapOf("tennis" to INITIAL_ELO, "padel" to INITIAL_ELO),
                        "stats" to mapOf("matchesPlayed" to 0),
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.ABORTED ||
                e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS
            ) {
                throw UsernameTakenException()
            }
            throw e
        }
    }

    private fun DocumentSnapshot.toUserProfile(uid: String): UserProfile? {
        if (!exists()) return null
        val username = getString("username") ?: return null
        val displayName = getString("displayName") ?: username
        val elo = get("elo") as? Map<*, *>
        val stats = get("stats") as? Map<*, *>
        val eloTennis = (elo?.get("tennis") as? Number)?.toInt() ?: INITIAL_ELO
        val eloPadel = (elo?.get("padel") as? Number)?.toInt() ?: INITIAL_ELO
        val matchesPlayed = (stats?.get("matchesPlayed") as? Number)?.toInt() ?: 0
        return UserProfile(
            uid = uid,
            displayName = displayName,
            username = username,
            eloTennis = eloTennis,
            eloPadel = eloPadel,
            matchesPlayed = matchesPlayed,
        )
    }

    companion object {
        private const val USERS = "users"
        private const val USERNAMES = "usernames"
        const val INITIAL_ELO = 1200
        private val USERNAME_REGEX = Regex("^[a-z0-9_.]{3,20}$")
        val RESERVED_USERNAMES = setOf(
            "admin", "root", "null", "undefined", "tennistracker", "support",
        )

        fun String.normalizeUsername(): String = trim().lowercase()
        fun String.isValidUsername(): Boolean = USERNAME_REGEX.matches(this)
    }
}
