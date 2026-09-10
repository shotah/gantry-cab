package com.gantree.cab.mailbox

fun googleSignInHint(className: String, message: String?, causeLines: List<String> = emptyList()): String {
  val msg = message?.trim().orEmpty()
  val summary = when {
    msg.contains("28444") || msg.contains("Developer console", ignoreCase = true) ->
      "GCP isn’t set up for this APK. Add an Android OAuth client: package com.gantree.cab and this build’s SHA-1."
    className.contains("Cancellation", ignoreCase = true) ->
      "Google closed the sheet after the account. That is often a SHA-1 mismatch, not you hitting Back."
    className.contains("NoCredential", ignoreCase = true) ->
      "No Google account, or this APK’s SHA-1 isn’t an Android OAuth client for com.gantree.cab."
    msg.isNotBlank() -> msg
    else -> "Google sign-in failed."
  }
  val trail = buildList {
    add(className.substringAfterLast('.'))
    if (msg.isNotBlank()) add(msg)
    addAll(causeLines.map { it.trim() }.filter { it.isNotBlank() })
  }.distinct()
  return if (trail.isEmpty()) summary else summary + "\n" + trail.joinToString(" → ")
}

fun googleSignInHint(err: Throwable): String {
  val causes = generateSequence(err.cause) { it.cause }.mapNotNull { t ->
    val m = t.message?.trim().orEmpty()
    if (m.isEmpty()) t.javaClass.simpleName else "${t.javaClass.simpleName}: $m"
  }.toList()
  return googleSignInHint(err.javaClass.name, err.message, causes)
}
