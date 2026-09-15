package com.gantree.cab.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gantree.cab.mailbox.HoldState
import com.gantree.cab.mailbox.SpeakPhase
import com.gantree.cab.mailbox.holdLabel
import com.gantree.cab.mailbox.speakBarLabel

/**
 * Push-to-talk. Voice mode swaps the whole compose row for this one wide bar
 * (pendant `HoldToTalk`): press starts one utterance, release commits it,
 * sliding off before release throws it away. The words go to [onText]; nothing
 * is typed into compose and nothing is spoken here — the owner sends the turn.
 */
@Composable
fun HoldToTalk(
  disabled: Boolean,
  micGranted: Boolean,
  speaking: SpeakPhase,
  onMicAsk: () -> Unit,
  onHoldStart: () -> Unit,
  onText: (String) -> Unit,
  photo: String? = null,
  onPhotoClear: () -> Unit = {},
) {
  val scheme = MaterialTheme.colorScheme
  val context = LocalContext.current
  val haptics = LocalHapticFeedback.current
  var state by remember { mutableStateOf(HoldState.IDLE) }
  val text by rememberUpdatedState(onText)
  val askMic by rememberUpdatedState(onMicAsk)
  val holdStart by rememberUpdatedState(onHoldStart)
  val listener = remember(context) {
    HoldListener(
      context,
      onDone = { words ->
        if (state != HoldState.BLOCKED) {
          state = HoldState.IDLE
        }
        if (words.isNotEmpty()) {
          text(words)
        }
      },
      onBlocked = { state = HoldState.BLOCKED },
    )
  }
  DisposableEffect(listener) {
    onDispose { listener.abort() }
  }
  LaunchedEffect(micGranted) {
    if (micGranted && state == HoldState.BLOCKED) {
      state = HoldState.IDLE
    }
  }

  fun begin() {
    if (disabled || listener.listening) {
      return
    }
    if (!micGranted) {
      state = HoldState.BLOCKED
      askMic()
      return
    }
    holdStart()
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    if (!listener.begin()) {
      state = HoldState.BLOCKED
      return
    }
    state = HoldState.LISTENING
  }

  fun release() {
    if (!listener.listening) {
      return
    }
    state = HoldState.FINISHING
    listener.release()
  }

  fun cancel() {
    listener.abort()
  }

  val kitTalking = if (state == HoldState.IDLE) speakBarLabel(speaking) else ""
  val label = kitTalking.ifEmpty { holdLabel(state) }
  val listening = state == HoldState.LISTENING
  val lit = listening || kitTalking.isNotEmpty()
  val shape = RoundedCornerShape(28.dp)
  val pulse = if (listening) {
    val transition = rememberInfiniteTransition(label = "hold")
    val a by transition.animateFloat(
      initialValue = 1f,
      targetValue = 0.55f,
      animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
      label = "holdPulse",
    )
    a
  } else {
    1f
  }
  Surface(tonalElevation = 2.dp, color = scheme.surface) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
      if (photo != null) {
        StagedPhoto(photo = photo, onPhotoClear = onPhotoClear)
      }
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .alpha(if (disabled) 0.4f else pulse)
          .clip(shape)
          .background(
            when {
              listening -> scheme.primaryContainer
              lit -> scheme.tertiaryContainer
              else -> scheme.surfaceContainerHigh
            },
          )
          .border(1.dp, if (lit) scheme.tertiary else scheme.outlineVariant, shape)
          .semantics {
            contentDescription = "Hold to talk"
            role = Role.Button
            selected = listening
          }
          .pointerInput(disabled, micGranted) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              down.consume()
              begin()
              var settled = false
              while (!settled) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val inside = change.position.x in 0f..size.width.toFloat() &&
                  change.position.y in 0f..size.height.toFloat()
                change.consume()
                if (change.changedToUp()) {
                  if (inside) release() else cancel()
                  settled = true
                } else if (!change.pressed) {
                  cancel()
                  settled = true
                }
              }
              if (!settled) {
                cancel()
              }
            }
          },
      ) {
        Text(
          label,
          style = MaterialTheme.typography.titleSmall,
          color = when {
            listening -> scheme.onPrimaryContainer
            lit -> scheme.onTertiaryContainer
            state == HoldState.BLOCKED -> scheme.error
            else -> scheme.onSurface
          },
        )
      }
    }
  }
}
