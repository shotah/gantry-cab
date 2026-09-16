package com.gantree.cab.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.gantree.cab.mailbox.DEFAULT_LANG
import com.gantree.cab.mailbox.DEFAULT_PHOTO_SIZE
import com.gantree.cab.mailbox.FONT_IDS
import com.gantree.cab.mailbox.LANG_IDS
import com.gantree.cab.mailbox.PHOTO_SIZE_IDS
import com.gantree.cab.mailbox.THEME_IDS
import com.gantree.cab.mailbox.chatSp
import com.gantree.cab.mailbox.displaySlug
import com.gantree.cab.mailbox.fontLabel
import com.gantree.cab.mailbox.langLabel
import com.gantree.cab.mailbox.photoSizeChip
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
  photoSizeId: String = DEFAULT_PHOTO_SIZE,
  onPhotoSize: (String) -> Unit = {},
  backdropOn: Boolean = true,
  followTheme: Boolean = true,
  onBackdropToggle: () -> Unit = {},
  onFollowToggle: () -> Unit = {},
  voiceOffered: Boolean = false,
  permits: Permits = Permits(),
  gpsOn: Boolean = false,
  onGpsToggle: () -> Unit = {},
  onMicAsk: () -> Unit = {},
  onLocAsk: () -> Unit = {},
  onNotifyAsk: () -> Unit = {},
  langId: String = DEFAULT_LANG,
  onLang: (String) -> Unit = {},
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
    // Pendant's Access block: ask here so hold-to-talk is not the first prompt.
    Text("Access", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    if (voiceOffered) {
      PermitRow(
        name = "Microphone",
        action = if (permits.mic) "On" else "Enable microphone",
        onClick = if (permits.mic) null else onMicAsk,
      )
    }
    PermitRow(
      name = "Location",
      action = when {
        !permits.location -> "Enable location"
        gpsOn -> "On"
        else -> "Off"
      },
      pressed = permits.location && gpsOn,
      onClick = if (permits.location) onGpsToggle else onLocAsk,
    )
    PermitRow(
      name = "Notifications",
      action = if (permits.notify) "On" else "Enable notifications",
      onClick = if (permits.notify) null else onNotifyAsk,
    )
    if (voiceOffered) {
      // Pendant Settings → Language. Only the mouth: recognizer locale + `/api/tts` `lang`.
      SettingsPick(
        label = "Language",
        ids = LANG_IDS,
        selected = langId,
        itemLabel = ::langLabel,
        onPick = onLang,
      )
      Text(
        "Hold to talk listens, and ${displaySlug(slug)} speaks, in this language.",
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
      )
    }
    SettingsPick(
      label = "Theme",
      ids = THEME_IDS,
      selected = themeId,
      itemLabel = ::themeLabel,
      onPick = onTheme,
      leading = { ThemeSwatch(it, modifier = Modifier.size(16.dp)) },
    )
    SettingsPick(
      label = "Font size",
      ids = FONT_IDS,
      selected = fontId,
      itemLabel = ::fontLabel,
      onPick = onFont,
      itemText = { Text(fontLabel(it), fontSize = chatSp(it).sp) },
    )
    SettingsPick(
      label = "Photo size",
      ids = PHOTO_SIZE_IDS,
      selected = photoSizeId,
      itemLabel = ::photoSizeChip,
      onPick = onPhotoSize,
    )
    Text(
      "Smaller sends faster and costs fewer tokens to look at.",
      style = MaterialTheme.typography.bodySmall,
      color = scheme.onSurfaceVariant,
    )
    Text("Follow Kit's mood", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = followTheme,
        onClick = onFollowToggle,
        label = { Text(if (followTheme) "On" else "Off") },
        modifier = Modifier.semantics {
          role = Role.Switch
          selected = followTheme
          contentDescription = "Follow Kit's mood"
        },
      )
    }
    Text(
      "When on, ${displaySlug(slug)} picks the color theme. Off keeps the one you pick.",
      style = MaterialTheme.typography.bodySmall,
      color = scheme.onSurfaceVariant,
    )
    Text("Backdrop", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = backdropOn,
        onClick = onBackdropToggle,
        label = { Text(if (backdropOn) "On" else "Off") },
        modifier = Modifier.semantics {
          role = Role.Switch
          selected = backdropOn
          contentDescription = "Backdrop"
        },
      )
    }
    Text(
      "${displaySlug(slug)} can paint a wallpaper behind the thread. Off keeps the theme.",
      style = MaterialTheme.typography.bodySmall,
      color = scheme.onSurfaceVariant,
    )
    Text("Android Auto", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    Text(
      "Kit arrives as a message card that Auto reads aloud; tap the card to reply by voice. " +
        "Needs Android Auto → Developer settings → Unknown sources. " +
        "Open Cab in the car and tap Reply on the first card to start talking, or open Cab here " +
        "before you drive and lock the phone. Plug in, then tap Test to hear a check message.",
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

/**
 * One closed-set setting as a dropdown (Theme, Font size, Photo size,
 * Language): a read-only field showing the pick, the choices in a menu under
 * it. Same shape as the PWA's `<select>` rows; the drawer stays one column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPick(
  label: String,
  ids: List<String>,
  selected: String,
  itemLabel: (String) -> String,
  onPick: (String) -> Unit,
  leading: (@Composable (String) -> Unit)? = null,
  itemText: @Composable (String) -> Unit = { Text(itemLabel(it)) },
) {
  var open by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
    OutlinedTextField(
      value = itemLabel(selected),
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text(label) },
      leadingIcon = leading?.let { paint -> { paint(selected) } },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        .semantics { contentDescription = label },
    )
    ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      for (id in ids) {
        val on = id == selected
        DropdownMenuItem(
          text = { itemText(id) },
          leadingIcon = leading?.let { paint -> { paint(id) } },
          onClick = {
            onPick(id)
            open = false
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
          modifier = Modifier.semantics {
            role = Role.RadioButton
            this.selected = on
            contentDescription = itemLabel(id)
          },
        )
      }
    }
  }
}

/**
 * One Access line: name left, a chip right when there is something to do,
 * plain "On" when there is not (pendant `PermitRow`). Location's chip is a
 * switch once granted — the same send-on-turns pref as the attach GPS chip.
 */
@Composable
private fun PermitRow(
  name: String,
  action: String,
  pressed: Boolean = false,
  onClick: (() -> Unit)?,
) {
  val scheme = MaterialTheme.colorScheme
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      name,
      style = MaterialTheme.typography.bodyMedium,
      color = scheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    if (onClick == null) {
      Text(action, style = MaterialTheme.typography.labelLarge, color = scheme.primary)
    } else {
      FilterChip(
        selected = pressed,
        onClick = onClick,
        label = { Text(action) },
        modifier = Modifier.semantics {
          role = Role.Button
          selected = pressed
          contentDescription = action
        },
      )
    }
  }
}
