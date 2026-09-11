package com.gantree.cab

import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.WireFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MouthTest {
  @Test
  fun faceFrameUpdatesRevAndSkipsTheThread() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "face", text = "7"))
    assertEquals(7, mouth.avatarRev.value)
    assertTrue(mouth.lines.value.isEmpty())
  }

  @Test
  fun cmdsReplaceTheCatalog() {
    val mouth = Mouth()
    val cmds = listOf(SlashCommand("new", "reset this session"))
    mouth.ingest(WireFrame(kind = "cmds", commands = cmds))
    assertEquals(cmds, mouth.catalog.value)
    mouth.replace(listOf(ChatLine("1", true, "hi", "inbound")), true, "ok")
    assertEquals(emptyList<SlashCommand>(), mouth.catalog.value)
    assertEquals(true, mouth.up.value)
    assertEquals(0L, mouth.typingUntil.value)
  }

  @Test
  fun silentKindsStayOffTheThread() {
    val mouth = Mouth()
    for (kind in listOf("ack", "allow", "pin")) {
      mouth.ingest(WireFrame(kind = kind, text = "nope"))
    }
    assertTrue(mouth.lines.value.isEmpty())
    assertEquals(0L, mouth.typingUntil.value)
  }

  @Test
  fun mailboxErrorIsAHintNotABubble() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "error", text = "bad frame"))
    assertTrue(mouth.lines.value.isEmpty())
    assertEquals("bad frame", mouth.hint.value)
  }

  @Test
  fun emptyTextDropsUnlessItIsAPingOrPhoto() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "reply", text = "  "))
    assertTrue(mouth.lines.value.isEmpty())
    mouth.ingest(WireFrame(kind = "push"))
    assertEquals("(ping)", mouth.lines.value.single().text)
    mouth.ingest(WireFrame(kind = "reply", id = "p", images = listOf("data:image/jpeg;base64,aa")))
    assertEquals("", mouth.lines.value.last().text)
    assertEquals("data:image/jpeg;base64,aa", mouth.lines.value.last().photo)
  }

  @Test
  fun inboundIsFromYouAndAddCapsAtEighty() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "inbound", id = "1", text = "hi"))
    assertEquals(true, mouth.lines.value.single().fromYou)
    mouth.setUp(true)
    mouth.setHint("live")
    mouth.setFaceHint("bad jpeg")
    mouth.setAvatarRev(2)
    assertEquals("live", mouth.hint.value)
    assertEquals("bad jpeg", mouth.faceHint.value)
    for (i in 0 until 90) {
      mouth.add(ChatLine("$i", false, "n$i", "reply"))
    }
    assertEquals(80, mouth.lines.value.size)
    assertEquals("n10", mouth.lines.value.first().text)
  }

  @Test
  fun transcriptReplayPaintsAndDedupesById() {
    val mouth = Mouth()
    assertTrue(
      mouth.ingest(WireFrame(kind = "inbound", id = "a", text = "hatch", seq = 1, at = 10, replay = true)),
    )
    assertTrue(
      mouth.ingest(WireFrame(kind = "reply", id = "b", text = "latched", seq = 2, at = 20, replay = true)),
    )
    assertFalse(
      mouth.ingest(WireFrame(kind = "reply", id = "b", text = "latched", seq = 2, at = 20, replay = true)),
    )
    assertEquals(listOf("hatch", "latched"), mouth.lines.value.map { it.text })
  }

  @Test
  fun draftReplacesInPlaceAndBlankClears() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "draft", text = "Gate"))
    mouth.ingest(WireFrame(kind = "draft", text = "Gate's on the latch"))
    assertEquals(1, mouth.lines.value.size)
    val draft = mouth.lines.value.single()
    assertEquals(DRAFT_ID, draft.id)
    assertEquals(false, draft.fromYou)
    assertEquals("draft", draft.kind)
    assertEquals("Gate's on the latch", draft.text)
    mouth.ingest(WireFrame(kind = "draft", text = "  "))
    assertTrue(mouth.lines.value.isEmpty())
  }

  @Test
  fun replyClearsTheDraftBubble() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "draft", text = "⏳…"))
    mouth.ingest(WireFrame(kind = "reply", id = "r1", text = "Gate's on the latch until 21:00."))
    assertEquals(1, mouth.lines.value.size)
    assertEquals("r1", mouth.lines.value.single().id)
    assertEquals("reply", mouth.lines.value.single().kind)
  }

  @Test
  fun emptyReplyStillDropsTheDraft() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "draft", text = "⏳…"))
    mouth.ingest(WireFrame(kind = "reply", text = "  "))
    assertTrue(mouth.lines.value.isEmpty())
  }

  @Test
  fun socketDownDropsDraftAndTyping() {
    var t = 1_000L
    val mouth = Mouth { t }
    mouth.setUp(true)
    mouth.ingest(WireFrame(kind = "draft", text = "⏳…"))
    mouth.ingest(WireFrame(kind = "typing"))
    assertEquals(1, mouth.lines.value.size)
    assertEquals(t + TYPING_TTL_MS, mouth.typingUntil.value)
    mouth.setUp(false)
    assertTrue(mouth.lines.value.isEmpty())
    assertEquals(0L, mouth.typingUntil.value)
    assertFalse(mouth.up.value)
  }

  @Test
  fun draftStaysLastPastTheEightyCap() {
    val mouth = Mouth()
    for (i in 0 until 80) {
      mouth.add(ChatLine("$i", false, "n$i", "reply"))
    }
    mouth.ingest(WireFrame(kind = "draft", text = "live"))
    assertEquals(80, mouth.lines.value.size)
    assertEquals(DRAFT_ID, mouth.lines.value.last().id)
    assertEquals("live", mouth.lines.value.last().text)
    assertEquals("n1", mouth.lines.value.first().text)
  }

  @Test
  fun typingTtlAndClearingKinds() {
    var t = 10_000L
    val mouth = Mouth { t }
    mouth.ingest(WireFrame(kind = "typing"))
    assertEquals(16_000L, mouth.typingUntil.value)
    t = 12_000L
    mouth.ingest(WireFrame(kind = "typing"))
    assertEquals(18_000L, mouth.typingUntil.value)
    mouth.ingest(WireFrame(kind = "ack", id = "a1"))
    assertEquals(18_000L, mouth.typingUntil.value)
    mouth.ingest(WireFrame(kind = "reply", id = "r1", text = "done"))
    assertEquals(0L, mouth.typingUntil.value)
    mouth.ingest(WireFrame(kind = "typing"))
    assertEquals(18_000L, mouth.typingUntil.value)
    mouth.ingest(WireFrame(kind = "push", text = "20:40"))
    assertEquals(0L, mouth.typingUntil.value)
    mouth.ingest(WireFrame(kind = "typing"))
    mouth.ingest(WireFrame(kind = "error", text = "rate"))
    assertEquals(0L, mouth.typingUntil.value)
    assertTrue(clearsTyping("reply"))
    assertTrue(clearsTyping("push"))
    assertTrue(clearsTyping("error"))
    assertFalse(clearsTyping("ack"))
    assertFalse(clearsTyping("draft"))
  }

  @Test
  fun pushClearsTypingButLeavesTheDraft() {
    val mouth = Mouth { 1L }
    mouth.ingest(WireFrame(kind = "draft", text = "⏳…"))
    mouth.ingest(WireFrame(kind = "typing"))
    mouth.ingest(WireFrame(kind = "push", text = "still on the dock?"))
    assertEquals(0L, mouth.typingUntil.value)
    assertEquals(DRAFT_ID, mouth.lines.value.last().id)
    assertEquals("push", mouth.lines.value.first().kind)
  }

  @Test
  fun ackClearsPendingOnTheMatchingBubble() {
    val mouth = Mouth()
    mouth.add(ChatLine("a1", true, "hi", "inbound", pending = true))
    mouth.ingest(WireFrame(kind = "ack", id = "nope"))
    assertTrue(mouth.lines.value.single().pending)
    mouth.ingest(WireFrame(kind = "ack", id = "a1"))
    assertFalse(mouth.lines.value.single().pending)
  }

  @Test
  fun catchUpPaintsBySeqAndKeepsADraftLast() {
    val mouth = Mouth()
    mouth.ingest(WireFrame(kind = "reply", id = "b", text = "second", seq = 2, at = 20L))
    mouth.ingest(WireFrame(kind = "reply", id = "a", text = "first", seq = 1, at = 10L))
    assertEquals(listOf("a", "b"), mouth.lines.value.map { it.id })
    mouth.ingest(WireFrame(kind = "draft", text = "⏳ spinning up"))
    mouth.ingest(
      WireFrame(kind = "inbound", id = "late", text = "I already sent this", seq = 3, at = 15L),
    )
    assertEquals(listOf("a", "b", "late", DRAFT_ID), mouth.lines.value.map { it.id })
    val restamp = mouth.ingest(WireFrame(kind = "reply", id = "b", text = "second", seq = 2, at = 20L))
    assertEquals(false, restamp)
    assertEquals(4, mouth.lines.value.size)
  }

  @Test
  fun echoRestampKeepsPendingUntilAck() {
    val mouth = Mouth()
    mouth.add(ChatLine("a1", true, "hi", "inbound", pending = true, at = 50L))
    assertEquals(false, mouth.ingest(WireFrame(kind = "inbound", id = "a1", text = "hi", seq = 4, at = 40L)))
    val line = mouth.lines.value.single()
    assertEquals(4, line.seq)
    assertEquals(40L, line.at)
    assertEquals(true, line.pending)
    mouth.ingest(WireFrame(kind = "ack", id = "a1"))
    assertEquals(false, mouth.lines.value.single().pending)
  }
}
