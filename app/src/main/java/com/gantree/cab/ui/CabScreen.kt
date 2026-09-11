package com.gantree.cab.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gantree.cab.ChatLine
import com.gantree.cab.dev.SAMPLE_IDS
import com.gantree.cab.mailbox.DEFAULT_PHOTO_SIZE
import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.displaySlug
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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
  authHint: String = "",
  signingIn: Boolean = false,
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
  typingUntil: Long = 0L,
  sub: String = "",
  onCarTest: () -> Unit = {},
  photoSizeId: String = DEFAULT_PHOTO_SIZE,
  onPhotoSize: (String) -> Unit = {},
  backdropBytes: ByteArray? = null,
  backdropOn: Boolean = true,
  followTheme: Boolean = true,
  onBackdropToggle: () -> Unit = {},
  onFollowToggle: () -> Unit = {},
) {
  val scheme = MaterialTheme.colorScheme
  var settingsOpen by remember { mutableStateOf(false) }
  val list = rememberLazyListState()
  val title = displaySlug(slug)
  val view = LocalView.current
  val googleDoor = googleReady && email.isBlank() && spike.isBlank() && lines.isEmpty()
  val showSettings = settingsOpen && !compact
  val barColors = TopAppBarDefaults.topAppBarColors(
    containerColor = scheme.surface,
    titleContentColor = scheme.onSurface,
    actionIconContentColor = scheme.onSurfaceVariant,
    navigationIconContentColor = scheme.onSurface,
  )
  BackHandler(enabled = showSettings) { settingsOpen = false }
  var typing by remember(up, typingUntil) {
    mutableStateOf(up && typingUntil > System.currentTimeMillis())
  }
  LaunchedEffect(up, typingUntil) {
    if (!up || typingUntil <= 0L) {
      typing = false
      return@LaunchedEffect
    }
    val wait = typingUntil - System.currentTimeMillis()
    if (wait <= 0L) {
      typing = false
      return@LaunchedEffect
    }
    typing = true
    delay(wait)
    typing = false
  }
  var followNewest by remember { mutableStateOf(true) }
  LaunchedEffect(list) {
    snapshotFlow { list.isScrollInProgress }.collect { scrolling ->
      if (!scrolling) {
        followNewest = pinnedToNewest(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
      }
    }
  }
  val last = lines.lastOrNull()
  LaunchedEffect(last?.id) {
    if (last != null && shouldFollowNewest(followNewest, last.fromYou)) {
      list.scrollToItem(0)
    }
  }
  val keepScreen = typing || lines.any { it.pending }
  LaunchedEffect(keepScreen) {
    view.keepScreenOn = keepScreen
  }
  Scaffold(
    modifier = Modifier.fillMaxSize().imePadding(),
    containerColor = scheme.background,
    topBar = {
      if (showSettings) {
        TopAppBar(
          navigationIcon = {
            IconButton(
              onClick = { settingsOpen = false },
              modifier = Modifier.semantics { contentDescription = "close settings" },
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
          },
          title = { Text("Settings") },
          colors = barColors,
        )
      } else {
        TopAppBar(
          navigationIcon = {
            Box(modifier = Modifier.padding(start = 8.dp)) {
              KitAvatar(
                slug = slug,
                bytes = avatarBytes,
                size = 40.dp,
                editable = !googleDoor,
                onClick = onAvatar,
              )
            }
          },
          title = {
            Column {
              Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (up) scheme.tertiary else scheme.outline),
                )
                Text(
                  when {
                    !up -> "Offline"
                    typing -> "Live · typing…"
                    else -> "Live"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = if (up) scheme.tertiary else scheme.outline,
                )
              }
            }
          },
          actions = {
            IconButton(
              onClick = { settingsOpen = true },
              modifier = Modifier.semantics { contentDescription = "settings" },
            ) {
              Icon(Icons.Outlined.Settings, contentDescription = null)
            }
          },
          colors = barColors,
        )
      }
    },
    bottomBar = {
      if (!googleDoor && !showSettings) {
        CabCompose(
          disabled = false,
          placeholder = if (up) "Message $title" else "Waiting for mailbox…",
          gpsOn = gpsOn,
          catalog = catalog,
          onSend = onSend,
          onPhoto = onPhoto,
          onPin = onPin,
          onGpsToggle = onGpsToggle,
          onEngage = onEngage,
        )
      }
    },
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      if (showSettings) {
        CabSettings(
          origin = origin,
          slug = slug,
          spike = spike,
          email = email,
          googleReady = googleReady,
          cranes = cranes,
          sub = sub,
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
          authHint = authHint,
          signingIn = signingIn,
          onCarTest = onCarTest,
          photoSizeId = photoSizeId,
          onPhotoSize = onPhotoSize,
          backdropOn = backdropOn,
          followTheme = followTheme,
          onBackdropToggle = onBackdropToggle,
          onFollowToggle = onFollowToggle,
        )
      } else if (googleDoor) {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          KitAvatar(slug = slug, bytes = avatarBytes, size = 72.dp)
          Text(
            "Sign in with Google to talk.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center,
          )
          Button(
            onClick = onGoogle,
            enabled = !signingIn,
            modifier = Modifier.padding(top = 20.dp),
          ) {
            Text(if (signingIn) "Opening Google…" else "Continue with Google")
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
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 12.dp),
            )
          }
        }
      } else {
        Box(modifier = Modifier.fillMaxSize()) {
          if (!compact) {
            ChatBackdrop(backdropBytes)
          }
          Column(modifier = Modifier.fillMaxSize()) {
            if (dev && !compact) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(horizontal = 16.dp, vertical = 4.dp),
              ) {
                for (id in SAMPLE_IDS) {
                  AssistChip(onClick = { onSample(id) }, label = { Text(id) })
                }
              }
            }
          if (faceHint.isNotBlank()) {
            Text(
              faceHint,
              color = scheme.error,
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier
                .fillMaxWidth()
                .background(scheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            )
          }
          if (hint.isNotBlank()) {
            Text(
              hint,
              style = MaterialTheme.typography.bodySmall,
              color = scheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
          }
          if (lines.isEmpty()) {
            Column(
              modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 48.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              KitAvatar(slug = slug, bytes = avatarBytes, size = 72.dp)
              Text(
                if (up) {
                  "No messages yet. Say hello, or type / for commands."
                } else if (email.isNotBlank() || spike.isNotBlank()) {
                  "Connecting to the mailbox…"
                } else {
                  "Open Settings to connect. You are the operator; the name in the bar is the crane."
                },
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
              )
            }
          } else {
            LazyColumn(
              state = list,
              reverseLayout = true,
              modifier = Modifier.weight(1f).fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
              items(threadNewestFirst(lines), key = { it.id }) { line ->
                val mine = line.fromYou
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                ) {
                  Surface(
                    color = if (mine) scheme.primaryContainer else scheme.surfaceContainerHighest,
                    contentColor = if (mine) scheme.onPrimaryContainer else scheme.onSurface,
                    shape = if (mine) {
                      RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
                    } else {
                      RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                    },
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(0.82f),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      if (line.kind == "push") {
                        Text("Ping", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                      }
                      line.photo?.let { ChatPhoto(it) }
                      if (line.text.isNotBlank()) {
                        ChatMarkdown(text = line.text, draft = line.kind == "draft")
                      }
                      if (mine && line.pending) {
                        Text("sending", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                      }
                      if (mine && line.failed != null) {
                        Text(
                          line.failed,
                          style = MaterialTheme.typography.labelSmall,
                          color = scheme.error,
                          modifier = Modifier.semantics { contentDescription = "not sent" },
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
        }
      }
    }
  }
}

@Composable
private fun ChatBackdrop(bytes: ByteArray?) {
  val bmp = remember(bytes) {
    if (bytes == null || bytes.isEmpty()) {
      null
    } else {
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
  } ?: return
  Image(
    bitmap = bmp,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxSize().alpha(0.6f),
  )
}

@Composable
private fun ChatPhoto(url: String) {
  AsyncImage(
    model = url,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxWidth().heightIn(max = 192.dp).clip(RoundedCornerShape(12.dp)),
  )
}
