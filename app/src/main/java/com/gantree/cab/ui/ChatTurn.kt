package com.gantree.cab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gantree.cab.ChatLine
import com.gantree.cab.mailbox.REACTION_PALETTE
import com.gantree.cab.mailbox.REACT_HOLD_MS
import com.gantree.cab.mailbox.REACT_HOLD_SLOP_PX
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun ChatTurn(
  line: ChatLine,
  reactable: Boolean,
  picking: Boolean,
  onOpenPicker: () -> Unit,
  onPick: (String) -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val mine = line.fromYou
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
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
      modifier = Modifier
        .fillMaxWidth(0.82f)
        .then(if (reactable) Modifier.reactHold(onOpenPicker) else Modifier),
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
    val chip = line.reaction
    if (chip != null && !picking) {
      ReactionChip(
        emoji = chip,
        fromYou = mine,
        onClick = if (reactable) onOpenPicker else null,
      )
    }
    if (picking) {
      ReactionPicker(current = line.reaction, onPick = onPick)
    }
  }
}

@Composable
private fun ReactionChip(emoji: String, fromYou: Boolean, onClick: (() -> Unit)?) {
  val scheme = MaterialTheme.colorScheme
  val kitReacted = fromYou
  Surface(
    shape = RoundedCornerShape(50),
    color = if (kitReacted) scheme.surfaceContainerHighest else scheme.primaryContainer,
    contentColor = if (kitReacted) scheme.onSurface else scheme.onPrimaryContainer,
    border = BorderStroke(1.dp, if (kitReacted) scheme.outline else scheme.primary),
    modifier = Modifier
      .padding(top = 4.dp)
      .semantics { contentDescription = "reaction $emoji" }
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
  ) {
    Text(emoji, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 14.sp)
  }
}

@Composable
private fun ReactionPicker(current: String?, onPick: (String) -> Unit) {
  val scheme = MaterialTheme.colorScheme
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = scheme.surface,
    border = BorderStroke(1.dp, scheme.outline),
    tonalElevation = 3.dp,
    modifier = Modifier
      .padding(top = 4.dp)
      .semantics { contentDescription = "React" },
  ) {
    Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      for (row in REACTION_PALETTE.chunked(6)) {
        Row {
          for (emoji in row) {
            val selected = current == emoji
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onPick(emoji) }
                .semantics { contentDescription = "React $emoji" },
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) scheme.surfaceContainerHighest else scheme.surface,
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                  Text(emoji, fontSize = 18.sp)
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
private fun ChatPhoto(url: String) {
  AsyncImage(
    model = url,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxWidth().heightIn(max = 192.dp).clip(RoundedCornerShape(12.dp)),
  )
}

private fun Modifier.reactHold(onHold: () -> Unit): Modifier = pointerInput(onHold) {
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val origin = down.position
    val held = withTimeoutOrNull(REACT_HOLD_MS) {
      while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull() ?: break
        if (change.changedToUp() || !change.pressed) {
          break
        }
        if ((change.position - origin).getDistance() > REACT_HOLD_SLOP_PX) {
          break
        }
      }
      false
    }
    if (held == null) {
      onHold()
    }
  }
}
