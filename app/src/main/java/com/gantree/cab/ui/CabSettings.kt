package com.gantree.cab.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gantree.cab.mailbox.allowlistCopy
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
  sub: String = "",
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
  authHint: String = "",
  signingIn: Boolean = false,
  onCarTest: () -> Unit = {},
) {
  val scheme = MaterialTheme.colorScheme
  val clipboard = LocalClipboardManager.current
  var copied by remember { mutableStateOf(false) }
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 8.dp)
      .padding(bottom = 32.dp)
      .semantics { contentDescription = "Settings" },
  ) {
    Text(
      "You are the operator. The name in the chat bar is the crane.",
      style = MaterialTheme.typography.bodyMedium,
      color = scheme.onSurfaceVariant,
    )
    if (cranes.isNotEmpty()) {
      Text("Talking to", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
      ) {
        for (c in cranes) {
          FilterChip(
            selected = c == slug,
            onClick = { onSlug(c) },
            label = { Text(displaySlug(c)) },
          )
        }
      }
    }
    OutlinedTextField(
      value = origin,
      onValueChange = onOrigin,
      modifier = Modifier.fillMaxWidth(),
      label = { Text("Mailbox") },
      placeholder = { Text("https://pendant.example.com") },
      supportingText = {
        Text("The pendant Worker host — same site as the PWA. Cloudflare URL, or Gantree’s PENDANT_MAILBOX_URL without /ws/kit.")
      },
      singleLine = true,
    )
    OutlinedTextField(
      value = slug,
      onValueChange = { onSlug(it.lowercase()) },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("Talking to") },
      supportingText = { Text("The crane’s room, usually kit. Not your name.") },
      singleLine = true,
    )
    if (googleReady && email.isBlank()) {
      Button(
        onClick = onGoogle,
        enabled = !signingIn,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(if (signingIn) "Opening Google…" else "Continue with Google")
      }
    }
    if (authHint.isNotBlank()) {
      Text(
        authHint,
        style = MaterialTheme.typography.bodySmall,
        color = if (authHint.startsWith("Opening") || authHint.startsWith("Signed")) {
          scheme.primary
        } else {
          scheme.error
        },
      )
    }
    if (!googleReady) {
      Text(
        "This APK has no Google Sign-In. Use a phone secret, or rebuild with CAB_GOOGLE_WEB_CLIENT_ID.",
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
      )
    }
    OutlinedTextField(
      value = spike,
      onValueChange = onSpike,
      modifier = Modifier.fillMaxWidth(),
      label = { Text("Phone secret") },
      supportingText = {
        Text(
          if (googleReady) {
            "Optional. Lab MAILBOX_SECRET only — not the crane’s PENDANT_BEARER."
          } else {
            "MAILBOX_SECRET from pendant .dev.vars. Not PENDANT_BEARER (that’s Kit’s socket)."
          },
        )
      },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
    )
    Text("Theme", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .semantics { contentDescription = "color theme" },
    ) {
      for (id in THEME_IDS) {
        val on = id == themeId
        FilterChip(
          selected = on,
          onClick = { onTheme(id) },
          label = { Text(themeLabel(id)) },
          leadingIcon = { ThemeSwatch(id, modifier = Modifier.padding(start = 4.dp)) },
          modifier = Modifier.semantics {
            role = Role.RadioButton
            selected = on
            contentDescription = themeLabel(id)
          },
        )
      }
    }
    Text("Font size", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .semantics { contentDescription = "Font size" },
    ) {
      for (id in FONT_IDS) {
        val on = id == fontId
        FilterChip(
          selected = on,
          onClick = { onFont(id) },
          label = { Text(fontLabel(id), fontSize = chatSp(id).sp) },
          modifier = Modifier.semantics {
            role = Role.RadioButton
            selected = on
            contentDescription = fontLabel(id)
          },
        )
      }
    }
    Text("Android Auto", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    Text(
      "Cab has no tile in the car from a sideload. Kit arrives as a message card that Auto reads aloud; " +
        "tap the card to reply by voice. Needs Android Auto → Developer settings → Unknown sources, " +
        "and Cab must be Live before you plug in. Plug in, then tap Test to hear a check message.",
      style = MaterialTheme.typography.bodySmall,
      color = scheme.onSurfaceVariant,
    )
    FilledTonalButton(
      onClick = onCarTest,
      modifier = Modifier.semantics { contentDescription = "test car voice" },
    ) {
      Text("Test car voice")
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilledTonalButton(onClick = onConnect) { Text("Connect") }
      if (email.isNotBlank()) {
        TextButton(onClick = onSignOut) { Text("Sign out") }
      }
    }
    if (email.isNotBlank()) {
      Text(email, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
    if (email.isNotBlank() && cranes.isEmpty() && sub.isNotBlank()) {
      Text(
        "Not on any crane yet — give this to your yard admin",
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
      )
      Text(
        sub,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = scheme.onSurface,
        modifier = Modifier.semantics { contentDescription = "google sub" },
      )
      TextButton(
        onClick = {
          clipboard.setText(AnnotatedString(allowlistCopy(email, sub)))
          copied = true
        },
      ) {
        Text(if (copied) "Copied" else "Copy")
      }
    }
  }
}
