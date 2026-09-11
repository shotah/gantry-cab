package com.gantree.cab.ui

import com.gantree.cab.ChatLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatScrollTest {
  @Test
  fun newestTurnIsFirstForReverseLayout() {
    val lines = listOf(
      ChatLine("1", true, "a", "inbound"),
      ChatLine("2", false, "b", "reply"),
    )
    assertEquals(listOf("2", "1"), threadNewestFirst(lines).map { it.id })
    assertEquals(emptyList<ChatLine>(), threadNewestFirst(emptyList()))
  }

  @Test
  fun pinnedToNewestIsIndexZeroWithinSlop() {
    assertTrue(pinnedToNewest(0, 0))
    assertTrue(pinnedToNewest(0, 80))
    assertFalse(pinnedToNewest(0, 81))
    assertFalse(pinnedToNewest(1, 0))
    assertTrue(pinnedToNewest(0, 40, slopPx = 40))
    assertFalse(pinnedToNewest(0, 41, slopPx = 40))
  }

  @Test
  fun followStaysOnNewestUnlessScrolledUp() {
    assertTrue(shouldFollowNewest(pinnedToNewest = true, newestFromYou = false))
    assertTrue(shouldFollowNewest(pinnedToNewest = false, newestFromYou = true))
    assertFalse(shouldFollowNewest(pinnedToNewest = false, newestFromYou = false))
  }
}
