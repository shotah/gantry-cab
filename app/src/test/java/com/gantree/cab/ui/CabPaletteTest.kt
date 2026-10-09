package com.gantree.cab.ui

import androidx.compose.ui.graphics.Color
import com.gantree.cab.mailbox.THEME_IDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CabPaletteTest {
  @Test
  fun boomPaperAndInkStayPutAndJunkFallsToBoom() {
    assertEquals(Color(0xFFF07848), cabColors("boom").accent)
    assertEquals(Color(0xFF0E1316), cabColors("boom").canvas)
    assertEquals(Color(0xFFF07848), boomColors().toColorScheme().primary)
    assertEquals(Color(0xFF3DB8A0), cabColors("boom").toColorScheme().tertiary)
    assertEquals(cabColors("boom").accent, cabColors("nope").accent)
    assertEquals(cabColors("boom").accent, cabColors("noir").accent)
    assertEquals(cabColors("boom").canvas, cabColors("lamp").canvas)
    assertEquals("light", cabColors("paper").scheme)
    assertEquals(Color(0xFFC24A28), cabColors("paper").accent)
    assertEquals(Color(0xFFF6F1E8), cabColors("paper").canvas)
    assertEquals(Color(0xFFE4C4B0), cabColors("paper").you)
    assertEquals(Color(0xFFF6F1E8), cabColors("paper").toColorScheme().background)
    assertEquals(Color(0xFFF0B020), cabColors("ink").accent)
    assertEquals("dark", cabColors("ink").scheme)
    assertEquals(Color(0xFF050506), cabColors("ink").canvas)
    assertEquals(Color(0xFF3A2410), cabColors("ink").you)
    assertEquals(Color(0xFFC4C4CA), cabColors("ink").dim)
    assertEquals(cabColors("boom").line, cabColors("boom").toColorScheme().outlineVariant)
    assertEquals(cabColors("paper").line, cabColors("paper").toColorScheme().outlineVariant)
  }

  @Test
  fun schemesFollowTheCatalogAndKitMatchesTrack() {
    val light = setOf("paper", "lemonade", "fizz", "mist", "grit", "flare", "flicker")
    assertEquals(
      listOf(
        "boom", "paper", "ink",
        "marquee", "lemonade", "neon", "fizz", "rain", "mist",
        "fuse", "grit", "siren", "flare", "static", "flicker",
      ),
      THEME_IDS,
    )
    for (id in THEME_IDS) {
      val colors = cabColors(id)
      assertEquals(id, if (id in light) "light" else "dark", colors.scheme)
      assertEquals(id, colors.track, colors.kit)
    }
    for (id in listOf("siren", "flare", "fuse", "grit")) {
      assertNotEquals(id, cabColors(id).accent, cabColors(id).danger)
    }
  }

  @Test
  fun moodHexesMatchTheCatalog() {
    assertSwatch("marquee", Swatch(0xFF141A3C, 0xFF1C2450, 0xFF283064, 0xFF46508C, 0xFF7A84B8, 0xFFFFF8E6, 0xFFE6E0D0, 0xFFB0B4D8, 0xFF9AA0C8, 0xFFFFCC33, 0xFFFFE599, 0xFFC99A10, 0xFF332A12, 0xFFFF6B9D, 0xFF3AD0A0, 0xFF2A2470))
    assertSwatch("lemonade", Swatch(0xFFFFF6CC, 0xFFFFF0B0, 0xFFF7E690, 0xFFA89440, 0xFF6E6020, 0xFF1A1606, 0xFF2E2810, 0xFF5A5020, 0xFF665C28, 0xFF1F52E0, 0xFF10308C, 0xFF1842B8, 0xFFDDE6FF, 0xFFC0184C, 0xFF167A4A, 0xFFFFD84D, light = true))
    assertSwatch("neon", Swatch(0xFF120A1E, 0xFF1B1030, 0xFF281848, 0xFF4A2E7A, 0xFF7E58B8, 0xFFFDF2FF, 0xFFE6D8F2, 0xFFB89AD8, 0xFFA088C4, 0xFFFF2D95, 0xFFFFA6D2, 0xFFC0106A, 0xFF3A1030, 0xFFFF5A5A, 0xFF2EF2B0, 0xFF3A1458))
    assertSwatch("fizz", Swatch(0xFFE6FBFF, 0xFFD2F4FB, 0xFFBCEAF4, 0xFF4E8A98, 0xFF2E5C68, 0xFF081A20, 0xFF142A32, 0xFF2E5260, 0xFF3A5E6C, 0xFFE0107A, 0xFF8E0848, 0xFFC00C66, 0xFFFFD6EA, 0xFFC4123A, 0xFF0E7A5A, 0xFFB0EEFC, light = true))
    assertSwatch("rain", Swatch(0xFF0F131F, 0xFF161C2C, 0xFF20283C, 0xFF364260, 0xFF5C6A90, 0xFFE8ECF8, 0xFFC8D0E4, 0xFF8E9AC0, 0xFF8894BA, 0xFF8C9FE6, 0xFFC8D4FF, 0xFF4E60A8, 0xFF1A2040, 0xFFD06A90, 0xFF5CB09A, 0xFF1E2644))
    assertSwatch("mist", Swatch(0xFFECEEF6, 0xFFE0E3EE, 0xFFD0D4E4, 0xFF7E86A4, 0xFF505870, 0xFF14161E, 0xFF22262E, 0xFF464C62, 0xFF4C526A, 0xFF4A56A8, 0xFF2A3270, 0xFF3C4690, 0xFFD8DCF6, 0xFFB02858, 0xFF1E7462, 0xFFC8CCEC, light = true))
    assertSwatch("fuse", Swatch(0xFF17150F, 0xFF201D14, 0xFF2C281C, 0xFF4E4830, 0xFF7C7450, 0xFFFBF4E6, 0xFFE2D8C4, 0xFFAEA48A, 0xFFA0967E, 0xFFFF7A00, 0xFFFFBF80, 0xFFC45A00, 0xFF33200A, 0xFFFF5A6E, 0xFF86C46A, 0xFF332A16))
    assertSwatch("grit", Swatch(0xFFF3EFE4, 0xFFE9E3D2, 0xFFDCD4BC, 0xFF8A8060, 0xFF5A5238, 0xFF1A1810, 0xFF2C2818, 0xFF504A30, 0xFF5A543A, 0xFFD2500A, 0xFF8A3004, 0xFFB44208, 0xFFFFDCC4, 0xFFB4203A, 0xFF4A7A1E, 0xFFEAD29A, light = true))
    assertSwatch("siren", Swatch(0xFF160608, 0xFF200A0E, 0xFF2E1016, 0xFF58202A, 0xFF8E3A48, 0xFFFFF2F2, 0xFFECD4D6, 0xFFC09AA0, 0xFFAE8A90, 0xFFFF2E3F, 0xFFFFA0A8, 0xFFC0101E, 0xFF3E0C12, 0xFFFF6AB8, 0xFF46D08A, 0xFF3A0E18))
    assertSwatch("flare", Swatch(0xFFFFF0EE, 0xFFFDE0DC, 0xFFF6CCC6, 0xFFA06860, 0xFF6A4038, 0xFF1E0A0A, 0xFF301616, 0xFF5A3030, 0xFF663A3A, 0xFFD4102C, 0xFF880818, 0xFFB00C22, 0xFFFFD4D4, 0xFFB0147A, 0xFF1A7A4E, 0xFFFFC2BC, light = true))
    assertSwatch("static", Swatch(0xFF0D1410, 0xFF141C17, 0xFF1E2A22, 0xFF37493D, 0xFF5E7866, 0xFFF0F8F2, 0xFFD2DCD6, 0xFF9AB0A2, 0xFF8AA092, 0xFFB388FF, 0xFFDCC8FF, 0xFF7A4EE0, 0xFF221A38, 0xFFFF6A8A, 0xFF52D490, 0xFF26203C))
    assertSwatch("flicker", Swatch(0xFFEEF7F0, 0xFFDFF0E4, 0xFFCCE4D4, 0xFF6A8E78, 0xFF40604C, 0xFF0E1A12, 0xFF1A2A20, 0xFF365244, 0xFF425E50, 0xFF6A2FD0, 0xFF3E1484, 0xFF5824B0, 0xFFE8DCFF, 0xFFB4204E, 0xFF1A7A4A, 0xFFD8D0F8, light = true))
  }
}

