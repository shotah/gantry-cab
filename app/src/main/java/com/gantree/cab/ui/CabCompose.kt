package com.gantree.cab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.applyEmoji
import com.gantree.cab.mailbox.matchSlash
import com.gantree.cab.mailbox.searchEmoji
import com.gantree.cab.mailbox.slashInsert
import com.gantree.cab.mailbox.slashToken

@Composable
fun CabCompose(
  disabled: Boolean,
  placeholder: String,
  gpsOn: Boolean,
  catalog: List<SlashCommand>,
  onSend: (String) -> Unit,
  onPhoto: () -> Unit,
  onPin: () -> Unit,
  onGpsToggle: () -> Unit,
  onEngage: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  var draft by remember { mutableStateOf(TextFieldValue("")) }
  var emojiOpen by remember { mutableStateOf(false) }
  var emojiQuery by remember { mutableStateOf("") }
  var attachOpen by remember { mutableStateOf(false) }
  var slashDismissed by remember { mutableStateOf(false) }
  val matches = matchSlash(draft.text, catalog)
  val waiting = catalog.isEmpty() && slashToken(draft.text) != null
  val slashOpen = !disabled && !slashDismissed && !emojiOpen && (matches.isNotEmpty() || waiting)

  fun writeDraft(raw: String, cursor: Int, selection: TextRange? = null) {
    val next = applyEmoji(raw, cursor, "type")
    draft = if (next.text == raw) {
      TextFieldValue(raw, selection ?: TextRange(cursor))
    } else {
      TextFieldValue(next.text, TextRange(next.cursor))
    }
    slashDismissed = false
  }

  fun insertEmoji(emoji: String) {
    val start = draft.selection.start.coerceIn(0, draft.text.length)
    val end = draft.selection.end.coerceIn(0, draft.text.length)
    val next = draft.text.substring(0, start) + emoji + draft.text.substring(end)
    val caret = start + emoji.length
    draft = TextFieldValue(next, TextRange(caret))
    emojiQuery = ""
  }

  fun sendDraft() {
    val t = applyEmoji(draft.text, draft.text.length, "send").text.trim()
    if (t.isEmpty() || disabled) {
      return
    }
    emojiOpen = false
    attachOpen = false
    draft = TextFieldValue("")
    onSend(t)
  }

  val canSend = !disabled && draft.text.isNotBlank()
  Surface(tonalElevation = 2.dp, color = scheme.surface) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
      if (emojiOpen) {
        EmojiPanel(
          query = emojiQuery,
          onQuery = { emojiQuery = it },
          onPick = { insertEmoji(it) },
        )
      }
      if (slashOpen) {
        SlashPanel(
          matches = matches,
          waiting = waiting,
          onPick = { cmd ->
            val next = slashInsert(cmd)
            draft = TextFieldValue(next, TextRange(next.length))
          },
        )
      }
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        TextField(
          value = draft,
          onValueChange = { v ->
            onEngage()
            writeDraft(v.text, v.selection.end, v.selection)
          },
          modifier = Modifier
            .weight(1f)
            .onFocusChanged { if (it.isFocused) onEngage() },
          enabled = !disabled,
          placeholder = {
            Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          leadingIcon = {
            Box {
              IconButton(
                onClick = {
                  attachOpen = !attachOpen
                  emojiOpen = false
                },
                modifier = Modifier.size(36.dp).semantics { contentDescription = "attach" },
              ) {
                BadgedBox(
                  badge = {
                    if (gpsOn) {
                      Badge(containerColor = scheme.tertiary)
                    }
                  },
                ) {
                  Icon(
                    Icons.Outlined.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              DropdownMenu(expanded = attachOpen, onDismissRequest = { attachOpen = false }) {
                AttachMenu(
                  disabled = disabled,
                  gpsOn = gpsOn,
                  onPhoto = {
                    attachOpen = false
                    onPhoto()
                  },
                  onCommands = {
                    attachOpen = false
                    emojiOpen = false
                    slashDismissed = false
                    if (!draft.text.startsWith("/") || draft.text.any { it.isWhitespace() }) {
                      draft = TextFieldValue("/", TextRange(1))
                    }
                  },
                  onGpsToggle = onGpsToggle,
                  onPin = {
                    attachOpen = false
                    onPin()
                  },
                )
              }
            }
          },
          trailingIcon = {
            IconButton(
              enabled = !disabled,
              onClick = {
                emojiOpen = !emojiOpen
                emojiQuery = ""
                attachOpen = false
                slashDismissed = true
              },
              modifier = Modifier.size(36.dp).semantics { contentDescription = "emoji" },
            ) {
              Icon(
                Icons.Outlined.SentimentSatisfied,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (emojiOpen) scheme.primary else scheme.onSurfaceVariant,
              )
            }
          },
          shape = RoundedCornerShape(28.dp),
          colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
          ),
          maxLines = 4,
        )
        FilledIconButton(onClick = { sendDraft() }, enabled = canSend) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
      }
    }
  }
}

