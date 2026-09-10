package com.gantree.cab.drive

import com.gantree.cab.ChatLine
import org.junit.Assert.assertEquals
import org.junit.Test

class CarRowsTest {
  @Test
  fun emptyUsesTheEmptyTitle() {
    val rows = carRows(emptyList(), "kit", "No messages yet.")
    assertEquals(listOf(CarRow("No messages yet.", null)), rows)
  }

  @Test
  fun newestFirstWithYouAndSlug() {
    val rows = carRows(
      listOf(
        ChatLine("1", true, "hello", "inbound"),
        ChatLine("2", false, "hi", "reply"),
      ),
      "kit",
      "empty",
    )
    assertEquals("kit", rows[0].title)
    assertEquals("hi", rows[0].text)
    assertEquals("You", rows[1].title)
  }

  @Test
  fun photoOnlyRowUsesACaption() {
    val rows = carRows(
      listOf(ChatLine("1", true, "", "inbound", photo = "data:image/jpeg;base64,QQ")),
      "kit",
      "empty",
    )
    assertEquals("(photo)", rows[0].text)
  }

  @Test
  fun draftBecomesOneTypingRow() {
    val rows = carRows(
      listOf(
        ChatLine("s1", true, "On the dock — is the gate still open?", "inbound"),
        ChatLine("__draft__", false, "⏳ Gate's on the latch", "draft"),
      ),
      "kit",
      "empty",
    )
    assertEquals(2, rows.size)
    assertEquals("kit", rows[0].title)
    assertEquals("typing…", rows[0].text)
    assertEquals("You", rows[1].title)
  }

  @Test
  fun carTurnsAreOldestFirstAndSkipDrafts() {
    val turns = carTurns(
      listOf(
        ChatLine("1", true, "hello", "inbound", at = 1L),
        ChatLine("__draft__", false, "⏳", "draft", at = 2L),
        ChatLine("2", false, "", "reply", photo = "data:image/jpeg;base64,QQ", at = 3L),
      ),
    )
    assertEquals(2, turns.size)
    assertEquals("hello", turns[0].text)
    assertEquals(true, turns[0].fromYou)
    assertEquals("(photo)", turns[1].text)
    assertEquals(emptyList<CarTurn>(), carTurns(emptyList()))
    val many = (1..8).map { ChatLine("$it", false, "n$it", "reply", at = it.toLong()) }
    assertEquals(listOf("n3", "n4", "n5", "n6", "n7", "n8"), carTurns(many).map { it.text })
  }

  @Test
  fun blankSlugFallsBackAndOnlyTheLastSixShow() {
    val lines = (1..8).map { ChatLine("$it", it % 2 == 0, "n$it", "reply") }
    val rows = carRows(lines, "  ", "empty")
    assertEquals(6, rows.size)
    assertEquals("n8", rows[0].text)
    assertEquals("You", rows[0].title)
    assertEquals("kit", rows[1].title)
    assertEquals("n3", rows.last().text)
  }
}
