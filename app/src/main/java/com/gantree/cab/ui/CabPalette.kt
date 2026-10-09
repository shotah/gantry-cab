package com.gantree.cab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.gantree.cab.mailbox.chatSp
import com.gantree.cab.mailbox.parseFont
import com.gantree.cab.mailbox.parseTheme

data class CabColors(
  val canvas: Color,
  val panel: Color,
  val track: Color,
  val line: Color,
  val edge: Color,
  val fg: Color,
  val body: Color,
  val muted: Color,
  val dim: Color,
  val accent: Color,
  val mark: Color,
  val accentLine: Color,
  val accentSoft: Color,
  val danger: Color,
  val ok: Color,
  val you: Color,
  val kit: Color,
  val scheme: String = "dark",
)

val LocalCabColors = compositionLocalOf { boomColors() }

fun cabColors(themeId: String): CabColors = when (parseTheme(themeId)) {
  "paper" -> paperColors()
  "ink" -> inkColors()
  "marquee" -> marqueeColors()
  "lemonade" -> lemonadeColors()
  "neon" -> neonColors()
  "fizz" -> fizzColors()
  "rain" -> rainColors()
  "mist" -> mistColors()
  "fuse" -> fuseColors()
  "grit" -> gritColors()
  "siren" -> sirenColors()
  "flare" -> flareColors()
  "static" -> staticColors()
  "flicker" -> flickerColors()
  else -> boomColors()
}

fun boomColors() = CabColors(
  canvas = Color(0xFF0E1316),
  panel = Color(0xFF171D22),
  track = Color(0xFF232B32),
  line = Color(0xFF3A4550),
  edge = Color(0xFF5C6772),
  fg = Color(0xFFF4F0EA),
  body = Color(0xFFDCD6CE),
  muted = Color(0xFF9AA3AB),
  dim = Color(0xFF84909A),
  accent = Color(0xFFF07848),
  mark = Color(0xFFF3B199),
  accentLine = Color(0xFFC24A28),
  accentSoft = Color(0xFF2A1612),
  danger = Color(0xFFE070A0),
  ok = Color(0xFF3DB8A0),
  you = Color(0xFF3A1E16),
  kit = Color(0xFF232B32),
)

private fun paperColors() = CabColors(
  canvas = Color(0xFFF6F1E8),
  panel = Color(0xFFEFE8DC),
  track = Color(0xFFE4DCCF),
  line = Color(0xFF8E8270),
  edge = Color(0xFF5A5248),
  fg = Color(0xFF1C1814),
  body = Color(0xFF2E2822),
  muted = Color(0xFF524A42),
  dim = Color(0xFF5C544C),
  accent = Color(0xFFC24A28),
  mark = Color(0xFF8A2808),
  accentLine = Color(0xFFA83818),
  accentSoft = Color(0xFFF3D8CC),
  danger = Color(0xFFB42858),
  ok = Color(0xFF1A7A64),
  you = Color(0xFFE4C4B0),
  kit = Color(0xFFE4DCCF),
  scheme = "light",
)

private fun inkColors() = CabColors(
  canvas = Color(0xFF050506),
  panel = Color(0xFF141416),
  track = Color(0xFF262628),
  line = Color(0xFF6A6A70),
  edge = Color(0xFF9A9AA0),
  fg = Color(0xFFFAFAFA),
  body = Color(0xFFE4E4E6),
  muted = Color(0xFFB0B0B6),
  dim = Color(0xFFC4C4CA),
  accent = Color(0xFFF0B020),
  mark = Color(0xFFFFE08A),
  accentLine = Color(0xFFC88810),
  accentSoft = Color(0xFF2A220C),
  danger = Color(0xFFF07090),
  ok = Color(0xFF3CC8A8),
  you = Color(0xFF3A2410),
  kit = Color(0xFF262628),
)

