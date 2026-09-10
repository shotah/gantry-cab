package com.gantree.cab.ui

import android.app.Activity
import android.util.Base64
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom

data class GoogleId(val idToken: String, val nonce: String)

suspend fun requestGoogleId(activity: Activity, webClientId: String): GoogleId {
  val nonceBytes = ByteArray(24)
  SecureRandom().nextBytes(nonceBytes)
  val nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  val option = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(webClientId)
    .setNonce(nonce)
    .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
  val result = CredentialManager.create(activity).getCredential(activity, request)
  val cred = result.credential
  if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    error("expected google id token")
  }
  val google = GoogleIdTokenCredential.createFrom(cred.data)
  return GoogleId(idToken = google.idToken, nonce = nonce)
}
