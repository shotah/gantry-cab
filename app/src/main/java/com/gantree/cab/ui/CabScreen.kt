package com.gantree.cab.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gantree.cab.ChatLine
import com.gantree.cab.dev.SAMPLE_IDS
import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.decodeDataUrl
import com.gantree.cab.mailbox.displaySlug

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
  dev: Boolean = false,
  compact: Boolean = false,
  onSample: (String) -> Unit = {},
  themeId: String = "boom",
  fontId: String = "sm",
  gpsOn: Boolean = true,
  catalog: List<SlashCommand> = emptyList(),
  avatarBytes: ByteArray? = null,
  faceHint: String = "",
  cranes: List<String> = emptyList(),
  onTheme: (String) -> Unit = {},
  onFont: (String) -> Unit = {},
  onGpsToggle: () -> Unit = {},
  onPhoto: () -> Unit = {},
  onAvatar: () -> Unit = {},
  onPin: () -> Unit = {},
  onEngage: () -> Unit = {},
) {
  val colors = LocalCabColors.current
  var settingsOpen by remember { mutableStateOf(false) }
  val list = rememberLazyListState()
  val title = displaySlug(slug)
  val googleDoor = googleReady && email.isBlank() && spike.isBlank() && lines.isEmpty()
  LaunchedEffect(lines.size) {
    if (lines.isNotEmpty()) {
      list.animateScrollToItem(lines.lastIndex)
    }
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.canvas)
      .imePadding(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(colors.panel)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      KitAvatar(
        slug = slug,
        bytes = avatarBytes,
        size = 40.dp,
        editable = !googleDoor,
        onClick = onAvatar,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = colors.fg, style = MaterialTheme.typography.titleMedium)
        Text(
          if (up) "live" else "down",
          color = if (up) colors.ok else colors.dim,
          fontSize = 11.sp,
        )
      }
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { settingsOpen = !settingsOpen }
          .semantics { contentDescription = "settings" },
        contentAlignment = Alignment.Center,
      ) {
        Text("⚙", color = if (settingsOpen) colors.fg else colors.muted, fontSize = 18.sp)
      }
    }
    if (dev && !compact) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp),
      ) {
        for (id in SAMPLE_IDS) {
          TextButton(onClick = { onSample(id) }) { Text(id, color = colors.accent) }
        }
      }
    }
    if (settingsOpen && !compact) {
      Surface(color = colors.panel, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(12.dp)) {
          CabSettings(
            origin = origin,
            slug = slug,
            spike = spike,
            email = email,
            googleReady = googleReady,
            cranes = cranes,
            themeId = themeId,
            fontId = fontId,
            onOrigin = onOrigin,
            onSlug = onSlug,
            onSpike = onSpike,
            onTheme = onTheme,
            onFont = onFont,
            onConnect = onConnect,
            onGoogle = onGoogle,
            onSignOut = onSignOut,
          )
        }
      }
    }
    if (faceHint.isNotBlank()) {
      Text(
        faceHint,
        color = colors.danger,
        fontSize = 11.sp,
        modifier = Modifier
          .fillMaxWidth()
          .background(colors.panel)
          .padding(horizontal = 12.dp, vertical = 4.dp),
      )
    }
    if (!googleDoor && hint.isNotBlank()) {
      Text(
        hint,
        color = colors.muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      )
    }
    if (googleDoor) {
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        KitAvatar(slug = slug, bytes = avatarBytes, size = 64.dp)
        Text("Sign in with Google to talk.", color = colors.body, modifier = Modifier.padding(top = 12.dp))
        Button(
          onClick = onGoogle,
          modifier = Modifier.padding(top = 12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = colors.accentSoft, contentColor = colors.mark),
        ) {
          Text("Continue with Google")
        }
      }
    } else {
      LazyColumn(
        state = list,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
      ) {
        if (lines.isEmpty()) {
          item {
            Column(
              modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              KitAvatar(slug = slug, bytes = avatarBytes, size = 64.dp)
              Text(
                "Nothing yet. Type below — or / for harness commands.",
                color = colors.dim,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
              )
            }
          }
        }
        items(lines, key = { it.id }) { line ->
          val mine = line.fromYou
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
          ) {
            Surface(
              color = if (mine) colors.you else colors.kit,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(0.85f),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (line.kind == "push") {
                  Text("PING", color = colors.dim, fontSize = 11.sp)
                }
                line.photo?.let { ChatPhoto(it) }
                if (line.text.isNotBlank()) {
                  Text(line.text, color = colors.fg, style = MaterialTheme.typography.bodyLarge)
                }
              }
            }
          }
        }
      }
      Box(modifier = Modifier.background(colors.panel).padding(12.dp)) {
        CabCompose(
          disabled = !up,
          placeholder = "Message $title · / for commands",
          gpsOn = gpsOn,
          gpsHint = hint,
          catalog = catalog,
          onSend = onSend,
          onPhoto = onPhoto,
          onPin = onPin,
          onGpsToggle = onGpsToggle,
          onEngage = onEngage,
        )
      }
    }
  }
}

@Composable
private fun ChatPhoto(url: String) {
  val bmp = remember(url) {
    val bytes = decodeDataUrl(url) ?: return@remember null
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
  }
  if (bmp != null) {
    Image(
      bitmap = bmp,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxWidth().heightIn(max = 192.dp).clip(RoundedCornerShape(8.dp)),
    )
  }
}
