package com.gantree.cab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gantree.cab.ChatLine

private val Canvas = Color(0xFF0E1316)
private val Panel = Color(0xFF171D22)
private val Body = Color(0xFFE6E4E1)
private val Muted = Color(0xFF9AA3AE)
private val Accent = Color(0xFFF07848)
private val You = Color(0xFF2A333A)
private val Kit = Color(0xFF232B32)

@Composable
fun CabTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = darkColorScheme(
      background = Canvas,
      surface = Panel,
      primary = Accent,
      onPrimary = Canvas,
      onBackground = Body,
      onSurface = Body,
    ),
    content = content,
  )
}

@Composable
fun CabScreen(
  origin: String,
  slug: String,
  spike: String,
  email: String,
  googleReady: Boolean,
  up: Boolean,
  hint: String,
  lines: List<ChatLine>,
  onOrigin: (String) -> Unit,
  onSlug: (String) -> Unit,
  onSpike: (String) -> Unit,
  onConnect: () -> Unit,
  onGoogle: () -> Unit,
  onSignOut: () -> Unit,
  onSend: (String) -> Unit,
) {
  var draft by remember { mutableStateOf("") }
  val list = rememberLazyListState()
  LaunchedEffect(lines.size) {
    if (lines.isNotEmpty()) {
      list.animateScrollToItem(lines.lastIndex)
    }
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Canvas)
      .imePadding()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text("cab", color = Accent, style = MaterialTheme.typography.titleLarge)
    Text(
      if (up) "live · $slug" else "down",
      color = if (up) Color(0xFF3DB8A0) else Muted,
      style = MaterialTheme.typography.bodySmall,
    )
    if (hint.isNotBlank()) {
      Text(hint, color = Muted, style = MaterialTheme.typography.bodySmall)
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
      onValueChange = onSlug,
      label = { Text("Crane slug") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      colors = fieldColors(),
    )
    OutlinedTextField(
      value = spike,
      onValueChange = onSpike,
      label = { Text("Spike secret (loopback)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      colors = fieldColors(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Canvas)) {
        Text("Listen")
      }
      if (googleReady) {
        Button(onClick = onGoogle, colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Body)) {
          Text("Google")
        }
      }
      if (email.isNotBlank()) {
        TextButton(onClick = onSignOut) { Text("Sign out", color = Muted) }
      }
    }
    if (email.isNotBlank()) {
      Text(email, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
    LazyColumn(
      state = list,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 8.dp),
    ) {
      items(lines, key = { it.id }) { line ->
        Surface(
          color = if (line.fromYou) You else Kit,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(0.92f).then(
            if (line.fromYou) Modifier.padding(start = 48.dp) else Modifier.padding(end = 48.dp),
          ),
        ) {
          Text(line.text, color = Body, modifier = Modifier.padding(12.dp))
        }
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.weight(1f),
        placeholder = { Text("Message Kit") },
        colors = fieldColors(),
      )
      Button(
        onClick = {
          val t = draft
          draft = ""
          onSend(t)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Canvas),
      ) {
        Text("Send")
      }
    }
  }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
  focusedTextColor = Body,
  unfocusedTextColor = Body,
  focusedBorderColor = Accent,
  unfocusedBorderColor = Color(0xFF3A4550),
  focusedLabelColor = Muted,
  unfocusedLabelColor = Muted,
  cursorColor = Accent,
)
