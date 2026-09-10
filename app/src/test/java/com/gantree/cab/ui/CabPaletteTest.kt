package com.gantree.cab.ui

import androidx.compose.ui.graphics.Color
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
  }
}
