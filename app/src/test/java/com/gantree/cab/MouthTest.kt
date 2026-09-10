package com.gantree.cab

import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.WireFrame
import org.junit.Assert.assertEquals
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
  }

  @Test
  fun silentKindsStayOffTheThread() {
    val mouth = Mouth()
    for (kind in listOf("ack", "typing", "draft", "allow", "pin")) {
      mouth.ingest(WireFrame(kind = kind, text = "nope"))
    }
    assertTrue(mouth.lines.value.isEmpty())
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
}
