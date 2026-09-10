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
