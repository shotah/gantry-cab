package com.gantree.cab.mailbox

fun googleSignInHint(className: String, message: String?): String {
  val msg = message?.trim().orEmpty()
  return when {
    className.contains("Cancellation", ignoreCase = true) -> "Google sign-in cancelled."
    msg.contains("28444") || msg.contains("Developer console", ignoreCase = true) ->
      "GCP isn’t set up for this APK. Add an Android OAuth client: package com.gantree.cab and this build’s SHA-1."
    className.contains("NoCredential", ignoreCase = true) ->
      "No Google account, or this APK’s SHA-1 isn’t an Android OAuth client for com.gantree.cab."
    msg.isNotBlank() -> msg
    else -> "Google sign-in failed."
  }
}
