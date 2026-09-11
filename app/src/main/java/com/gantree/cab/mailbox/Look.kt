package com.gantree.cab.mailbox

/** Same ids as pendant `lib/theme/catalog.ts`. Boom / Inlay / Lamp hexes must not drift. */
val THEME_IDS = listOf("boom", "inlay", "lamp", "noir", "ember", "tide", "bloom")
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
