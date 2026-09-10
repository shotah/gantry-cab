package com.gantree.cab.drive

import androidx.car.app.connection.CarConnection

/** Pendant `haptic.ts` visible-push buzz. */
const val PUSH_BUZZ_MS = 40L

/**
 * Kit HUNs and the Auto mouth. Post when a head unit is attached or the
 * phone thread is not resumed. `kind` is `reply` / `push` only — same as
 * [com.gantree.cab.mailbox.shouldSpeak].
 */
fun shouldPost(resumed: Boolean, carAttached: Boolean, kind: String?): Boolean =
  (kind == "reply" || kind == "push") && (carAttached || !resumed)

/** Visible ping on the phone when we skipped the toast. */
fun shouldBuzz(resumed: Boolean, carAttached: Boolean, kind: String?): Boolean =
  kind == "push" && resumed && !carAttached

fun carConnectionAttached(type: Int?): Boolean =
  type != null && type != CarConnection.CONNECTION_TYPE_NOT_CONNECTED
