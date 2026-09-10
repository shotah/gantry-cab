package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiTest {
  @Test
  fun shortcodesResolveNamesAndAliases() {
    assertEquals("🤷", emojiForShortcode("shrug"))
    assertEquals("🤷", emojiForShortcode("person_shrugging"))
    assertEquals("👍", emojiForShortcode("+1"))
    assertEquals("👍", emojiForShortcode("thumbs-up"))
    assertEquals(null, emojiForShortcode("nope"))
  }

  @Test
  fun finishedColonNameConvertsAndUnknownStays() {
    val next = applyEmoji("well :shrug: ok", 13, "type")
    assertEquals("well 🤷 ok", next.text)
    assertEquals(8, next.cursor)
    assertEquals("see :notacode: later", applyEmoji("see :notacode: later", 18, "type").text)
    assertEquals("https://example.com", applyEmoji("https://example.com", 19, "type").text)
  }

  @Test
  fun grinConvertsWhenFinishedAndOnSend() {
    assertEquals("yo :D", applyEmoji("yo :D", 5, "type").text)
    assertEquals("yo 😀 ", applyEmoji("yo :D ", 6, "type").text)
    assertEquals("yo 😀", applyEmoji("yo :D", 5, "send").text)
    assertEquals("😀!", applyEmoji(":D!", 3, "type").text)
    assertEquals("wow 🐶", applyEmoji("wow :dog:", 9, "type").text)
  }

  @Test
  fun closedFacesConvertAndMidWordColonStays() {
    assertEquals("hi 😊", applyEmoji("hi :)", 5, "type").text)
    assertEquals("see:(", applyEmoji("see:(", 6, "type").text)
    assertEquals("😆 ", applyEmoji("xD ", 3, "type").text)
    assertEquals("🙁.", applyEmoji(":(.", 3, "type").text)
    assertEquals("(😊", applyEmoji("(:)", 3, "type").text)
  }

  @Test
  fun caretParksAfterAReplacementThatContainsIt() {
    val next = applyEmoji(":shrug:", 4, "type")
    assertEquals("🤷", next.text)
    assertEquals(next.text.length, next.cursor)
  }

  @Test
  fun cursorPastTheEndIsClamped() {
    val next = applyEmoji("hi :)", 99, "type")
    assertEquals("hi 😊", next.text)
    assertEquals(next.text.length, next.cursor)
  }

  @Test
  fun pickerFiltersTheCatalog() {
    val idle = searchEmoji("")
    assertTrue(idle.any { it.name == "shrug" })
    assertTrue(idle.any { it.name == "sunny" })
    assertEquals(44, idle.size)
    assertTrue(idle.size < emojiCatalog.size)
    assertEquals(listOf("fire"), searchEmoji(":fire").map { it.name })
    assertEquals(listOf("fire"), searchEmoji("🔥").map { it.name })
    assertEquals(emptyList<EmojiEntry>(), searchEmoji("no-such-face"))
    val names = emojiCatalog.map { it.name }
    assertEquals(names.size, names.toSet().size)
  }
}
