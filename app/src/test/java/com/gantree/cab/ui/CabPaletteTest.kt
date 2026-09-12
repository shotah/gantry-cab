package com.gantree.cab.ui

import androidx.compose.ui.graphics.Color
import com.gantree.cab.mailbox.THEME_IDS
import org.junit.Assert.assertEquals
import org.junit.Test

class CabPaletteTest {
  @Test
  fun themesKeepTheirAccents() {
    assertEquals(Color(0xFFF07848), cabColors("boom").accent)
    assertEquals(Color(0xFFE6D3B0), cabColors("inlay").accent)
    assertEquals(Color(0xFFC5D24A), cabColors("lamp").accent)
    assertEquals(cabColors("boom").accent, cabColors("nope").accent)
    assertEquals(Color(0xFF0E1316), cabColors("boom").canvas)
    assertEquals(Color(0xFFF07848), boomColors().toColorScheme().primary)
    assertEquals(Color(0xFF3DB8A0), cabColors("boom").toColorScheme().tertiary)
    assertEquals(Color(0xFF8EB4D4), cabColors("noir").accent)
    assertEquals(Color(0xFFE07040), cabColors("ember").accent)
    assertEquals(Color(0xFF3CB8B0), cabColors("tide").accent)
    assertEquals(Color(0xFFD070C0), cabColors("bloom").accent)
    assertEquals("light", cabColors("paper").scheme)
    assertEquals(Color(0xFFC24A28), cabColors("paper").accent)
    assertEquals(Color(0xFF1E5A8C), cabColors("chalk").accent)
    assertEquals(Color(0xFF0C6E68), cabColors("foam").accent)
    assertEquals(Color(0xFFA02080), cabColors("petal").accent)
    assertEquals(Color(0xFFF0B020), cabColors("ink").accent)
    assertEquals("dark", cabColors("ink").scheme)
    assertEquals(Color(0xFFF6F1E8), cabColors("paper").toColorScheme().background)
    assertEquals(cabColors("boom").line, cabColors("boom").toColorScheme().outlineVariant)
    assertEquals(cabColors("paper").line, cabColors("paper").toColorScheme().outlineVariant)
  }

  @Test
  fun daylightCousinsStayLightAndInkStaysDark() {
    val light = setOf("paper", "chalk", "foam", "petal")
    assertEquals(THEME_IDS, listOf(
      "boom", "inlay", "lamp", "noir", "ember", "tide", "bloom",
      "paper", "chalk", "foam", "petal", "ink",
    ))
    for (id in THEME_IDS) {
      val colors = cabColors(id)
      assertEquals(id, if (id in light) "light" else "dark", colors.scheme)
    }
    assertEquals(Color(0xFFF6F1E8), cabColors("paper").canvas)
    assertEquals(Color(0xFFE4C4B0), cabColors("paper").you)
    assertEquals(Color(0xFFF2F5F8), cabColors("chalk").canvas)
    assertEquals(Color(0xFFEEF6F5), cabColors("foam").canvas)
    assertEquals(Color(0xFFF7F1F6), cabColors("petal").canvas)
    assertEquals(Color(0xFF050506), cabColors("ink").canvas)
    assertEquals(Color(0xFF3A2410), cabColors("ink").you)
    assertEquals(Color(0xFFC4C4CA), cabColors("ink").dim)
  }
}
