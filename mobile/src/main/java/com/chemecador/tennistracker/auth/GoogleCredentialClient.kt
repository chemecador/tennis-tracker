package com.chemecador.tennistracker.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.chemecador.tennistracker.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class GoogleCredentialClient(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val webClientId: String = context.getString(R.string.default_web_client_id)

    suspend fun requestIdToken(): String {
        val response = try {
            credentialManager.getCredential(context, buildRequest(filterAuthorized = true))
        } catch (_: NoCredentialException) {
            credentialManager.getCredential(context, buildRequest(filterAuthorized = false))
        }
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Unexpected credential type: ${credential.type}")
        }
        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            error("Invalid Google ID token: ${e.message}")
        }
    }

    private fun buildRequest(filterAuthorized: Boolean): GetCredentialRequest {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(filterAuthorized)
            .setAutoSelectEnabled(filterAuthorized)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }
}
