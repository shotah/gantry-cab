package com.gantree.cab.ui

import android.app.Activity
import android.util.Base64
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom

data class GoogleId(val idToken: String, val nonce: String)

suspend fun requestGoogleId(activity: Activity, webClientId: String): GoogleId {
  val nonceBytes = ByteArray(24)
  SecureRandom().nextBytes(nonceBytes)
  val nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  val option = GetSignInWithGoogleOption.Builder(webClientId)
    .setNonce(nonce)
    .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
  val result = try {
    CredentialManager.create(activity).getCredential(activity, request)
  } catch (e: NoCredentialException) {
    throw e
  }
  val cred = result.credential
  if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    error("expected google id token")
  }
  val google = GoogleIdTokenCredential.createFrom(cred.data)
  return GoogleId(idToken = google.idToken, nonce = nonce)
}