private fun marqueeColors() = CabColors(
  canvas = Color(0xFF141A3C),
  panel = Color(0xFF1C2450),
  track = Color(0xFF283064),
  line = Color(0xFF46508C),
  edge = Color(0xFF7A84B8),
  fg = Color(0xFFFFF8E6),
  body = Color(0xFFE6E0D0),
  muted = Color(0xFFB0B4D8),
  dim = Color(0xFF9AA0C8),
  accent = Color(0xFFFFCC33),
  mark = Color(0xFFFFE599),
  accentLine = Color(0xFFC99A10),
  accentSoft = Color(0xFF332A12),
  danger = Color(0xFFFF6B9D),
  ok = Color(0xFF3AD0A0),
  you = Color(0xFF2A2470),
  kit = Color(0xFF283064),
)

private fun lemonadeColors() = CabColors(
  canvas = Color(0xFFFFF6CC),
  panel = Color(0xFFFFF0B0),
  track = Color(0xFFF7E690),
  line = Color(0xFFA89440),
  edge = Color(0xFF6E6020),
  fg = Color(0xFF1A1606),
  body = Color(0xFF2E2810),
  muted = Color(0xFF5A5020),
  dim = Color(0xFF665C28),
  accent = Color(0xFF1F52E0),
  mark = Color(0xFF10308C),
  accentLine = Color(0xFF1842B8),
  accentSoft = Color(0xFFDDE6FF),
  danger = Color(0xFFC0184C),
  ok = Color(0xFF167A4A),
  you = Color(0xFFFFD84D),
  kit = Color(0xFFF7E690),
  scheme = "light",
)

private fun neonColors() = CabColors(
  canvas = Color(0xFF120A1E),
  panel = Color(0xFF1B1030),
  track = Color(0xFF281848),
  line = Color(0xFF4A2E7A),
  edge = Color(0xFF7E58B8),
  fg = Color(0xFFFDF2FF),
  body = Color(0xFFE6D8F2),
  muted = Color(0xFFB89AD8),
  dim = Color(0xFFA088C4),
  accent = Color(0xFFFF2D95),
  mark = Color(0xFFFFA6D2),
  accentLine = Color(0xFFC0106A),
  accentSoft = Color(0xFF3A1030),
  danger = Color(0xFFFF5A5A),
  ok = Color(0xFF2EF2B0),
  you = Color(0xFF3A1458),
  kit = Color(0xFF281848),
)

private fun fizzColors() = CabColors(
  canvas = Color(0xFFE6FBFF),
  panel = Color(0xFFD2F4FB),
  track = Color(0xFFBCEAF4),
  line = Color(0xFF4E8A98),
  edge = Color(0xFF2E5C68),
  fg = Color(0xFF081A20),
  body = Color(0xFF142A32),
  muted = Color(0xFF2E5260),
  dim = Color(0xFF3A5E6C),
  accent = Color(0xFFE0107A),
  mark = Color(0xFF8E0848),
  accentLine = Color(0xFFC00C66),
  accentSoft = Color(0xFFFFD6EA),
  danger = Color(0xFFC4123A),
  ok = Color(0xFF0E7A5A),
  you = Color(0xFFB0EEFC),
  kit = Color(0xFFBCEAF4),
  scheme = "light",
)

private fun rainColors() = CabColors(
  canvas = Color(0xFF0F131F),
  panel = Color(0xFF161C2C),
  track = Color(0xFF20283C),
  line = Color(0xFF364260),
  edge = Color(0xFF5C6A90),
  fg = Color(0xFFE8ECF8),
  body = Color(0xFFC8D0E4),
  muted = Color(0xFF8E9AC0),
  dim = Color(0xFF8894BA),
  accent = Color(0xFF8C9FE6),
  mark = Color(0xFFC8D4FF),
  accentLine = Color(0xFF4E60A8),
  accentSoft = Color(0xFF1A2040),
  danger = Color(0xFFD06A90),
  ok = Color(0xFF5CB09A),
  you = Color(0xFF1E2644),
  kit = Color(0xFF20283C),
)

private fun mistColors() = CabColors(
  canvas = Color(0xFFECEEF6),
  panel = Color(0xFFE0E3EE),
  track = Color(0xFFD0D4E4),
  line = Color(0xFF7E86A4),
  edge = Color(0xFF505870),
  fg = Color(0xFF14161E),
  body = Color(0xFF22262E),
  muted = Color(0xFF464C62),
  dim = Color(0xFF4C526A),
  accent = Color(0xFF4A56A8),
  mark = Color(0xFF2A3270),
  accentLine = Color(0xFF3C4690),
  accentSoft = Color(0xFFD8DCF6),
  danger = Color(0xFFB02858),
  ok = Color(0xFF1E7462),
  you = Color(0xFFC8CCEC),
  kit = Color(0xFFD0D4E4),
  scheme = "light",
)

