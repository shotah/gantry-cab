package com.gantree.cab.mailbox

import java.net.URI

/** Why the phone socket must not open yet. Null means go ahead. */
fun mailboxConnectError(
  slug: String,
  bearer: String,
  sessionExpired: Boolean = false,
): String? {
  if (parseSlug(slug) == null) {
    return "Talking to needs a crane slug like kit."
  }
  if (sessionExpired) {
    return "Google session expired — sign in again."
  }
  if (bearer.isBlank()) {
    return "No Google session or phone secret yet — the mailbox socket cannot open."
  }
  return null
}

/** JWT `exp` is unix seconds. Zero means unknown — do not lock the phone out. */
fun sessionExpired(expEpochSec: Long, nowEpochSec: Long): Boolean =
  expEpochSec > 0L && nowEpochSec >= expEpochSec

/** Prefer the Google session; never fall back to the spike after it expires. */
fun liveBearer(session: String, sessionExp: Long, spike: String, nowEpochSec: Long): String {
  if (session.isNotBlank()) {
    return if (sessionExpired(sessionExp, nowEpochSec)) "" else session
  }
  return spike
}

/**
 * Emulator loopback may persist the lab secret. A debug APK on a real
 * phone must not write it to disk. Google session: never keep the spike.
 */
fun persistSpikeAllowed(origin: String, hasGoogleSession: Boolean, debugBuild: Boolean): Boolean {
  if (hasGoogleSession) {
    return false
  }
  if (!debugBuild) {
    return true
  }
  return loopbackMailboxHost(origin)
}

fun loopbackMailboxHost(origin: String): Boolean {
  val raw = normalizeMailboxOrigin(origin)
  if (raw.isEmpty()) {
    return false
  }
  val host = try {
    URI(raw).host?.lowercase()
  } catch (_: Exception) {
    null
  } ?: return false
  return host == "10.0.2.2" || host == "localhost" || host == "127.0.0.1"
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

/** Email + Google sub, what the yard admin pastes. Pendant copies the same two lines. */
fun allowlistCopy(email: String, sub: String): String =
  listOf(email, sub).map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

fun mailboxTimeoutHint(): String =
  "Mailbox timed out — Android ended the background socket. Open Cab to listen again."

/** HTTP 401/403/404 are terminal; keep retrying transport failures. */
fun mailboxShouldRetry(httpCode: Int?): Boolean = when (httpCode) {
  401, 403, 404 -> false
  else -> true
}

/** 2s, 4s, 8s, 16s, 32s, then cap at 60s. */
fun mailboxRetryDelayMs(attempt: Int): Long {
  val shift = attempt.coerceIn(0, 5)
  return (2_000L shl shift).coerceAtMost(60_000L)
}