/** Hexes Cab paints, in pendant catalog order. `kit` is `track` and is not repeated. */
private data class Swatch(
  val canvas: Long,
  val panel: Long,
  val track: Long,
  val line: Long,
  val edge: Long,
  val fg: Long,
  val body: Long,
  val muted: Long,
  val dim: Long,
  val accent: Long,
  val mark: Long,
  val accentLine: Long,
  val accentSoft: Long,
  val danger: Long,
  val ok: Long,
  val you: Long,
  val light: Boolean = false,
)

private fun assertSwatch(id: String, s: Swatch) {
  val c = cabColors(id)
  fun hex(v: Long) = Color(v)
  assertEquals("$id canvas", hex(s.canvas), c.canvas)
  assertEquals("$id panel", hex(s.panel), c.panel)
  assertEquals("$id track", hex(s.track), c.track)
  assertEquals("$id line", hex(s.line), c.line)
  assertEquals("$id edge", hex(s.edge), c.edge)
  assertEquals("$id fg", hex(s.fg), c.fg)
  assertEquals("$id body", hex(s.body), c.body)
  assertEquals("$id muted", hex(s.muted), c.muted)
  assertEquals("$id dim", hex(s.dim), c.dim)
  assertEquals("$id accent", hex(s.accent), c.accent)
  assertEquals("$id mark", hex(s.mark), c.mark)
  assertEquals("$id accentLine", hex(s.accentLine), c.accentLine)
  assertEquals("$id accentSoft", hex(s.accentSoft), c.accentSoft)
  assertEquals("$id danger", hex(s.danger), c.danger)
  assertEquals("$id ok", hex(s.ok), c.ok)
  assertEquals("$id you", hex(s.you), c.you)
  assertEquals("$id kit", c.track, c.kit)
  assertEquals("$id scheme", if (s.light) "light" else "dark", c.scheme)
}