private fun fuseColors() = CabColors(
  canvas = Color(0xFF17150F),
  panel = Color(0xFF201D14),
  track = Color(0xFF2C281C),
  line = Color(0xFF4E4830),
  edge = Color(0xFF7C7450),
  fg = Color(0xFFFBF4E6),
  body = Color(0xFFE2D8C4),
  muted = Color(0xFFAEA48A),
  dim = Color(0xFFA0967E),
  accent = Color(0xFFFF7A00),
  mark = Color(0xFFFFBF80),
  accentLine = Color(0xFFC45A00),
  accentSoft = Color(0xFF33200A),
  danger = Color(0xFFFF5A6E),
  ok = Color(0xFF86C46A),
  you = Color(0xFF332A16),
  kit = Color(0xFF2C281C),
)

private fun gritColors() = CabColors(
  canvas = Color(0xFFF3EFE4),
  panel = Color(0xFFE9E3D2),
  track = Color(0xFFDCD4BC),
  line = Color(0xFF8A8060),
  edge = Color(0xFF5A5238),
  fg = Color(0xFF1A1810),
  body = Color(0xFF2C2818),
  muted = Color(0xFF504A30),
  dim = Color(0xFF5A543A),
  accent = Color(0xFFD2500A),
  mark = Color(0xFF8A3004),
  accentLine = Color(0xFFB44208),
  accentSoft = Color(0xFFFFDCC4),
  danger = Color(0xFFB4203A),
  ok = Color(0xFF4A7A1E),
  you = Color(0xFFEAD29A),
  kit = Color(0xFFDCD4BC),
  scheme = "light",
)

private fun sirenColors() = CabColors(
  canvas = Color(0xFF160608),
  panel = Color(0xFF200A0E),
  track = Color(0xFF2E1016),
  line = Color(0xFF58202A),
  edge = Color(0xFF8E3A48),
  fg = Color(0xFFFFF2F2),
  body = Color(0xFFECD4D6),
  muted = Color(0xFFC09AA0),
  dim = Color(0xFFAE8A90),
  accent = Color(0xFFFF2E3F),
  mark = Color(0xFFFFA0A8),
  accentLine = Color(0xFFC0101E),
  accentSoft = Color(0xFF3E0C12),
  danger = Color(0xFFFF6AB8),
  ok = Color(0xFF46D08A),
  you = Color(0xFF3A0E18),
  kit = Color(0xFF2E1016),
)

private fun flareColors() = CabColors(
  canvas = Color(0xFFFFF0EE),
  panel = Color(0xFFFDE0DC),
  track = Color(0xFFF6CCC6),
  line = Color(0xFFA06860),
  edge = Color(0xFF6A4038),
  fg = Color(0xFF1E0A0A),
  body = Color(0xFF301616),
  muted = Color(0xFF5A3030),
  dim = Color(0xFF663A3A),
  accent = Color(0xFFD4102C),
  mark = Color(0xFF880818),
  accentLine = Color(0xFFB00C22),
  accentSoft = Color(0xFFFFD4D4),
  danger = Color(0xFFB0147A),
  ok = Color(0xFF1A7A4E),
  you = Color(0xFFFFC2BC),
  kit = Color(0xFFF6CCC6),
  scheme = "light",
)

private fun staticColors() = CabColors(
  canvas = Color(0xFF0D1410),
  panel = Color(0xFF141C17),
  track = Color(0xFF1E2A22),
  line = Color(0xFF37493D),
  edge = Color(0xFF5E7866),
  fg = Color(0xFFF0F8F2),
  body = Color(0xFFD2DCD6),
  muted = Color(0xFF9AB0A2),
  dim = Color(0xFF8AA092),
  accent = Color(0xFFB388FF),
  mark = Color(0xFFDCC8FF),
  accentLine = Color(0xFF7A4EE0),
  accentSoft = Color(0xFF221A38),
  danger = Color(0xFFFF6A8A),
  ok = Color(0xFF52D490),
  you = Color(0xFF26203C),
  kit = Color(0xFF1E2A22),
)

