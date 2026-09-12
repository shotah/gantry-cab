package com.gantree.cab.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

/** Pendant `KitAvatar` `xl`. Slot stays 40×80 so the bar does not grow. */
val HEADER_FACE_SIZE = 82.dp
val HEADER_FACE_SLOT_W = 80.dp
val HEADER_FACE_SLOT_H = 40.dp
val HEADER_FACE_NUDGE_X = (-2).dp
val HEADER_FACE_NUDGE_Y = (-4).dp
val HEADER_FACE_STROKE = 2.dp

@Composable
fun KitAvatar(
  slug: String,
  bytes: ByteArray?,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  stroke: Dp = 0.dp,
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
  val ring = if (stroke > 0.dp) {
    Modifier.border(stroke, scheme.outlineVariant, CircleShape)
  } else {
    Modifier
  }
  val base = Modifier.size(size).then(ring).clip(CircleShape).background(scheme.surfaceContainer)
  val face: @Composable (Modifier) -> Unit = { mod ->
    val paint = mod.then(base)
    if (bmp != null) {
      Image(
        bitmap = bmp,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = paint,
      )
    } else {
      Image(
        painter = painterResource(R.drawable.kit_face),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = paint,
      )
    }
  }
  if (!editable) {
    face(modifier)
  } else {
    face(
      modifier
        .clickable(onClick = onClick)
        .semantics { contentDescription = "Change $name's photo" },
    )
  }
}
