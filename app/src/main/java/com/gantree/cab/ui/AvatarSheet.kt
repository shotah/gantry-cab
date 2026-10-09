package com.gantree.cab.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gantree.cab.mailbox.avatarShareUri
import com.gantree.cab.mailbox.displaySlug
import com.gantree.cab.mailbox.shareImageIntent

/**
 * Tap the header face and this comes up: the face large, then Copy (image
 * on the clipboard), Share (system sheet), Replace (the photo picker the
 * tap used to open directly). The crane paints its own face now, so the
 * phone needs a way to get it back out. Copy and Share are only offered
 * when a face has been set; the bundled default is not anyone's work.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSheet(
  slug: String,
  bytes: ByteArray?,
  onReplace: () -> Unit,
  onDismiss: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val ctx = LocalContext.current
  val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var copied by remember { mutableStateOf(false) }
  val name = displaySlug(slug)
  val face = bytes?.takeIf { it.isNotEmpty() }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = scheme.surface) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      KitAvatar(slug = slug, bytes = face, size = 160.dp, stroke = HEADER_FACE_STROKE)
      Text("$name's photo", style = MaterialTheme.typography.titleLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (face != null) {
          TextButton(
            onClick = {
              val uri = avatarShareUri(ctx, face)
              ctx.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newUri(ctx.contentResolver, "$name's photo", uri))
              copied = true
            },
          ) {
            Text(if (copied) "Copied" else "Copy")
          }
          TextButton(
            onClick = {
              ctx.startActivity(shareImageIntent(avatarShareUri(ctx, face)))
              onDismiss()
            },
          ) {
            Text("Share")
          }
        }
        Button(
          onClick = {
            onDismiss()
            onReplace()
          },
        ) {
          Text("Replace")
        }
      }
    }
  }
}
