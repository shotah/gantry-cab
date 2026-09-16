package com.gantree.cab.mailbox

/**
 * Settings → Language. One closed set for the mouth's ears and Kit's voice:
 * the BCP-47 the hold-to-talk recognizer listens in, and the id the Worker
 * swaps a Chirp 3 HD locale for on `POST /api/tts { text, lang }`. Not on the
 * mailbox wire — voice is mouth-local. Same ids, labels and tags as pendant
 * `lib/phone/lang.ts` (docs/frontends.md "Language"). Mandarin is `zh-CN` to a
 * recognizer but `cmn-CN` to Google TTS; the Worker owns that swap, so the
 * phone only sends the id.
 */
data class Language(val id: String, val label: String, val speech: String)

val LANGUAGES = listOf(
  Language("en", "English", "en-US"),
  Language("ja", "日本語 · Japanese", "ja-JP"),
  Language("zh", "中文 · Mandarin", "zh-CN"),
  Language("vi", "Tiếng Việt · Vietnamese", "vi-VN"),
)
val LANG_IDS = LANGUAGES.map { it.id }
const val DEFAULT_LANG = "en"

private fun rowOf(id: String?): Language = LANGUAGES.firstOrNull { it.id == id } ?: LANGUAGES[0]

/** Settings pref: junk or missing → English. */
fun parseLang(v: String?): String = LANGUAGES.firstOrNull { it.id == v }?.id ?: DEFAULT_LANG

fun langLabel(id: String): String = rowOf(id).label

/** BCP-47 for `RecognizerIntent.EXTRA_LANGUAGE`. */
fun speechLang(id: String): String = rowOf(id).speech
