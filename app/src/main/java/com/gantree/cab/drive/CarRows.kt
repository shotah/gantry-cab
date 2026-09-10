package com.gantree.cab.drive

import com.gantree.cab.ChatLine

data class CarRow(
  val title: String,
  val text: String?,
)

/** Same rows the Auto ListTemplate paints — newest last, shown newest-first. */
fun carRows(lines: List<ChatLine>, slug: String, emptyTitle: String): List<CarRow> {
  val kit = slug.ifBlank { "kit" }
  val drafting = lines.any { it.kind == "draft" && it.text.isNotBlank() }
  val last = lines.filter { it.kind != "draft" }.takeLast(6)
  if (last.isEmpty() && !drafting) {
    return listOf(CarRow(title = emptyTitle, text = null))
  }
  val rows = last.asReversed().map { line ->
    CarRow(
      title = if (line.fromYou) "You" else kit,
      text = when {
        line.text.isNotBlank() -> line.text
        line.photo != null -> "(photo)"
        else -> null
      },
    )
  }
  return if (drafting) listOf(CarRow(title = kit, text = "typing…")) + rows else rows
}
