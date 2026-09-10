package com.gantree.cab

import android.content.Context
import com.gantree.cab.mailbox.DEFAULT_FONT
import com.gantree.cab.mailbox.DEFAULT_THEME
import com.gantree.cab.mailbox.parseFont
import com.gantree.cab.mailbox.parseTheme

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

  var gps: Boolean
    get() = p.getString(GPS, "on") != "off"
    set(value) { p.edit().putString(GPS, if (value) "on" else "off").apply() }

  var theme: String
    get() = parseTheme(p.getString(THEME, DEFAULT_THEME))
    set(value) { p.edit().putString(THEME, parseTheme(value)).apply() }

  var font: String
    get() = parseFont(p.getString(FONT, DEFAULT_FONT))
    set(value) { p.edit().putString(FONT, parseFont(value)).apply() }

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
    private const val GPS = "gps"
    private const val THEME = "theme"
    private const val FONT = "font"
  }
}
