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
  fun emptyThreadGetsKitsStarterCardSoReplyExists() {
    val turns = carMessages(emptyList(), "kit", 1_000L)
    assertEquals(listOf(carStarter("kit", 1_000L)), turns)
    val starter = turns.single()
    assertEquals(false, starter.fromYou)
    assertEquals(true, starter.read)
    assertEquals(1_000L, starter.at)
    assertEquals("Nothing said yet. Tap Reply and talk to Kit.", starter.text)
    // Drafts alone are not a thread either.
    assertEquals(turns.map { it.text }, carMessages(listOf(ChatLine("__draft__", false, "⏳", "draft")), "kit", 1_000L).map { it.text })
  }

  @Test
  fun aRealTurnReplacesTheStarter() {
    val turns = carMessages(listOf(ChatLine("1", true, "hello", "inbound", at = 5L)), "kit", 1_000L)
    assertEquals(listOf(CarTurn(fromYou = true, text = "hello", at = 5L)), turns)
    assertEquals(true, turns.single().read)
    val reply = carMessages(listOf(ChatLine("2", false, "hi", "reply", at = 6L)), "kit", 1_000L).single()
    assertEquals(false, reply.read)
  }

  @Test
  fun draftTokensDoNotChangeCarTurns() {
    val base = listOf(ChatLine("1", true, "hello", "inbound", at = 1L))
    val a = carTurns(base + ChatLine("__draft__", false, "Gate", "draft", at = 2L))
    val b = carTurns(base + ChatLine("__draft__", false, "Gate's on the latch until 21:00", "draft", at = 3L))
    assertEquals(a, b)
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
