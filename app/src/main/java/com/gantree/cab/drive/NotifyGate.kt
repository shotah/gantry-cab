package com.gantree.cab.drive

import androidx.car.app.connection.CarConnection

/** Pendant `haptic.ts` visible-push buzz. */
const val PUSH_BUZZ_MS = 40L

/**
 * Kit HUNs and the Auto mouth. Post when a head unit is attached or the
 * phone thread is not resumed. Skip while the Cab Auto conversation
 * screen is open (the template is the mouth then). Maps / Assistant /
 * music keep HUNs because that screen is not started. `kind` is
 * `reply` / `push` only — same as [com.gantree.cab.mailbox.shouldSpeak].
 */
fun shouldPost(
  resumed: Boolean,
  carAttached: Boolean,
  kind: String?,
  threadVisible: Boolean = false,
): Boolean =
  (kind == "reply" || kind == "push") &&
    !threadVisible &&
    (carAttached || !resumed)

/** Visible ping on the phone when we skipped the toast. */
fun shouldBuzz(resumed: Boolean, carAttached: Boolean, kind: String?): Boolean =
  kind == "push" && resumed && !carAttached

fun carConnectionAttached(type: Int?): Boolean =
  type != null && type != CarConnection.CONNECTION_TYPE_NOT_CONNECTED
