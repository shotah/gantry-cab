package com.gantree.cab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Two-tone theme preview — not an icon, just the palette. */
@Composable
fun ThemeSwatch(themeId: String, modifier: Modifier = Modifier.size(12.dp)) {
  val swatch = cabColors(themeId)
  Box(
    modifier = modifier
      .clip(CircleShape)
      .border(1.dp, swatch.line, CircleShape),
  ) {
    Canvas(Modifier.fillMaxSize()) {
      drawArc(swatch.canvas, 90f, 180f, true)
      drawArc(swatch.accent, 270f, 180f, true)
    }
  }
}
