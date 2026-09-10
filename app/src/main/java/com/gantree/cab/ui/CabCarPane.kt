package com.gantree.cab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gantree.cab.drive.CarRow

private val CarBg = Color(0xFF121212)
private val CarLine = Color(0xFF2A2A2A)
private val CarTitle = Color(0xFFF2F2F2)
private val CarBody = Color(0xFFB8B8B8)
private val CarAccent = Color(0xFFF07848)

/** DHU / Auto stand-in for shots. Live Auto uses ConversationItem + host Reply. */
@Composable
fun CabCarPane(
  slug: String,
  rows: List<CarRow>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarBg)
      .padding(horizontal = 48.dp, vertical = 28.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(slug.ifBlank { "cab" }, color = CarAccent, fontSize = 28.sp, fontFamily = CabSans)
    HorizontalDivider(color = CarLine)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      items(rows, key = { "${it.title}-${it.text}" }) { row ->
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(row.title, color = CarTitle, style = MaterialTheme.typography.titleLarge)
          row.text?.takeIf { it.isNotBlank() }?.let { Text(it, color = CarBody, style = MaterialTheme.typography.bodyLarge) }
        }
      }
    }
  }
}
