package com.gantree.cab.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gantree.cab.dev.sampleScene
import com.gantree.cab.drive.carRows

@Composable
fun PhoneShot(sampleId: String) {
  val scene = sampleScene(sampleId) ?: return
  CabTheme {
    CabScreen(
      origin = "http://10.0.2.2:3000",
      slug = scene.slug,
      spike = "",
      email = scene.email,
      googleReady = scene.id == "unsigned",
      up = scene.up,
      hint = scene.hint,
      lines = scene.lines,
      onOrigin = {},
      onSlug = {},
      onSpike = {},
      onConnect = {},
      onGoogle = {},
      onSignOut = {},
      onSend = {},
      compact = true,
    )
  }
}

@Composable
fun AutoShot(sampleId: String) {
  val scene = sampleScene(sampleId) ?: return
  CabTheme {
    CabCarPane(
      slug = scene.slug,
      rows = carRows(scene.lines, scene.slug, "No messages yet. Speak a reply when Kit pings you."),
    )
  }
}

@Preview(name = "phone-thread", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewPhoneThread() {
  PhoneShot("thread")
}

@Preview(name = "auto-thread", widthDp = 1024, heightDp = 576)
@Composable
private fun PreviewAutoThread() {
  AutoShot("thread")
}
