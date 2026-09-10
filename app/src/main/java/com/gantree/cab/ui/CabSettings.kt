package com.gantree.cab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gantree.cab.mailbox.FONT_IDS
import com.gantree.cab.mailbox.THEME_IDS
import com.gantree.cab.mailbox.chatSp
import com.gantree.cab.mailbox.displaySlug

@Composable
fun CabSettings(
  origin: String,
  slug: String,
  spike: String,
  email: String,
  googleReady: Boolean,
  cranes: List<String>,
  themeId: String,
  fontId: String,
  onOrigin: (String) -> Unit,
  onSlug: (String) -> Unit,
  onSpike: (String) -> Unit,
  onTheme: (String) -> Unit,
  onFont: (String) -> Unit,
  onConnect: () -> Unit,
  onGoogle: () -> Unit,
  onSignOut: () -> Unit,
) {
  val colors = LocalCabColors.current
  Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
    if (cranes.isNotEmpty()) {
      Text("Agent", color = colors.muted, fontSize = 12.sp)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (c in cranes) {
          val on = c == slug
          Text(
            displaySlug(c),
            color = if (on) colors.accent else colors.body,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { onSlug(c) }
              .background(if (on) colors.track else colors.canvas)
              .padding(horizontal = 8.dp, vertical = 6.dp),
          )
        }
      }
    }
    OutlinedTextField(
      value = origin,
      onValueChange = onOrigin,
      label = { Text("Mailbox origin") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      colors = fieldColors(),
    )
    OutlinedTextField(
      value = slug,
      onValueChange = { onSlug(it.lowercase()) },
      label = { Text("Agent name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      colors = fieldColors(),
    )
    OutlinedTextField(
      value = spike,
      onValueChange = onSpike,
      label = { Text("Agent access secret") },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
      modifier = Modifier.fillMaxWidth(),
      colors = fieldColors(),
    )
    Text("Theme", color = colors.muted, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      for (id in THEME_IDS) {
        val on = id == themeId
        val swatch = cabColors(id)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (on) colors.accent else colors.line, RoundedCornerShape(8.dp))
            .clickable { onTheme(id) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
          Canvas(Modifier.size(12.dp).clip(CircleShape)) {
            drawArc(swatch.canvas, 90f, 180f, true)
            drawArc(swatch.accent, 270f, 180f, true)
          }
          Text(id.replaceFirstChar { it.uppercase() }, color = colors.body, fontSize = 12.sp)
        }
      }
    }
    Text("Font size", color = colors.muted, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
      for (id in FONT_IDS) {
        val on = id == fontId
        Text(
          "Aa",
          color = colors.fg,
          fontSize = chatSp(id).sp,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (on) colors.accentLine else colors.line, RoundedCornerShape(8.dp))
            .background(if (on) colors.track else colors.canvas)
            .clickable { onFont(id) }
            .padding(vertical = 8.dp),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.canvas)) {
        Text("Listen")
      }
      if (googleReady && email.isBlank()) {
        Button(onClick = onGoogle, colors = ButtonDefaults.buttonColors(containerColor = colors.panel, contentColor = colors.body)) {
          Text("Google")
        }
      }
      if (email.isNotBlank()) {
        TextButton(onClick = onSignOut) { Text("sign out", color = colors.dim) }
      }
    }
    if (email.isNotBlank()) {
      Text(email, color = colors.muted, fontSize = 12.sp)
    }
  }
}
