package com.gantree.cab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gantree.cab.R
import com.gantree.cab.mailbox.TODO_LIST_COMMAND
import com.gantree.cab.mailbox.TodoPriority
import com.gantree.cab.mailbox.TodoRow
import com.gantree.cab.mailbox.canTick
import com.gantree.cab.mailbox.pocketFooter
import com.gantree.cab.mailbox.settleTicked
import com.gantree.cab.mailbox.sortTodo
import com.gantree.cab.mailbox.tasksLabel
import com.gantree.cab.mailbox.todoAddText
import com.gantree.cab.mailbox.todoDoneCommand
import com.gantree.cab.mailbox.todoMeta
import com.gantree.cab.mailbox.todoPriority
import com.gantree.cab.mailbox.todoWords
import java.time.LocalDate

/**
 * Header check-square (pendant `TasksButton`). Painted whenever the list has
 * rows; the number is only tasks that changed since the drawer was last open.
 */
@Composable
fun TasksButton(shown: Boolean, changed: Int, onOpen: () -> Unit) {
  if (!shown) {
    return
  }
  val label = tasksLabel(changed)
  IconButton(
    onClick = onOpen,
    modifier = Modifier.semantics { contentDescription = label },
  ) {
    if (changed > 0) {
      BadgedBox(badge = { Badge { Text(changed.toString()) } }) {
        Icon(painterResource(R.drawable.ic_check_box), contentDescription = null)
      }
    } else {
      Icon(painterResource(R.drawable.ic_check_box), contentDescription = null)
    }
  }
}

/**
 * One checklist row per task: `!!` urgent first, then `!` high, then the
 * rest, oldest first inside each rank; the marker paints as a coloured tag
 * ahead of the words, not as the first word. The checkbox sends `/todo done
 * <id>` and the sheet **stays open** so several can be ticked; the row shows
 * ticked and struck through and will not send twice. Add and "Full list"
 * close the sheet so the answer is in view. A new list settles every tick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksSheet(
  rows: List<TodoRow>,
  onTick: (String) -> Unit,
  onAsk: (String) -> Unit,
  onDismiss: () -> Unit,
  today: LocalDate = LocalDate.now(),
) {
  val scheme = MaterialTheme.colorScheme
  val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var ticked by remember { mutableStateOf(setOf<Long>()) }
  var words by remember { mutableStateOf("") }
  // The next `todo` frame is the truth: done rows are gone, the rest un-tick.
  LaunchedEffect(rows) { ticked = settleTicked() }
  val ask: (String) -> Unit = { text ->
    onAsk(text)
    onDismiss()
  }
  val add: () -> Unit = {
    val text = todoAddText(words)
    if (text != null) {
      words = ""
      ask(text)
    }
  }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = scheme.surface) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Tasks", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = { ask(TODO_LIST_COMMAND) }) { Text("Full list") }
      }
      for (row in sortTodo(rows)) {
        val done = row.id in ticked
        val priority = todoPriority(row.text)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = done,
            onCheckedChange = {
              if (canTick(row.id, ticked)) {
                ticked = ticked + row.id
                onTick(todoDoneCommand(row.id))
              }
            },
            modifier = Modifier.semantics { contentDescription = "done ${row.slug}" },
          )
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              priority.tag?.let { tag ->
                Text(
                  if (priority == TodoPriority.URGENT) "!!" else "!",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (priority == TodoPriority.URGENT) scheme.error else scheme.tertiary,
                  modifier = Modifier.semantics { contentDescription = tag },
                )
              }
              Text(
                todoWords(row.text),
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) scheme.onSurfaceVariant else scheme.onSurface,
              )
            }
            Text(
              todoMeta(row, today),
              style = MaterialTheme.typography.labelSmall,
              color = scheme.onSurfaceVariant,
            )
          }
        }
      }
      pocketFooter(rows.size)?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = words,
          onValueChange = { words = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text("in your words") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(onSend = { add() }),
        )
        TextButton(onClick = add) { Text("Add") }
      }
    }
  }
}
