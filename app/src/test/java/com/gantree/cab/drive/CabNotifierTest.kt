package com.gantree.cab.drive

import org.junit.Assert.assertEquals
import org.junit.Test

class CabNotifierTest {
  @Test
  fun conversationIdIsStableForTheSlug() {
    assertEquals("cab-kit", CabNotifier.conversationId("kit"))
  }

  @Test
  fun kitHistoryKeepsLastTurnsForTheSameSlug() {
    val first = pushKitTurn(emptyList(), "", "kit", "hi", 1L, cap = 3)
    assertEquals("kit", first.first)
    val second = pushKitTurn(first.second, first.first, "kit", "there", 2L, cap = 3)
    val third = pushKitTurn(second.second, second.first, "kit", "again", 3L, cap = 3)
    val fourth = pushKitTurn(third.second, second.first, "kit", "overflow", 4L, cap = 3)
    assertEquals(listOf("there", "again", "overflow"), fourth.second.map { it.text })
    val other = pushKitTurn(fourth.second, "kit", "dock", "reset", 5L, cap = 3)
    assertEquals(listOf("reset"), other.second.map { it.text })
  }
}
