package com.gantree.cab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Bottom,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .border(1.dp, colors.line, RoundedCornerShape(12.dp))
          .clip(RoundedCornerShape(12.dp))
          .background(colors.canvas),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
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
            ) { EmojiFace(if (emojiOpen) colors.mark else colors.muted) }
            Box {
              RoundIconButton(
                label = "attach",
                enabled = true,
                onClick = {
                  attachOpen = !attachOpen
                  emojiOpen = false
                },
                gpsDot = gpsOn,
              ) { ClipIcon(colors.muted) }
              DropdownMenu(
                expanded = attachOpen,
                onDismissRequest = { attachOpen = false },
                modifier = Modifier.semantics { contentDescription = "Attach" },
              ) {
                DropdownMenuItem(
                  text = { Text("Photo") },
                  enabled = !disabled,
                  onClick = {
                    attachOpen = false
                    onPhoto()
                  },
                )
                DropdownMenuItem(
                  text = { Text("Commands") },
                  enabled = !disabled,
                  modifier = Modifier.semantics { contentDescription = "harness commands" },
                  onClick = {
                    attachOpen = false
                    emojiOpen = false
                    slashDismissed = false
                    if (!draft.text.startsWith("/") || draft.text.any { it.isWhitespace() }) {
                      draft = TextFieldValue("/", TextRange(1))
                    }
                  },
                )
                if (gpsHint.isNotBlank()) {
                  DropdownMenuItem(
                    text = { Text(gpsHint, fontSize = 11.sp, color = colors.dim) },
                    onClick = {},
                    enabled = false,
                  )
                }
                DropdownMenuItem(
                  text = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text("GPS")
                      Text(if (gpsOn) "on" else "off", color = if (gpsOn) colors.ok else colors.muted)
                    }
                  },
                  onClick = onGpsToggle,
                  modifier = Modifier.semantics { contentDescription = if (gpsOn) "GPS on" else "GPS off" },
                )
                DropdownMenuItem(
                  text = { Text("Drop a silent pin") },
                  enabled = !disabled && gpsOn,
                  modifier = Modifier.semantics { contentDescription = "drop pin" },
                  onClick = {
                    attachOpen = false
                    onPin()
                  },
                )
              }
            }
          }
          OutlinedTextField(
            value = draft,
            onValueChange = { v ->
              onEngage()
              writeDraft(v.text, v.selection.end, v.selection)
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            enabled = !disabled,
            colors = composeFieldColors(),
          )
        }
      }
      Button(
        onClick = {
          val t = applyEmoji(draft.text, draft.text.length, "send").text.trim()
          if (t.isEmpty() || disabled) {
            return@Button
          }
          emojiOpen = false
          draft = TextFieldValue("")
          onSend(t)
        },
        enabled = !disabled && draft.text.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.canvas),
      ) {
        Text("Send")
      }
    }
  }
}

@Composable
private fun EmojiPanel(query: String, onQuery: (String) -> Unit, onPick: (String) -> Unit) {
  val colors = LocalCabColors.current
  val choices = remember(query) { searchEmoji(query) }
  Surface(
    color = colors.panel,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
      .border(1.dp, colors.line, RoundedCornerShape(12.dp))
      .semantics { contentDescription = "Emoji" },
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text("Search or :shrug:") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors(),
      )
      if (choices.isEmpty()) {
        Text("No matches", color = colors.dim, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(36.dp),
          modifier = Modifier.heightIn(max = 200.dp).padding(top = 6.dp),
        ) {
          items(choices, key = { it.name }) { entry ->
            Text(
              entry.emoji,
              fontSize = 20.sp,
              modifier = Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onPick(entry.emoji) }
                .padding(6.dp)
                .semantics { contentDescription = ":${entry.name}:" },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SlashPanel(matches: List<SlashCommand>, waiting: Boolean, onPick: (SlashCommand) -> Unit) {
  val colors = LocalCabColors.current
  Surface(
    color = colors.panel,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
      .border(1.dp, colors.accentLine, RoundedCornerShape(12.dp))
      .semantics { contentDescription = "Harness commands" },
  ) {
    Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("COMMANDS", color = colors.mark, fontSize = 11.sp)
        Text("These go to the crane, not the chat model.", color = colors.dim, fontSize = 11.sp)
      }
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
        ) {
          Text("/${cmd.name}", color = colors.mark, fontSize = 14.sp)
          Text(cmd.hint, color = colors.muted, fontSize = 12.sp, maxLines = 1)
        }
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
      .clip(CircleShape)
      .clickable(enabled = enabled, onClick = onClick)
      .semantics { contentDescription = label },
  ) {
    content()
    if (gpsDot) {
      val colors = LocalCabColors.current
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
          .size(6.dp)
          .clip(CircleShape)
          .background(colors.ok),
      )
    }
  }
}

@Composable
private fun EmojiFace(color: androidx.compose.ui.graphics.Color) {
  androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
    drawCircle(color = color, style = stroke)
    drawCircle(color = color, radius = 1.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.42f))
    drawCircle(color = color, radius = 1.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.42f))
    drawArc(
      color = color,
      startAngle = 20f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.42f),
      size = androidx.compose.ui.geometry.Size(size.width * 0.44f, size.height * 0.4f),
      style = stroke,
    )
  }
}

@Composable
private fun ClipIcon(color: androidx.compose.ui.graphics.Color) {
  androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
      width = 1.5.dp.toPx(),
      cap = androidx.compose.ui.graphics.StrokeCap.Round,
      join = androidx.compose.ui.graphics.StrokeJoin.Round,
    )
    val path = androidx.compose.ui.graphics.Path().apply {
      moveTo(size.width * 0.32f, size.height * 0.52f)
      lineTo(size.width * 0.62f, size.height * 0.22f)
      cubicTo(size.width * 0.78f, size.height * 0.06f, size.width * 0.98f, size.height * 0.26f, size.width * 0.82f, size.height * 0.42f)
      lineTo(size.width * 0.45f, size.height * 0.78f)
    }
    drawPath(path, color = color, style = stroke)
  }
}
