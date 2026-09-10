package com.gantree.cab

import android.content.Context

class CabPrefs(ctx: Context) {
  private val p = ctx.getSharedPreferences("cab", Context.MODE_PRIVATE)

  var origin: String
    get() = p.getString(ORIGIN, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.MAILBOX_ORIGIN
    set(value) { p.edit().putString(ORIGIN, value.trim()).apply() }

  var slug: String
    get() = p.getString(SLUG, "kit") ?: "kit"
    set(value) { p.edit().putString(SLUG, value.trim().lowercase()).apply() }

  var spike: String
    get() = p.getString(SPIKE, "") ?: ""
    set(value) { p.edit().putString(SPIKE, value).apply() }

  var session: String
    get() = p.getString(SESSION, "") ?: ""
    set(value) { p.edit().putString(SESSION, value).apply() }

  var email: String
    get() = p.getString(EMAIL, "") ?: ""
    set(value) { p.edit().putString(EMAIL, value).apply() }

  val bearer: String
    get() = session.ifBlank { spike }

  val signedIn: Boolean
    get() = bearer.isNotBlank()

  fun signOut() {
    p.edit().remove(SESSION).remove(EMAIL).apply()
  }

  companion object {
    private const val ORIGIN = "origin"
    private const val SLUG = "slug"
    private const val SPIKE = "spike"
    private const val SESSION = "session"
    private const val EMAIL = "email"
  }
}
