package com.gantree.cab.mailbox

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class TtsResult {
  class Ok(val mp3: ByteArray) : TtsResult()
  data class Err(val reason: SpeakFail) : TtsResult()
}

fun ttsUrl(origin: String): String = httpOrigin(origin) + "/api/tts"

/**
 * `POST /api/tts { text, lang }` → MP3 bytes. The same Worker route the PWA
 * plays through; the phone sends its Bearer session instead of the cookie.
 * `lang` is the Settings → Language id (`en` / `ja` / `zh`); the Worker keeps
 * the speaker and swaps the Chirp locale, and drops an id it does not know.
 * 404 until the Worker has a Chirp key (or `VOICE=off`), 401 when the session
 * is gone, 429 too many, 502 when Google refused. Nothing is stored and the
 * Worker never logs the text (pendant docs/voice.md).
 */
class TtsApi(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build(),
) {
  fun synthesize(origin: String, bearer: String, text: String, lang: String = DEFAULT_LANG): TtsResult {
    val body = JSONObject().put("text", text).put("lang", parseLang(lang)).toString().toRequestBody(JSON)
    val req = Request.Builder().url(ttsUrl(origin)).post(body)
    if (bearer.isNotBlank()) {
      req.header("Authorization", "Bearer $bearer")
    }
    return try {
      client.newCall(req.build()).execute().use { res ->
        if (!res.isSuccessful) {
          return TtsResult.Err(speakFailFromStatus(res.code))
        }
        val bytes = res.body?.bytes() ?: ByteArray(0)
        if (bytes.isEmpty()) TtsResult.Err(SpeakFail.VENDOR) else TtsResult.Ok(bytes)
      }
    } catch (_: IOException) {
      TtsResult.Err(SpeakFail.OFFLINE)
    }
  }

  companion object {
    private val JSON = "application/json; charset=utf-8".toMediaType()
  }
}
