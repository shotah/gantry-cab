package com.gantree.cab.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DocsShotTest {
  @Test
  fun phoneThreadIsPhoneSized() {
    val img = renderPhone("thread")
    assertEquals(PHONE_W, img.width)
    assertEquals(PHONE_H, img.height)
  }

  @Test
  fun autoThreadIsLandscape() {
    val img = renderAuto("thread")
    assertEquals(AUTO_W, img.width)
    assertEquals(AUTO_H, img.height)
  }

  @Test
  fun everyNamedShotRenders() {
    for (name in DOCS_SHOT_NAMES) {
      val img = renderDocsShot(name)
      if (name.startsWith("phone-")) {
        assertEquals(PHONE_W, img.width)
        assertEquals(PHONE_H, img.height)
      } else {
        assertEquals(AUTO_W, img.width)
        assertEquals(AUTO_H, img.height)
      }
    }
  }

  @Test(expected = IllegalStateException::class)
  fun unknownShotNameFails() {
    renderDocsShot("nope")
  }

  @Test
  fun emojiPickerPaintsFacesNearComposer() {
    val img = renderDocsShot("phone-emoji")
    var painted = 0
    for (y in 620 until 760) {
      for (x in 20 until 370) {
        val rgb = img.getRGB(x, y) and 0xFFFFFF
        if (rgb != 0x0E1316 && rgb != 0x171D22 && rgb != 0x3A4550) painted += 1
      }
    }
    assertTrue(painted > 80)
  }

  @Test
  fun settingsIsACardNotAFullBleedSheet() {
    val img = renderDocsShot("phone-settings")
    val left = img.getRGB(40, 160) and 0xFFFFFF
    val card = img.getRGB(250, 68) and 0xFFFFFF
    assertEquals(0x0E1316, left)
    assertEquals(0x171D22, card)
  }

  @Test
  fun attachMenuPaintsAboveComposer() {
    val img = renderDocsShot("phone-attach")
    var panel = 0
    for (y in 600 until 760) {
      for (x in 16 until 220) {
        if (img.getRGB(x, y) and 0xFFFFFF == 0x171D22) panel += 1
      }
    }
    assertTrue(panel > 400)
  }

  @Test
  fun composerLeavesAGapBeforeSend() {
    val img = renderPhone("thread")
    val y = 800
    val field = img.getRGB(280, y) and 0xFFFFFF
    val gap = img.getRGB(306, y) and 0xFFFFFF
    val send = img.getRGB(340, y) and 0xFFFFFF
    assertEquals(0x0E1316, field)
    assertEquals(0x171D22, gap)
    assertTrue(send != 0x0E1316 && send != 0x171D22)
  }

  @Test
  fun settingsEmailSitsBelowListen() {
    val img = renderDocsShot("phone-settings")
    var listenTop = -1
    var listenBottom = -1
    for (y in 64 until 450) {
      if ((img.getRGB(140, y) and 0xFFFFFF) == 0x2A1612) {
        if (listenTop < 0) listenTop = y
        listenBottom = y
      }
    }
    assertTrue(listenTop > 0)
    var mutedInButton = 0
    for (y in listenTop..listenBottom) {
      for (x in 134 until 200) {
        if ((img.getRGB(x, y) and 0xFFFFFF) == 0x9AA3AB) mutedInButton += 1
      }
    }
    assertEquals(0, mutedInButton)
    var email = 0
    for (y in listenBottom + 8 until listenBottom + 28) {
      for (x in 130 until 250) {
        val rgb = img.getRGB(x, y) and 0xFFFFFF
        if (rgb != 0x171D22 && rgb != 0x3A4550 && rgb != 0x2A1612) email += 1
      }
    }
    assertTrue(email > 20)
  }

  @Test
  fun gpsDotPaintsOnAttachWhenOn() {
    val img = renderPhone("thread")
    var live = 0
    for (y in 805 until 840) {
      for (x in 28 until 46) {
        if ((img.getRGB(x, y) and 0xFFFFFF) == 0x3DB8A0) live += 1
      }
    }
    assertTrue(live > 8)
  }

  @Test
  fun writesNamedPngsWhenAsked() {
    if (System.getenv("CAB_WRITE_SHOTS") != "1") {
      return
    }
    val dir = docsDir()
    writeDocsShots(dir)
    for (name in DOCS_SHOT_NAMES) {
      val png = File(dir, "$name.png")
      assertTrue(png.isFile)
      assertTrue(png.length() > 1_000)
    }
  }

  private fun docsDir(): File {
    val here = File(".").canonicalFile
    val root = if (File(here, "assets").isDirectory) here else here.parentFile
    return File(root, "assets/docs")
  }
}
