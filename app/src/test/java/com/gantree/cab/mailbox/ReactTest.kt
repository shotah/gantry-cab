package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactTest {
  @Test
  fun paletteIsTheCraneList() {
    assertEquals(
      listOf("👍", "👎", "❤️", "🔥", "🤣", "😢", "🤔", "🙏", "👀", "🎉", "💯", "👏"),
      REACTION_PALETTE,
    )
    for (emoji in REACTION_PALETTE) {
      assertEquals(emoji, parseReactionText(emoji))
    }
  }

  @Test
  fun parseReactionTextNormalizesAndRefusesJunk() {
    assertEquals("👍", parseReactionText("👍"))
    assertEquals("❤️ 🔥", parseReactionText("  ❤️   🔥 "))
    assertEquals("", parseReactionText(""))
    assertEquals("", parseReactionText("   "))
    assertEquals("", parseReactionText(null))
    assertNull(parseReactionText("👍\u0000"))
    assertEquals("👨‍👩‍👧", parseReactionText("👨‍👩‍👧"))
    assertNull(parseReactionText("🔥".repeat(REACTION_TEXT_MAX)))
  }

  @Test
  fun toggleClearsTheSameEmoji() {
    assertEquals("👍", toggleReaction(null, "👍"))
    assertEquals("", toggleReaction("👍", "👍"))
    assertEquals("❤️", toggleReaction("👍", "❤️"))
  }

  @Test
  fun onlyKitReplyAndPushCanTakeAReaction() {
    assertTrue(canReact(fromYou = false, kind = "reply", id = "r1"))
    assertTrue(canReact(fromYou = false, kind = "push", id = "p1"))
    assertFalse(canReact(fromYou = true, kind = "inbound", id = "a1"))
    assertFalse(canReact(fromYou = false, kind = "draft", id = "__draft__"))
    assertFalse(canReact(fromYou = false, kind = "reply", id = ""))
  }

  @Test
  fun reactFrameIsKindIdAndText() {
    val set = parseFrame(encodeFrame(reactFrame("r1", "👍")))!!
    assertEquals("react", set.kind)
    assertEquals("r1", set.id)
    assertEquals("👍", set.text)
    val clear = parseFrame(encodeFrame(reactFrame("r1", "")))!!
    assertNull(clear.text)
    assertEquals("", parseReactionText(clear.text))
    assertTrue(encodeFrame(reactFrame("r1", "")).contains("\"text\":\"\""))
  }
}
