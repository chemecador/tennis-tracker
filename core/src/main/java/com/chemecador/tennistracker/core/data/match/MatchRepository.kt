package com.chemecador.tennistracker.core.data.match

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface MatchRepository {
    suspend fun saveCompletedMatch(record: MatchRecord): Result<String>
}

class FirestoreMatchRepository(
    private val db: FirebaseFirestore,
) : MatchRepository {
    override suspend fun saveCompletedMatch(record: MatchRecord): Result<String> = runCatching {
        db.collection(MATCHES_COLLECTION)
            .document(record.matchId)
            .set(record)
            .await()
        record.matchId
    }

    private companion object {
        const val MATCHES_COLLECTION = "matches"
    }
}
