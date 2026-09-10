package com.gantree.cab.mailbox

val THEME_IDS = listOf("boom", "inlay", "lamp")
const val DEFAULT_THEME = "boom"

val FONT_IDS = listOf("sm", "md", "lg", "xl")
const val DEFAULT_FONT = "sm"

fun parseTheme(v: String?): String =
  if (v != null && v in THEME_IDS) v else DEFAULT_THEME

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
