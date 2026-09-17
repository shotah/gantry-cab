package com.gantree.cab.mailbox

/**
 * Emoji on a bubble, both ways. Same list and rules as pendant
 * `lib/mailbox/react.ts` / crane `channel.Palette`.
 * `{ kind: "react", id: <bubble id>, text: "👍" }` — empty text clears.
 * Not a turn: no phone queue, no cursor. A down crane is the Worker's
 * `q:crane:<id>` (latest wins); this mouth does not wait or retry.
 */

/** What the picker shows and what the model may react with. */
val REACTION_PALETTE = listOf("👍", "👎", "❤️", "🔥", "🤣", "😢", "🤔", "🙏", "👀", "🎉", "💯", "👏")

/** A reaction is a few emoji at most. Bytes, UTF-8. */
const val REACTION_TEXT_MAX = 64

/** Hold this long on a Kit bubble and the palette opens. */
const val REACT_HOLD_MS = 450L

/** A drag this far cancels the hold (pendant Thread.tsx). */
const val REACT_HOLD_SLOP_PX = 10f

/**
 * Normalize a reaction's text: trimmed, single-spaced, no control
 * characters, within the byte cap. `""` is a clear. `null` is junk.
 */
fun parseReactionText(raw: String?): String? {
  if (raw == null) {
    return ""
  }
  val text = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
  if (text.isEmpty()) {
    return ""
  }
  for (ch in text) {
    if (ch == '\u200d' || ch == '\ufe0f') {
      continue
    }
    val cat = ch.category
    if (cat == CharCategory.CONTROL || cat == CharCategory.FORMAT) {
      return null
    }
  }
  if (text.toByteArray(Charsets.UTF_8).size > REACTION_TEXT_MAX) {
    return null
  }
  return text
}

/** Tap the emoji you already set and it clears; anything else replaces. */
fun toggleReaction(current: String?, emoji: String): String =
  if (current == emoji) "" else emoji

/** Kit's finished turns take a reaction; a draft, a refusal, or your own bubble does not. */
fun canReact(fromYou: Boolean, kind: String?, id: String): Boolean =
  !fromYou && (kind == "reply" || kind == "push") && id.isNotEmpty()

fun reactFrame(id: String, text: String): WireFrame =
  WireFrame(kind = "react", id = id, text = text)