private fun flickerColors() = CabColors(
  canvas = Color(0xFFEEF7F0),
  panel = Color(0xFFDFF0E4),
  track = Color(0xFFCCE4D4),
  line = Color(0xFF6A8E78),
  edge = Color(0xFF40604C),
  fg = Color(0xFF0E1A12),
  body = Color(0xFF1A2A20),
  muted = Color(0xFF365244),
  dim = Color(0xFF425E50),
  accent = Color(0xFF6A2FD0),
  mark = Color(0xFF3E1484),
  accentLine = Color(0xFF5824B0),
  accentSoft = Color(0xFFE8DCFF),
  danger = Color(0xFFB4204E),
  ok = Color(0xFF1A7A4A),
  you = Color(0xFFD8D0F8),
  kit = Color(0xFFCCE4D4),
  scheme = "light",
)

fun CabColors.toColorScheme() = if (scheme == "light") {
  lightColorScheme(
    primary = accent,
    onPrimary = canvas,
    primaryContainer = accentSoft,
    onPrimaryContainer = mark,
    secondary = ok,
    onSecondary = canvas,
    secondaryContainer = track,
    onSecondaryContainer = ok,
    tertiary = ok,
    onTertiary = canvas,
    background = canvas,
    onBackground = body,
    surface = panel,
    onSurface = fg,
    surfaceVariant = track,
    onSurfaceVariant = muted,
    surfaceContainerLowest = canvas,
    surfaceContainerLow = panel,
    surfaceContainer = track,
    surfaceContainerHigh = line,
    surfaceContainerHighest = kit,
    outline = edge,
    outlineVariant = line,
    error = danger,
    onError = fg,
    inversePrimary = accentLine,
  )
} else {
  darkColorScheme(
    primary = accent,
    onPrimary = canvas,
    primaryContainer = accentSoft,
    onPrimaryContainer = mark,
    secondary = ok,
    onSecondary = canvas,
    secondaryContainer = track,
    onSecondaryContainer = ok,
    tertiary = ok,
    onTertiary = canvas,
    background = canvas,
    onBackground = body,
    surface = panel,
    onSurface = fg,
    surfaceVariant = track,
    onSurfaceVariant = muted,
    surfaceContainerLowest = canvas,
    surfaceContainerLow = panel,
    surfaceContainer = track,
    surfaceContainerHigh = line,
    surfaceContainerHighest = kit,
    outline = edge,
    outlineVariant = line,
    error = danger,
    onError = fg,
    inversePrimary = accentLine,
  )
}

private fun cabTypography(chat: Float): Typography {
  val base = Typography()
  return Typography(
    displayLarge = base.displayLarge.copy(fontFamily = CabSans),
    displayMedium = base.displayMedium.copy(fontFamily = CabSans),
    displaySmall = base.displaySmall.copy(fontFamily = CabSans),
    headlineLarge = base.headlineLarge.copy(fontFamily = CabSans),
    headlineMedium = base.headlineMedium.copy(fontFamily = CabSans),
    headlineSmall = base.headlineSmall.copy(fontFamily = CabSans),
    titleLarge = base.titleLarge.copy(fontFamily = CabSans),
    titleMedium = base.titleMedium.copy(fontFamily = CabSans),
    titleSmall = base.titleSmall.copy(fontFamily = CabSans),
    bodyLarge = base.bodyLarge.copy(fontFamily = CabSans, fontSize = chat.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = CabSans),
    bodySmall = base.bodySmall.copy(fontFamily = CabSans),
    labelLarge = base.labelLarge.copy(fontFamily = CabSans),
    labelMedium = base.labelMedium.copy(fontFamily = CabSans),
    labelSmall = base.labelSmall.copy(fontFamily = CabSans),
  )
}

@Composable
fun CabTheme(themeId: String = "boom", fontId: String = "sm", content: @Composable () -> Unit) {
  val colors = cabColors(themeId)
  CompositionLocalProvider(LocalCabColors provides colors) {
    MaterialTheme(
      colorScheme = colors.toColorScheme(),
      typography = cabTypography(chatSp(parseFont(fontId))),
      content = content,
    )
  }
}
