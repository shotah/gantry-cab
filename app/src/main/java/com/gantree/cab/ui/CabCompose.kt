package com.gantree.cab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
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
  gpsHint: String,
  catalog: List<SlashCommand>,
  onSend: (String) -> Unit,
  onPhoto: () -> Unit,
  onPin: () -> Unit,
  onGpsToggle: () -> Unit,
  onEngage: () -> Unit,
) {
  val colors = LocalCabColors.current
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
  Column(modifier = Modifier.fillMaxWidth()) {
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
    if (attachOpen) {
      AttachPanel(
        disabled = disabled,
        gpsOn = gpsOn,
        gpsHint = gpsHint,
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
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 44.dp)
          .border(1.dp, colors.edge, RoundedCornerShape(12.dp))
          .clip(RoundedCornerShape(12.dp))
          .background(colors.canvas),
      ) {
        Column(
          modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 2.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          RoundIconButton(
            label = "emoji",
            enabled = !disabled,
            onClick = {
              emojiOpen = !emojiOpen
              emojiQuery = ""
              attachOpen = false
              slashDismissed = true
            },
          ) {
            Icon(
              Icons.Outlined.SentimentSatisfied,
              contentDescription = null,
              tint = if (emojiOpen) colors.mark else colors.muted,
              modifier = Modifier.size(16.dp),
            )
          }
          RoundIconButton(
            label = "attach",
            enabled = true,
            onClick = {
              attachOpen = !attachOpen
              emojiOpen = false
            },
            gpsDot = gpsOn,
          ) {
            Icon(
              Icons.Outlined.AttachFile,
              contentDescription = null,
              tint = colors.muted,
              modifier = Modifier.size(14.dp),
            )
          }
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .padding(start = 2.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        ) {
          if (draft.text.isEmpty()) {
            Text(placeholder, color = colors.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          BasicTextField(
            value = draft,
            onValueChange = { v ->
              onEngage()
              writeDraft(v.text, v.selection.end, v.selection)
            },
            modifier = Modifier
              .fillMaxWidth()
              .onFocusChanged { if (it.isFocused) onEngage() },
            enabled = !disabled,
            textStyle = TextStyle(
              color = if (disabled) colors.muted else colors.fg,
              fontSize = 14.sp,
              fontFamily = CabSans,
            ),
            cursorBrush = SolidColor(colors.accent),
            maxLines = 4,
          )
        }
      }
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .widthIn(min = 68.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(12.dp))
          .border(1.dp, colors.accentLine, RoundedCornerShape(12.dp))
          .background(colors.accentSoft)
          .alpha(if (canSend) 1f else 0.4f)
          .clickable(enabled = canSend, onClick = { sendDraft() })
          .padding(horizontal = 14.dp)
          .semantics { role = Role.Button },
      ) {
        Text("Send", color = colors.mark, fontSize = 14.sp)
      }
    }
  }
}

@Composable
private fun AttachPanel(
  disabled: Boolean,
  gpsOn: Boolean,
  gpsHint: String,
  onPhoto: () -> Unit,
  onCommands: () -> Unit,
  onGpsToggle: () -> Unit,
  onPin: () -> Unit,
) {
  val colors = LocalCabColors.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
      .semantics { contentDescription = "Attach" },
  ) {
    Column(
      modifier = Modifier
        .width(208.dp)
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
        .background(colors.panel)
        .padding(6.dp),
    ) {
      AttachRow("Photo", enabled = !disabled, onClick = onPhoto)
      AttachRow("Commands", enabled = !disabled, contentDescription = "harness commands", onClick = onCommands)
      if (gpsHint.isNotBlank()) {
        Text(
          gpsHint,
          color = colors.dim,
          fontSize = 11.sp,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
      AttachRow(
        "GPS",
        contentDescription = if (gpsOn) "GPS on" else "GPS off",
        trailing = {
          Text(if (gpsOn) "on" else "off", color = if (gpsOn) colors.ok else colors.muted, fontSize = 12.sp)
        },
        onClick = onGpsToggle,
      )
      AttachRow("Drop a silent pin", enabled = !disabled && gpsOn, contentDescription = "drop pin", onClick = onPin)
    }
  }
}

@Composable
private fun AttachRow(
  label: String,
  enabled: Boolean = true,
  contentDescription: String? = null,
  trailing: (@Composable () -> Unit)? = null,
  onClick: () -> Unit,
) {
  val colors = LocalCabColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .then(
        if (contentDescription != null) {
          Modifier.semantics { this.contentDescription = contentDescription }
        } else {
          Modifier
        },
      )
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 8.dp)
      .alpha(if (enabled) 1f else 0.4f),
  ) {
    Text(label, color = colors.body, fontSize = 12.sp)
    trailing?.invoke()
  }
}

@Composable
private fun EmojiPanel(query: String, onQuery: (String) -> Unit, onPick: (String) -> Unit) {
  val colors = LocalCabColors.current
  val choices = remember(query) { searchEmoji(query) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, colors.line, RoundedCornerShape(12.dp))
      .background(colors.panel)
      .padding(8.dp)
      .semantics { contentDescription = "Emoji" },
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .border(1.dp, colors.edge, RoundedCornerShape(8.dp))
        .background(colors.canvas)
        .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
      if (query.isEmpty()) {
        Text("Search or :shrug:", color = colors.muted, fontSize = 14.sp)
      }
      BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = TextStyle(color = colors.fg, fontSize = 14.sp, fontFamily = CabSans),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            val first = choices.firstOrNull()
            if (first != null) {
              onPick(first.emoji)
            }
          },
        ),
        modifier = Modifier
          .fillMaxWidth()
          .semantics { contentDescription = "Search emoji" },
      )
    }
    if (choices.isEmpty()) {
      Text("No matches", color = colors.dim, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(36.dp),
        modifier = Modifier.heightIn(max = 208.dp).padding(top = 6.dp),
      ) {
        items(choices, key = { it.name }) { entry ->
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
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

@Composable
private fun SlashPanel(matches: List<SlashCommand>, waiting: Boolean, onPick: (SlashCommand) -> Unit) {
  val colors = LocalCabColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, colors.accentLine, RoundedCornerShape(12.dp))
      .background(colors.panel)
      .heightIn(max = 240.dp)
      .verticalScroll(rememberScrollState())
      .semantics { contentDescription = "Harness commands" },
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
      Text("COMMANDS", color = colors.mark, fontSize = 11.sp)
      Text("These go to the crane, not the chat model.", color = colors.dim, fontSize = 11.sp)
    }
    HorizontalDivider(color = colors.line)
    if (waiting) {
      Text(
        "Waiting for the crane to publish commands.",
        color = colors.muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      )
    }
    for (cmd in matches) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onPick(cmd) }
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("/${cmd.name}", color = colors.mark, fontSize = 14.sp)
        Text(cmd.hint, color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
private fun RoundIconButton(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
  gpsDot: Boolean = false,
  content: @Composable () -> Unit,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(28.dp)
      .clickable(enabled = enabled, onClick = onClick)
      .semantics { contentDescription = label },
  ) {
    content()
    if (gpsDot) {
      val colors = LocalCabColors.current
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(7.dp)
          .clip(CircleShape)
          .background(colors.ok),
      )
    }
  }
}
