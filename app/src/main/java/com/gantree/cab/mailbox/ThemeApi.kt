package com.gantree.cab.mailbox

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GET `/api/theme?slug=`. Returns a known id, empty when the room has none,
 * null when the request failed (keep whatever we already have).
 */
class ThemeApi(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build(),
) {
  fun fetch(origin: String, slug: String, bearer: String): String? {
    val req = Request.Builder().url(themeUrl(origin, slug)).get()
    if (bearer.isNotBlank()) {
      req.header("Authorization", "Bearer $bearer")
    }
    return try {
      client.newCall(req.build()).execute().use { res ->
        if (!res.isSuccessful) {
          return null
        }
        roomThemeFromState(res.body?.string().orEmpty())
      }
    } catch (_: Exception) {
      null
    }
  }
}

/** `{ "theme": "noir" | null, "themes": [...] }` — catalog cards are ignored. */
fun roomThemeFromState(raw: String): String {
  return try {
    val o = JSONObject(raw)
    if (!o.has("theme") || o.isNull("theme")) {
      ""
    } else {
      knownTheme(o.optString("theme")).orEmpty()
    }
  } catch (_: Exception) {
    ""
  }
}
