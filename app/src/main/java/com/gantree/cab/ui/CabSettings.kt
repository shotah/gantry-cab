package com.gantree.cab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gantree.cab.mailbox.FONT_IDS
import com.gantree.cab.mailbox.THEME_IDS
import com.gantree.cab.mailbox.chatSp
import com.gantree.cab.mailbox.displaySlug
import com.gantree.cab.mailbox.fontLabel
import com.gantree.cab.mailbox.themeLabel

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
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
  ) {
    if (cranes.isNotEmpty()) {
      Text("Agent", color = colors.muted, fontSize = 12.sp)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (c in cranes) {
          val on = c == slug
          Text(
            displaySlug(c),
            color = if (on) colors.accent else colors.body,
            fontSize = 14.sp,
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
    CabInput(label = "Mailbox origin", value = origin, onValueChange = onOrigin)
    CabInput(label = "Agent name", value = slug, onValueChange = { onSlug(it.lowercase()) })
    CabInput(label = "Agent access secret", value = spike, onValueChange = onSpike, secret = true)
    Text("Theme", color = colors.muted, fontSize = 12.sp)
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier
        .fillMaxWidth()
        .semantics { contentDescription = "color theme" },
    ) {
      for (id in THEME_IDS) {
        val on = id == themeId
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
          modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (on) colors.accentLine else colors.edge, RoundedCornerShape(8.dp))
            .background(if (on) colors.track else colors.canvas)
            .clickable { onTheme(id) }
            .padding(horizontal = 6.dp)
            .semantics {
              role = Role.RadioButton
              selected = on
              contentDescription = themeLabel(id)
            },
        ) {
          ThemeSwatch(id)
          Text(themeLabel(id), color = colors.body, fontSize = 11.sp)
        }
      }
    }
    Text("Font size", color = colors.muted, fontSize = 12.sp)
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier
        .fillMaxWidth()
        .semantics { contentDescription = "Font size" },
    ) {
      for (id in FONT_IDS) {
        val on = id == fontId
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (on) colors.accentLine else colors.edge, RoundedCornerShape(8.dp))
            .background(if (on) colors.track else colors.canvas)
            .clickable { onFont(id) }
            .semantics {
              role = Role.RadioButton
              selected = on
              contentDescription = fontLabel(id)
            },
        ) {
          Text("Aa", color = colors.fg, fontSize = chatSp(id).sp, fontFamily = CabSans)
        }
      }
    }
    HorizontalDivider(color = colors.line, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AccentAction("Listen", onClick = onConnect)
      if (googleReady && email.isBlank()) {
        AccentAction("Google", onClick = onGoogle, filled = false)
      }
      if (email.isNotBlank()) {
        Text(
          "sign out",
          color = colors.dim,
          fontSize = 12.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onSignOut)
            .padding(vertical = 4.dp),
        )
      }
    }
    if (email.isNotBlank()) {
      Text(
        email,
        color = colors.muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun AccentAction(label: String, onClick: () -> Unit, filled: Boolean = true) {
  val colors = LocalCabColors.current
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, colors.accentLine, RoundedCornerShape(12.dp))
      .background(if (filled) colors.accentSoft else colors.panel)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .semantics { role = Role.Button },
  ) {
    Text(label, color = colors.mark, fontSize = 14.sp)
  }
}

@Composable
private fun CabInput(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  secret: Boolean = false,
) {
  val colors = LocalCabColors.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
    Text(label, color = colors.muted, fontSize = 12.sp)
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
      textStyle = TextStyle(color = colors.fg, fontSize = 14.sp, fontFamily = CabSans),
      cursorBrush = SolidColor(colors.accent),
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .border(1.dp, colors.edge, RoundedCornerShape(8.dp))
        .background(colors.canvas)
        .padding(horizontal = 8.dp, vertical = 8.dp),
    )
  }
}
