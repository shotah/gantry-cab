package com.gantree.cab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gantree.cab.R
import com.gantree.cab.mailbox.ASK_AIMS_REPORT
import com.gantree.cab.mailbox.ASK_AIMS_RUBRIC
import com.gantree.cab.mailbox.Aim
import com.gantree.cab.mailbox.AimDay
import com.gantree.cab.mailbox.AimWeek
import com.gantree.cab.mailbox.AimsBoard
import com.gantree.cab.mailbox.askAim
import com.gantree.cab.mailbox.goalsLabel
import com.gantree.cab.mailbox.linkLine
import com.gantree.cab.mailbox.signed
import com.gantree.cab.mailbox.statsLine
import com.gantree.cab.mailbox.trendLine
import kotlin.math.abs

/**
 * Header target with the aim count (pendant `GoalsButton`). Only painted when
 * the board has rows, so a room with no ledger never grows a dead button.
 */
@Composable
fun GoalsButton(count: Int, onOpen: () -> Unit) {
  if (count <= 0) {
    return
  }
  val label = goalsLabel(count)
  IconButton(
    onClick = onOpen,
    modifier = Modifier.semantics { contentDescription = label },
  ) {
    Icon(painterResource(R.drawable.ic_target), contentDescription = null)
  }
}

/**
 * One card per aim, the stamp line as `[aims]` carries it, then the cross-aim
 * lines. Every button is a turn the human can see: `/aims <area>`, `/aims`,
 * `/aims rubric`. The sheet closes so the answer is in view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsSheet(board: AimsBoard, onAsk: (String) -> Unit, onDismiss: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val ask: (String) -> Unit = { text ->
    onAsk(text)
    onDismiss()
  }
  // Whole board at once; a half sheet would fold the stamp line under the grid.
  val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = scheme.surface) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Goals", style = MaterialTheme.typography.titleLarge)
        Row {
          TextButton(onClick = { ask(ASK_AIMS_REPORT) }) { Text("Full report") }
          TextButton(onClick = { ask(ASK_AIMS_RUBRIC) }) { Text("Rubric") }
        }
      }
      for (aim in board.aims) {
        AimCard(aim = aim, onAsk = { ask(askAim(aim.area)) })
      }
      for (link in board.links) {
        Text(
          linkLine(link),
          style = MaterialTheme.typography.bodySmall,
          color = scheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun AimCard(aim: Aim, onAsk: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(aim.area, style = MaterialTheme.typography.titleMedium)
      Text(
        signed(aim.rating30),
        style = MaterialTheme.typography.titleMedium,
        color = scoreColor(aim.rating30, scheme.tertiary, scheme.error, scheme.onSurfaceVariant),
      )
    }
    Text(aim.sentence, style = MaterialTheme.typography.bodyMedium)
    DayGrid(aim.days)
    Text(statsLine(aim), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
    if (aim.weeks.isNotEmpty()) {
      WeekStrip(aim.weeks)
    }
    val trend = trendLine(aim)
    if (trend.isNotEmpty()) {
      Text(trend, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
    }
    TextButton(
      onClick = onAsk,
      modifier = Modifier.semantics { contentDescription = "Ask Kit about ${aim.area}" },
    ) {
      Text("Ask Kit about ${aim.area}")
    }
  }
}

/** Sign is the hue, magnitude the weight, an eventless day an outline; the score under each cell. */
@Composable
private fun DayGrid(days: List<AimDay>) {
  if (days.isEmpty()) {
    return
  }
  val scheme = MaterialTheme.colorScheme
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    for (d in days) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val fill = scoreColor(d.score.toDouble(), scheme.tertiary, scheme.error, scheme.outline)
        val cell = Modifier.size(16.dp)
        if (d.events.isEmpty()) {
          Box(cell.border(1.dp, scheme.outline, RoundedCornerShape(3.dp)))
        } else {
          Box(cell.alpha(dayWeight(d.score)).background(fill, RoundedCornerShape(3.dp)))
        }
        Text(signed(d.score), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
      }
    }
  }
}

/** One bar per Sunday-start bucket around a zero line, oldest left. */
@Composable
private fun WeekStrip(weeks: List<AimWeek>) {
  val scheme = MaterialTheme.colorScheme
  val half = 18.dp
  Row(
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.height(half * 2),
  ) {
    for (w in weeks) {
      val h = half * (abs(w.mean).coerceAtMost(3.0) / 3.0).toFloat()
      val fill = scoreColor(w.mean, scheme.tertiary, scheme.error, scheme.outline)
      Column(modifier = Modifier.width(8.dp).height(half * 2)) {
        Box(modifier = Modifier.height(if (w.mean > 0) half - h else half))
        Box(
          modifier = Modifier
            .width(8.dp)
            .height(if (w.mean == 0.0) 1.dp else h)
            .background(fill, RoundedCornerShape(2.dp)),
        )
      }
    }
  }
}

/** Positive → up hue, negative → against hue, zero → neutral. */
fun scoreColor(score: Double, up: Color, against: Color, zero: Color): Color = when {
  score > 0 -> up
  score < 0 -> against
  else -> zero
}

/** `|score|` 1…3 → 0.45…1.0 so a light day reads lighter than a strong one. */
fun dayWeight(score: Int): Float {
  val n = abs(score).coerceIn(0, 3)
  return if (n == 0) 0.3f else 0.45f + 0.275f * (n - 1)
}
