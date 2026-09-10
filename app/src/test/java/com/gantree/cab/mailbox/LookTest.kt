package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Test

class LookTest {
  @Test
  fun themeFallsBackToBoom() {
    assertEquals(DEFAULT_THEME, parseTheme(null))
    assertEquals("boom", parseTheme("nope"))
    assertEquals("inlay", parseTheme("inlay"))
    assertEquals("lamp", parseTheme("lamp"))
    assertEquals(listOf("boom", "inlay", "lamp"), THEME_IDS)
  }

  @Test
  fun fontFallsBackToSmall() {
    assertEquals(DEFAULT_FONT, parseFont(null))
    assertEquals("sm", parseFont("nope"))
    assertEquals("lg", parseFont("lg"))
    assertEquals(listOf("sm", "md", "lg", "xl"), FONT_IDS)
    assertEquals(14f, chatSp("sm"), 0.01f)
    assertEquals(16f, chatSp("md"), 0.01f)
    assertEquals(20f, chatSp("lg"), 0.01f)
    assertEquals(24f, chatSp("xl"), 0.01f)
    assertEquals(14f, chatSp("nope"), 0.01f)
  }

  @Test
  fun labelsMatchPendant() {
    assertEquals("Boom", themeLabel("boom"))
    assertEquals("Boom", themeLabel("nope"))
    assertEquals("Inlay", themeLabel("inlay"))
    assertEquals("Small", fontLabel("sm"))
    assertEquals("Medium", fontLabel("md"))
    assertEquals("Large", fontLabel("lg"))
    assertEquals("Extra large", fontLabel("xl"))
  }
}
