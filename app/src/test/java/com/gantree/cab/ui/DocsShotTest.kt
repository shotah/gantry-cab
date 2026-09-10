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
    for (y in 640 until 770) {
      for (x in 20 until 370) {
        val rgb = img.getRGB(x, y) and 0xFFFFFF
        if (rgb != 0x0E1316 && rgb != 0x171D22 && rgb != 0x232B32) painted += 1
      }
    }
    assertTrue(painted > 80)
  }

  @Test
  fun emojiPanelStaysNearComposer() {
    val img = renderDocsShot("phone-emoji")
    assertEquals(0x0E1316, img.getRGB(200, 500) and 0xFFFFFF)
  }

  @Test
  fun settingsIsAFullScreen() {
    val img = renderDocsShot("phone-settings")
    assertEquals(0x171D22, img.getRGB(200, 32) and 0xFFFFFF)
    assertEquals(0x171D22, img.getRGB(200, 500) and 0xFFFFFF)
    assertEquals(0x171D22, img.getRGB(200, 790) and 0xFFFFFF)
  }

  @Test
  fun attachMenuPaintsAboveComposer() {
    val img = renderDocsShot("phone-attach")
    var panel = 0
    for (y in 600 until 760) {
      for (x in 56 until 250) {
        if (img.getRGB(x, y) and 0xFFFFFF == 0x171D22) panel += 1
      }
    }
    assertTrue(panel > 400)
    assertEquals(0x0E1316, img.getRGB(300, 500) and 0xFFFFFF)
  }

  @Test
  fun composerLeavesAGapBeforeSend() {
    val img = renderPhone("thread")
    val y = 790
    val field = img.getRGB(200, y) and 0xFFFFFF
    val gap = img.getRGB(324, y) and 0xFFFFFF
    val send = img.getRGB(354, y) and 0xFFFFFF
    assertEquals(0x232B32, field)
    assertEquals(0x171D22, gap)
    assertTrue(send != 0x232B32 && send != 0x171D22)
  }

  @Test
  fun composerFieldUsesMostOfTheBar() {
    val img = renderPhone("thread")
    val y = 790
    assertEquals(0x232B32, img.getRGB(80, y) and 0xFFFFFF)
    assertEquals(0x232B32, img.getRGB(280, y) and 0xFFFFFF)
  }

  @Test
  fun settingsEmailSitsBelowConnect() {
    val img = renderDocsShot("phone-settings")
    var connectTop = -1
    var connectBottom = -1
    for (y in 400 until 820) {
      if ((img.getRGB(40, y) and 0xFFFFFF) == 0x2A1612) {
        if (connectTop < 0) connectTop = y
        connectBottom = y
      }
    }
    assertTrue(connectTop > 0)
    var mutedInButton = 0
    for (y in connectTop..connectBottom) {
      for (x in 24 until 120) {
        if ((img.getRGB(x, y) and 0xFFFFFF) == 0x9AA3AB) mutedInButton += 1
      }
    }
    assertEquals(0, mutedInButton)
    var email = 0
    for (y in connectBottom + 8 until (connectBottom + 40).coerceAtMost(PHONE_H)) {
      for (x in 24 until 250) {
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
    for (y in 770 until 830) {
      for (x in 24 until 60) {
        if ((img.getRGB(x, y) and 0xFFFFFF) == 0x3DB8A0) live += 1
      }
    }
    assertTrue(live > 8)
  }

  @Test
  fun vectorDrawablesParseToShapes() {
    for (name in listOf(VEC_ATTACH, VEC_SMILE, VEC_PHOTO, VEC_CODE, VEC_PIN)) {
      val shapes = vectorShapes(name)
      assertTrue(name, shapes.isNotEmpty())
      assertTrue(name, shapes.any { it.bounds2D.width > 4 && it.bounds2D.height > 4 })
    }
  }

  @Test
  fun streamShotDiffersFromThread() {
    val thread = renderPhone("thread")
    val stream = renderPhone("stream")
    var diffs = 0
    var y = 0
    while (y < PHONE_H) {
      var x = 0
      while (x < PHONE_W) {
        if (thread.getRGB(x, y) != stream.getRGB(x, y)) diffs += 1
        x += 4
      }
      y += 4
    }
    assertTrue(diffs > 20)
  }

  @Test
  fun photoShotPaintsHatchTape() {
    val img = renderDocsShot("phone-photo")
    var tape = 0
    for (y in 80 until 420) {
      for (x in 40 until 370) {
        val rgb = img.getRGB(x, y) and 0xFFFFFF
        if (rgb == 0xF3B199 || rgb == 0xF5C542) tape += 1
      }
    }
    assertTrue(tape > 40)
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
