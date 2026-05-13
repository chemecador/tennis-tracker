package com.chemecador.tennistracker.wear.auth

import android.content.Context
import android.net.Uri
import androidx.wear.phone.interactions.authentication.CodeChallenge
import androidx.wear.phone.interactions.authentication.CodeVerifier
import androidx.wear.phone.interactions.authentication.OAuthRequest
import androidx.wear.phone.interactions.authentication.OAuthResponse
import androidx.wear.phone.interactions.authentication.RemoteAuthClient
import com.chemecador.tennistracker.wear.BuildConfig
import com.chemecador.tennistracker.wear.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.net.toUri

class WearGoogleAuth(private val context: Context) {

    private val webClientId: String = context.getString(R.string.default_web_client_id)

    suspend fun signIn(): String {
        val verifier = CodeVerifier()
        val challenge = CodeChallenge(verifier)

        val authUrl = "https://accounts.google.com/o/oauth2/v2/auth".toUri()
            .buildUpon()
            .appendQueryParameter("scope", "openid email profile")
            .appendQueryParameter("prompt", "select_account")
            .build()

        val request = OAuthRequest.Builder(context)
            .setAuthProviderUrl(authUrl)
            .setClientId(webClientId)
            .setCodeChallenge(challenge)
            .build()

        val response = sendAuthorizationRequest(request)

        val responseUri = response.responseUrl
            ?: error("Authorization returned no redirect URL")
        val code = responseUri.getQueryParameter("code")
            ?: error(
                "Missing 'code' in response (error=${responseUri.getQueryParameter("error")})"
            )

        return exchangeCodeForIdToken(
            code = code,
            codeVerifier = verifier.value,
            redirectUri = OAuthRequest.WEAR_REDIRECT_URL_PREFIX + context.packageName,
        )
    }

    private suspend fun sendAuthorizationRequest(
        request: OAuthRequest,
    ): OAuthResponse = suspendCancellableCoroutine { cont ->
        val client = RemoteAuthClient.create(context)
        val executor = Executors.newSingleThreadExecutor()
        val callback = object : RemoteAuthClient.Callback() {
            override fun onAuthorizationResponse(
                request: OAuthRequest,
                response: OAuthResponse,
            ) {
                if (response.errorCode == RemoteAuthClient.NO_ERROR) {
                    cont.resume(response)
                } else {
                    cont.resumeWithException(
                        IllegalStateException("Wear OAuth error code ${response.errorCode}")
                    )
                }
                client.close()
                executor.shutdown()
            }

            override fun onAuthorizationError(request: OAuthRequest, errorCode: Int) {
                cont.resumeWithException(
                    IllegalStateException("Wear OAuth error code $errorCode")
                )
                client.close()
                executor.shutdown()
            }
        }
        cont.invokeOnCancellation {
            client.close()
            executor.shutdown()
        }
        client.sendAuthorizationRequest(request, executor, callback)
    }

    private suspend fun exchangeCodeForIdToken(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): String = withContext(Dispatchers.IO) {
        val clientSecret = BuildConfig.GOOGLE_OAUTH_CLIENT_SECRET
        if (clientSecret.isBlank()) {
            error("Missing GOOGLE_OAUTH_CLIENT_SECRET in local.properties")
        }
        val body = buildString {
            append("client_id=").append(Uri.encode(webClientId))
            append("&client_secret=").append(Uri.encode(clientSecret))
            append("&code=").append(Uri.encode(code))
            append("&code_verifier=").append(Uri.encode(codeVerifier))
            append("&grant_type=authorization_code")
            append("&redirect_uri=").append(Uri.encode(redirectUri))
        }.toByteArray(Charsets.UTF_8)

        val connection = (URL("https://oauth2.googleapis.com/token").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body) }
            val ok = connection.responseCode in 200..299
            val stream = if (ok) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (!ok) error("Token endpoint ${connection.responseCode}: $response")
            val json = JSONObject(response)
            json.optString("id_token").takeIf { it.isNotBlank() }
                ?: error("Token response missing id_token: $response")
        } finally {
            connection.disconnect()
        }
    }
}
