package com.gantree.cab.mailbox

/**
 * Same ids as pendant `lib/theme/catalog.ts`, in catalog order.
 * Boom hexes stay shared with gantree. A stored id that is no longer
 * here (`inlay`, `lamp`, `noir`, …) is unknown: [parseTheme] falls to boom.
 */
val THEME_PLAIN = listOf("boom", "paper", "ink")

val THEME_MOODS = listOf(
  "marquee",
  "lemonade",
  "neon",
  "fizz",
  "rain",
  "mist",
  "fuse",
  "grit",
  "siren",
  "flare",
  "static",
  "flicker",
)

val THEME_IDS = THEME_PLAIN + THEME_MOODS
const val DEFAULT_THEME = "boom"

val FONT_IDS = listOf("sm", "md", "lg", "xl")
const val DEFAULT_FONT = "sm"

fun knownTheme(v: String?): String? =
  if (v != null && v in THEME_IDS) v else null

fun parseTheme(v: String?): String = knownTheme(v) ?: DEFAULT_THEME

fun parseFont(v: String?): String =
  if (v != null && v in FONT_IDS) v else DEFAULT_FONT

fun chatSp(fontId: String): Float = when (parseFont(fontId)) {
  "md" -> 16f
  "lg" -> 20f
  "xl" -> 24f
  else -> 14f
}

fun themeLabel(id: String): String = parseTheme(id).replaceFirstChar { it.uppercase() }

fun fontLabel(id: String): String = when (parseFont(id)) {
  "md" -> "Medium"
  "lg" -> "Large"
  "xl" -> "Extra large"
  else -> "Small"
}

/**
 * What to paint. Follow Kit when the room has a known id; empty / junk / follow-off
 * keeps the human's pick. Same rule as pendant `THEME_BOOT`.
 */
fun paintedTheme(follow: Boolean, roomTheme: String, mine: String): String {
  if (!follow) {
    return parseTheme(mine)
  }
  return knownTheme(roomTheme) ?: parseTheme(mine)
}

/**
 * GET `/api/theme` vs a live notice. If the room id changed while GET was in
 * flight, keep the notice so Kit's mood is not overwritten by a stale body.
 */
fun fetchedRoomTheme(before: String, live: String, got: String): String =
  if (live != before) live else got
