package com.gantree.cab.mailbox

/** Why the phone socket must not open yet. Null means go ahead. */
fun mailboxConnectError(slug: String, bearer: String): String? {
  if (parseSlug(slug) == null) {
    return "Talking to needs a crane slug like kit."
  }
  if (bearer.isBlank()) {
    return "No Google session or phone secret yet — the mailbox socket cannot open."
  }
  return null
}

fun mailboxSocketHint(code: Int?, detail: String?): String {
  val bit = detail?.trim().orEmpty().ifEmpty { "retrying" }
  return when (code) {
    401, 403 ->
      "Mailbox refused the socket (HTTP $code). Google worked, but this crane’s room list may not include you."
    404 -> "Mailbox has no socket for this crane name (HTTP 404)."
    null -> "Mailbox socket down — ${bit.take(160)}"
    else -> "Mailbox socket down (HTTP $code) — ${bit.take(120)}"
  }
}

fun mailboxSignedInHint(email: String, cranes: List<String>): String {
  val who = email.ifBlank { "you" }
  if (cranes.isEmpty()) {
    return "Google worked ($who), but this mailbox listed no cranes for you. The socket stays offline until you’re on the room list."
  }
  return "Signed in as $who"
}
