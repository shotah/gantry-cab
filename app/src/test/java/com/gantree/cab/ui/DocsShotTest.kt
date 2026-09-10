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
  fun emojiPickerPaintsFaces() {
    val img = renderDocsShot("phone-emoji")
    var yellow = 0
    for (y in 450 until 560) {
      for (x in 20 until 370) {
        if (img.getRGB(x, y) and 0xFFFFFF == 0xF5C542) yellow += 1
      }
    }
    assertTrue(yellow > 80)
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
