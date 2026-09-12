package com.gantree.cab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

val LocalCabColors = staticCompositionLocalOf { boomColors() }

fun cabColors(themeId: String): CabColors = when (parseTheme(themeId)) {
  "inlay" -> inlayColors()
  "lamp" -> lampColors()
  "noir" -> noirColors()
  "ember" -> emberColors()
  "tide" -> tideColors()
  "bloom" -> bloomColors()
  "paper" -> paperColors()
  "chalk" -> chalkColors()
  "foam" -> foamColors()
  "petal" -> petalColors()
  "ink" -> inkColors()
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

private fun inlayColors() = CabColors(
  canvas = Color(0xFF0C110F),
  panel = Color(0xFF151C19),
  track = Color(0xFF1E2823),
  line = Color(0xFF33423B),
  edge = Color(0xFF5A6E64),
  fg = Color(0xFFF2EBE0),
  body = Color(0xFFD9D0C4),
  muted = Color(0xFFA3ADA6),
  dim = Color(0xFF8A948C),
  accent = Color(0xFFE6D3B0),
  mark = Color(0xFFF7EBD4),
  accentLine = Color(0xFFA89068),
  accentSoft = Color(0xFF243028),
  danger = Color(0xFFD4787A),
  ok = Color(0xFF6BAF9A),
  you = Color(0xFF2A2820),
  kit = Color(0xFF1E2823),
)

private fun lampColors() = CabColors(
  canvas = Color(0xFF0C0C16),
  panel = Color(0xFF151522),
  track = Color(0xFF1E1E2E),
  line = Color(0xFF32324A),
  edge = Color(0xFF5A5A78),
  fg = Color(0xFFEEF0E6),
  body = Color(0xFFD5D8C8),
  muted = Color(0xFF9AA090),
  dim = Color(0xFF8A9088),
  accent = Color(0xFFC5D24A),
  mark = Color(0xFFE4EEC8),
  accentLine = Color(0xFF8A9430),
  accentSoft = Color(0xFF222418),
  danger = Color(0xFFE07090),
  ok = Color(0xFF5EC8B0),
  you = Color(0xFF2A2A18),
  kit = Color(0xFF1E1E2E),
)

private fun noirColors() = CabColors(
  canvas = Color(0xFF0A0C10),
  panel = Color(0xFF12151A),
  track = Color(0xFF1A1F28),
  line = Color(0xFF2E3644),
  edge = Color(0xFF5A6578),
  fg = Color(0xFFE8EEF4),
  body = Color(0xFFC5CED8),
  muted = Color(0xFF8A96A8),
  dim = Color(0xFF6E7A8C),
  accent = Color(0xFF8EB4D4),
  mark = Color(0xFFD4E4F4),
  accentLine = Color(0xFF4A78A0),
  accentSoft = Color(0xFF121820),
  danger = Color(0xFFD07090),
  ok = Color(0xFF5CB8A8),
  you = Color(0xFF1A2430),
  kit = Color(0xFF1A1F28),
)

private fun emberColors() = CabColors(
  canvas = Color(0xFF120C0A),
  panel = Color(0xFF1A1210),
  track = Color(0xFF261C16),
  line = Color(0xFF4A3430),
  edge = Color(0xFF7A5850),
  fg = Color(0xFFF4ECE4),
  body = Color(0xFFDCC8BC),
  muted = Color(0xFFB09080),
  dim = Color(0xFF8A7064),
  accent = Color(0xFFE07040),
  mark = Color(0xFFF4C4A0),
  accentLine = Color(0xFFA04828),
  accentSoft = Color(0xFF241410),
  danger = Color(0xFFE07090),
  ok = Color(0xFF6BB090),
  you = Color(0xFF2A1410),
  kit = Color(0xFF261C16),
)

private fun tideColors() = CabColors(
  canvas = Color(0xFF0A1214),
  panel = Color(0xFF101A1C),
  track = Color(0xFF182428),
  line = Color(0xFF2A3C44),
  edge = Color(0xFF4A6870),
  fg = Color(0xFFE4F0EE),
  body = Color(0xFFC4D8D4),
  muted = Color(0xFF88A8A8),
  dim = Color(0xFF6E8888),
  accent = Color(0xFF3CB8B0),
  mark = Color(0xFFB8ECE4),
  accentLine = Color(0xFF2A7878),
  accentSoft = Color(0xFF102020),
  danger = Color(0xFFD07890),
  ok = Color(0xFF4CBC9C),
  you = Color(0xFF142428),
  kit = Color(0xFF182428),
)

private fun bloomColors() = CabColors(
  canvas = Color(0xFF100C14),
  panel = Color(0xFF18141E),
  track = Color(0xFF221C2A),
  line = Color(0xFF3A3048),
  edge = Color(0xFF6A5878),
  fg = Color(0xFFF0E8F4),
  body = Color(0xFFD8D0DC),
  muted = Color(0xFFA890B0),
  dim = Color(0xFF8A7898),
  accent = Color(0xFFD070C0),
  mark = Color(0xFFF0C8E8),
  accentLine = Color(0xFF884878),
  accentSoft = Color(0xFF20141E),
  danger = Color(0xFFE07090),
  ok = Color(0xFF68B8A0),
  you = Color(0xFF241428),
  kit = Color(0xFF221C2A),
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

private fun chalkColors() = CabColors(
  canvas = Color(0xFFF2F5F8),
  panel = Color(0xFFE6ECF2),
  track = Color(0xFFD8E0E8),
  line = Color(0xFF7A8A98),
  edge = Color(0xFF4A5A68),
  fg = Color(0xFF12161C),
  body = Color(0xFF1E2630),
  muted = Color(0xFF3A4856),
  dim = Color(0xFF465462),
  accent = Color(0xFF1E5A8C),
  mark = Color(0xFF0E3A60),
  accentLine = Color(0xFF164A74),
  accentSoft = Color(0xFFD0E0F0),
  danger = Color(0xFFB42858),
  ok = Color(0xFF1A7060),
  you = Color(0xFFC8D6E4),
  kit = Color(0xFFD8E0E8),
  scheme = "light",
)

private fun foamColors() = CabColors(
  canvas = Color(0xFFEEF6F5),
  panel = Color(0xFFE0EEEC),
  track = Color(0xFFD0E4E0),
  line = Color(0xFF5E8884),
  edge = Color(0xFF3A5C58),
  fg = Color(0xFF102018),
  body = Color(0xFF1A2C2A),
  muted = Color(0xFF345250),
  dim = Color(0xFF425E5C),
  accent = Color(0xFF0C6E68),
  mark = Color(0xFF064840),
  accentLine = Color(0xFF0A5C58),
  accentSoft = Color(0xFFC4E8E4),
  danger = Color(0xFFB42858),
  ok = Color(0xFF1A7A64),
  you = Color(0xFFB8D8D4),
  kit = Color(0xFFD0E4E0),
  scheme = "light",
)

private fun petalColors() = CabColors(
  canvas = Color(0xFFF7F1F6),
  panel = Color(0xFFEFE4EE),
  track = Color(0xFFE6D8E6),
  line = Color(0xFF8E748E),
  edge = Color(0xFF5A485A),
  fg = Color(0xFF1A121C),
  body = Color(0xFF2A2030),
  muted = Color(0xFF4E3E56),
  dim = Color(0xFF5A4A62),
  accent = Color(0xFFA02080),
  mark = Color(0xFF6E0858),
  accentLine = Color(0xFF881068),
  accentSoft = Color(0xFFF4D0E8),
  danger = Color(0xFFB42858),
  ok = Color(0xFF1A7A64),
  you = Color(0xFFE4C0DC),
  kit = Color(0xFFE6D8E6),
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
