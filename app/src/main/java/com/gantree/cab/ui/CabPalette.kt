package com.gantree.cab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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

@Composable
fun CabTheme(themeId: String = "boom", fontId: String = "sm", content: @Composable () -> Unit) {
  val colors = cabColors(themeId)
  val chat = chatSp(parseFont(fontId)).sp
  val type = Typography(
    titleLarge = TextStyle(fontFamily = CabSans, fontSize = 22.sp, color = colors.fg),
    titleMedium = TextStyle(fontFamily = CabSans, fontSize = 16.sp, color = colors.fg),
    bodyLarge = TextStyle(fontFamily = CabSans, fontSize = chat, color = colors.body),
    bodySmall = TextStyle(fontFamily = CabSans, fontSize = 12.sp, color = colors.muted),
    labelLarge = TextStyle(fontFamily = CabSans, fontSize = 14.sp, color = colors.fg),
  )
  CompositionLocalProvider(LocalCabColors provides colors) {
    MaterialTheme(
      colorScheme = darkColorScheme(
        background = colors.canvas,
        surface = colors.panel,
        primary = colors.accent,
        onPrimary = colors.canvas,
        onBackground = colors.body,
        onSurface = colors.fg,
      ),
      typography = type,
      content = content,
    )
  }
}

@Composable
fun fieldColors(): TextFieldColors {
  val colors = LocalCabColors.current
  return OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.fg,
    unfocusedTextColor = colors.fg,
    focusedBorderColor = colors.accent,
    unfocusedBorderColor = colors.line,
    focusedLabelColor = colors.muted,
    unfocusedLabelColor = colors.muted,
    cursorColor = colors.accent,
    focusedPlaceholderColor = colors.muted,
    unfocusedPlaceholderColor = colors.muted,
  )
}

@Composable
fun composeFieldColors(): TextFieldColors {
  val colors = LocalCabColors.current
  return OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.fg,
    unfocusedTextColor = colors.fg,
    disabledTextColor = colors.muted,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    focusedLabelColor = colors.muted,
    unfocusedLabelColor = colors.muted,
    cursorColor = colors.accent,
    focusedPlaceholderColor = colors.muted,
    unfocusedPlaceholderColor = colors.muted,
  )
}
