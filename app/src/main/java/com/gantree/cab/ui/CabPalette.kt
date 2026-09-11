package com.gantree.cab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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
)

val LocalCabColors = staticCompositionLocalOf { boomColors() }

fun cabColors(themeId: String): CabColors = when (parseTheme(themeId)) {
  "inlay" -> inlayColors()
  "lamp" -> lampColors()
  "noir" -> noirColors()
  "ember" -> emberColors()
  "tide" -> tideColors()
  "bloom" -> bloomColors()
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

fun CabColors.toColorScheme() = darkColorScheme(
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
