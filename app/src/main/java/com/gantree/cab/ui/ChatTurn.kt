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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.gantree.cab.ChatLine
import com.gantree.cab.mailbox.REACTION_PALETTE
import com.gantree.cab.mailbox.REACT_HOLD_MS
import com.gantree.cab.mailbox.REACT_HOLD_SLOP_PX
import com.gantree.cab.mailbox.canCopy
import com.gantree.cab.mailbox.canHold
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One bubble. Hold opens the menu under it: "Copy text" for any bubble with
 * words (yours too, socket down too), the emoji rows only when [reactable].
 * [onClose] is the copy row closing the menu; a pick closes it via [onPick].
 */
@Composable
internal fun ChatTurn(
  line: ChatLine,
  reactable: Boolean,
  picking: Boolean,
  onOpenPicker: () -> Unit,
  onPick: (String) -> Unit,
  onClose: () -> Unit = {},
) {
  val scheme = MaterialTheme.colorScheme
  val clipboard = LocalClipboardManager.current
  val mine = line.fromYou
  val chip = line.reaction
  val showChip = chip != null && !picking
  val holdable = canHold(reactable, line.text)
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.82f)
        .padding(bottom = if (showChip) 12.dp else 0.dp),
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
          .fillMaxWidth()
          .testTag("chat-bubble")
          .then(if (holdable) Modifier.reactHold(onOpenPicker) else Modifier),
      ) {
        Column(
          modifier = Modifier.padding(
            start = 14.dp,
            top = 10.dp,
            end = 14.dp,
            bottom = if (showChip) 16.dp else 10.dp,
          ),
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
      if (chip != null && !picking) {
        ReactionChip(
          emoji = chip,
          fromYou = mine,
          onClick = if (reactable) onOpenPicker else null,
          modifier = Modifier
            .align(if (mine) Alignment.BottomEnd else Alignment.BottomStart)
            .offset(x = if (mine) (-8).dp else 8.dp, y = 10.dp)
            .zIndex(1f),
        )
      }
    }
    if (picking) {
      BubbleMenu(
        current = line.reaction,
        reactable = reactable,
        onCopy = if (canCopy(line.text)) {
          {
            clipboard.setText(AnnotatedString(line.text))
            onClose()
          }
        } else {
          null
        },
        onPick = onPick,
      )
    }
  }
}

@Composable
private fun ReactionChip(
  emoji: String,
  fromYou: Boolean,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val kitReacted = fromYou
  Surface(
    shape = RoundedCornerShape(50),
    color = if (kitReacted) scheme.surfaceContainerHighest else scheme.primaryContainer,
    contentColor = if (kitReacted) scheme.onSurface else scheme.onPrimaryContainer,
    border = BorderStroke(1.dp, if (kitReacted) scheme.outline else scheme.primary),
    shadowElevation = 2.dp,
    modifier = modifier
      .semantics { contentDescription = "reaction $emoji" }
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
  ) {
    Text(emoji, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 14.sp)
  }
}

/** Copy row first (the raw markdown goes on the clipboard), then the palette when the bubble takes a reaction. */
@Composable
private fun BubbleMenu(
  current: String?,
  reactable: Boolean,
  onCopy: (() -> Unit)?,
  onPick: (String) -> Unit,
) {
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
      if (onCopy != null) {
        TextButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
          Text("Copy text")
        }
      }
      for (row in if (reactable) REACTION_PALETTE.chunked(6) else emptyList()) {
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
