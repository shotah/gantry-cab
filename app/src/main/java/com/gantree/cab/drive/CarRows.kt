package com.gantree.cab.drive

import com.gantree.cab.ChatLine

data class CarRow(
  val title: String,
  val text: String?,
)

/** Same rows the Auto ListTemplate paints — newest last, shown newest-first. */
fun carRows(lines: List<ChatLine>, slug: String, emptyTitle: String): List<CarRow> {
  val last = lines.takeLast(6)
  if (last.isEmpty()) {
    return listOf(CarRow(title = emptyTitle, text = null))
  }
  return last.asReversed().map { line ->
    CarRow(
      title = if (line.fromYou) "You" else slug.ifBlank { "kit" },
      text = when {
        line.text.isNotBlank() -> line.text
        line.photo != null -> "(photo)"
        else -> null
      },
    )
  }
}
