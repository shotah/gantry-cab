package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private data class Bubble(
  override val id: String,
  override val at: Long,
  override val seq: Int? = null,
  override val kind: String? = null,
) : ThreadOrder

class ThreadTest {
  @Test
  fun capThreadKeepsTheNewest() {
    assertEquals(listOf(2, 3), capThread(listOf(1, 2, 3), 2))
    assertEquals(listOf(1, 2), capThread(listOf(1, 2), 5))
  }

  @Test
  fun rememberSeenDropsOldestIdsFirst() {
    val seen = LinkedHashSet(listOf("a", "b", "c"))
    rememberSeen(seen, "d", 3)
    assertEquals(listOf("b", "c", "d"), seen.toList())
  }

  @Test
  fun placeInThreadInsertsCatchUpBySeqAndKeepsADraftLast() {
    val late = Bubble(id = "b", at = 20, seq = 2)
    val early = Bubble(id = "a", at = 10, seq = 1)
    val draft = Bubble(id = "__draft__", at = 1, kind = "draft")
    val ordered = placeInThread(placeInThread(listOf(late), early), draft)
    assertEquals(listOf("a", "b", "__draft__"), ordered.map { it.id })
    val pending = placeInThread(
      listOf(Bubble(id = "mine", at = 50)),
      Bubble(id = "missed", at = 40, seq = 4),
    )
    assertEquals(listOf("missed", "mine"), pending.map { it.id })
  }

  @Test
  fun onlyQueuedTurnsMoveTheCursor() {
    assertEquals(true, movesCursor("reply"))
    assertEquals(true, movesCursor("inbound"))
    assertEquals(true, movesCursor("push"))
    assertEquals(true, movesCursor(null))
    assertEquals(false, movesCursor("ack"))
    assertEquals(false, movesCursor("error"))
    assertEquals(false, movesCursor("face"))
    assertEquals(false, movesCursor("backdrop"))
    assertEquals(false, movesCursor("theme"))
  }

  @Test
  fun ackSinceUsesTheHighestSeqNotLastArrival() {
    var cur = advanceCursor(ThreadCursor(seq = 0), id = "b", seq = 2)
    cur = advanceCursor(cur, id = "a", seq = 1)
    assertEquals("2", ackSince(cur))
    assertEquals("legacy", ackSince(ThreadCursor(id = "legacy", seq = 0)))
    assertNull(ackSince(ThreadCursor()))
  }
}
