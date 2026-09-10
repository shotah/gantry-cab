package com.gantree.cab.mailbox

/** Crane slug: letter first, then letters, digits, hyphen. Max 32. */
private val SLUG = Regex("^[a-z][a-z0-9-]{0,31}$")

fun parseSlug(raw: String): String? {
  val s = raw.trim().lowercase()
  return if (SLUG.matches(s)) s else null
}

/**
 * Phone socket URL. Creds stay on `Authorization` (session JWE or spike
 * secret) — production handshake ignores `?secret=` / `?bearer=`.
 */
fun mailboxUrl(origin: String, slug: String, role: String = "phone"): String {
  val base = origin.trim().trimEnd('/')
  val host = when {
    base.startsWith("https://", ignoreCase = true) ->
      "wss://${base.substring(8)}"
    base.startsWith("http://", ignoreCase = true) ->
      "ws://${base.substring(7)}"
    else -> "wss://$base"
  }
  return "$host/ws/$slug?role=$role"
}

fun httpOrigin(origin: String): String = origin.trim().trimEnd('/')