@Composable
private fun AttachMenu(
  disabled: Boolean,
  gpsOn: Boolean,
  onPhoto: () -> Unit,
  onCommands: () -> Unit,
  onGpsToggle: () -> Unit,
  onPin: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  Column(
    modifier = Modifier
      .width(196.dp)
      .semantics { contentDescription = "Attach" },
  ) {
    AttachRow(
      label = "Photo",
      icon = Icons.Outlined.Photo,
      enabled = !disabled,
      onClick = onPhoto,
    )
    AttachRow(
      label = "Commands",
      icon = Icons.Outlined.Code,
      enabled = !disabled,
      onClick = onCommands,
      modifier = Modifier.semantics { contentDescription = "harness commands" },
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
        .clickable(onClick = onGpsToggle)
        .padding(horizontal = 12.dp)
        .semantics { contentDescription = if (gpsOn) "GPS on" else "GPS off" },
    ) {
      Icon(
        Icons.Outlined.LocationOn,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = scheme.onSurfaceVariant,
      )
      Text(
        "Location",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f).padding(start = 12.dp),
      )
      Switch(checked = gpsOn, onCheckedChange = { onGpsToggle() })
    }
    AttachRow(
      label = "Drop a pin",
      icon = Icons.Outlined.AddLocationAlt,
      enabled = !disabled && gpsOn,
      onClick = onPin,
      modifier = Modifier.semantics { contentDescription = "drop pin" },
    )
  }
}

@Composable
private fun AttachRow(
  label: String,
  icon: ImageVector,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .height(40.dp)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 12.dp),
  ) {
    Icon(
      icon,
      contentDescription = null,
      modifier = Modifier.size(20.dp),
      tint = if (enabled) scheme.onSurfaceVariant else scheme.onSurface.copy(alpha = 0.38f),
    )
    Text(
      label,
      style = MaterialTheme.typography.bodyMedium,
      color = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.38f),
      modifier = Modifier.padding(start = 12.dp),
    )
  }
}

@Composable
private fun EmojiPanel(query: String, onQuery: (String) -> Unit, onPick: (String) -> Unit) {
  val choices = remember(query) { searchEmoji(query) }
  val scheme = MaterialTheme.colorScheme
  Surface(
    shape = MaterialTheme.shapes.medium,
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 4.dp)
      .semantics { contentDescription = "Emoji" },
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      BasicTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier
          .fillMaxWidth()
          .height(36.dp)
          .semantics { contentDescription = "Search emoji" },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            val first = choices.firstOrNull()
            if (first != null) {
              onPick(first.emoji)
            }
          },
        ),
        decorationBox = { inner ->
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().height(36.dp),
          ) {
            Box(
              contentAlignment = Alignment.CenterStart,
              modifier = Modifier.padding(horizontal = 12.dp),
            ) {
              if (query.isEmpty()) {
                Text(
                  "Search or :shrug:",
                  style = MaterialTheme.typography.bodySmall,
                  color = scheme.onSurfaceVariant,
                )
              }
              inner()
            }
          }
        },
      )
      if (choices.isEmpty()) {
        Text(
          "No matches",
          style = MaterialTheme.typography.bodySmall,
          color = scheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(36.dp),
          modifier = Modifier.heightIn(max = 80.dp).padding(top = 4.dp),
        ) {
          items(choices, key = { it.name }) { entry ->
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(36.dp)
                .clickable { onPick(entry.emoji) }
                .semantics { contentDescription = ":${entry.name}:" },
            ) {
              Text(entry.emoji, fontSize = 20.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SlashPanel(matches: List<SlashCommand>, waiting: Boolean, onPick: (SlashCommand) -> Unit) {
  val scheme = MaterialTheme.colorScheme
  Surface(
    shape = MaterialTheme.shapes.large,
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
      .heightIn(max = 240.dp)
      .verticalScroll(rememberScrollState())
      .semantics { contentDescription = "Harness commands" },
  ) {
    Column {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Commands", style = MaterialTheme.typography.titleSmall)
        Text(
          "These go to the crane, not the chat model.",
          style = MaterialTheme.typography.bodySmall,
          color = scheme.onSurfaceVariant,
        )
      }
      HorizontalDivider()
      if (waiting) {
        Text(
          "Waiting for the crane to publish commands.",
          style = MaterialTheme.typography.bodySmall,
          color = scheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
      }
      for (cmd in matches) {
        ListItem(
          headlineContent = { Text("/${cmd.name}") },
          supportingContent = {
            Text(cmd.hint, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          modifier = Modifier.clickable { onPick(cmd) },
        )
      }
    }
  }
}
