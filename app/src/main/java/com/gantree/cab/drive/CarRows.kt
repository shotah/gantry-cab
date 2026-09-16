package com.gantree.cab.drive

import com.gantree.cab.ChatLine
import com.gantree.cab.mailbox.displaySlug

data class CarRow(
  val title: String,
  val text: String?,
)

/** Chronological turns for Auto `ConversationItem` (oldest first). Drafts stay off. */
data class CarTurn(
  val fromYou: Boolean,
  val text: String,
  val at: Long,
  /** Unread is what Auto offers to read aloud; your own turns and the starter are not. */
  val read: Boolean = fromYou,
)

/**
 * The card before anything has been said. Auto's **Reply** on a
 * `ConversationItem` is the only way a template app gets the driver's voice,
 * and the item refuses an empty message list — so an empty thread painted
 * nothing to tap. This is Kit's first card, already read, saying what to do.
 * UI only: not in Mouth, not on the wire, gone once a real turn lands.
 */
fun carStarter(slug: String, now: Long): CarTurn =
  CarTurn(fromYou = false, text = "Nothing said yet. Tap Reply and talk to ${displaySlug(slug)}.", at = now, read = true)

/** What the conversation card holds: the thread, or the starter when there is none. */
fun carMessages(lines: List<ChatLine>, slug: String, now: Long): List<CarTurn> =
  carTurns(lines).ifEmpty { listOf(carStarter(slug, now)) }

fun carTurns(lines: List<ChatLine>, cap: Int = 6): List<CarTurn> {
  return lines.filter { it.kind != "draft" }.takeLast(cap).map { line ->
    CarTurn(
      fromYou = line.fromYou,
      text = when {
        line.text.isNotBlank() -> line.text
        line.photo != null -> "(photo)"
        else -> "(ping)"
      },
      at = line.at,
    )
  }
}

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
