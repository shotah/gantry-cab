package com.gantree.cab.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gantree.cab.R
import com.gantree.cab.mailbox.displaySlug

@Composable
fun KitAvatar(
  slug: String,
  bytes: ByteArray?,
  size: Dp = 40.dp,
  editable: Boolean = false,
  onClick: () -> Unit = {},
) {
  val bmp = remember(bytes) {
    if (bytes == null || bytes.isEmpty()) {
      null
    } else {
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
  }
  val name = displaySlug(slug)
  val scheme = MaterialTheme.colorScheme
  val base = Modifier.size(size).clip(CircleShape).background(scheme.surfaceContainer)
  val face: @Composable (Modifier) -> Unit = { mod ->
    if (bmp != null) {
      Image(
        bitmap = bmp,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = mod.then(base),
      )
    } else {
      Box(modifier = mod.then(base), contentAlignment = Alignment.Center) {
        Icon(
          painterResource(R.drawable.ic_sentiment_satisfied),
          contentDescription = null,
          tint = scheme.onSurfaceVariant,
          modifier = Modifier.size(size * 0.55f),
        )
      }
    }
  }
  if (!editable) {
    face(Modifier)
  } else {
    face(
      Modifier
        .clickable(onClick = onClick)
        .semantics { contentDescription = "Change $name's photo" },
    )
  }
}
