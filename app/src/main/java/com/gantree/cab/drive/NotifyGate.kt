package com.gantree.cab.drive

import android.app.NotificationManager
import android.content.Intent
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

/**
 * Auto cannot launch a sideloaded Cab, so the socket must already be up
 * when the phone meets the head unit. Reboot and APK update are the two
 * moments that silently take it down; both are allowed to start a
 * `specialUse` foreground service. Only when there is a credential.
 */
fun bootShouldListen(action: String?, signedIn: Boolean): Boolean =
  signedIn &&
    (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED)

/**
 * Settings → **Test car voice** would be a lie if Android drops the card
 * first. Auto heads-up (and reads) `IMPORTANCE_HIGH` only; the channel
 * starts there, a user can lower it. `null` = channel not created yet.
 */
fun carTestBlocked(notificationsEnabled: Boolean, channelImportance: Int?): Boolean =
  !notificationsEnabled ||
    (channelImportance != null && channelImportance < NotificationManager.IMPORTANCE_HIGH)

/**
 * Body of the Kit notification posted by Settings → **Test car voice**.
 * Goes through [CabNotifier.kitMessage] like a real reply, so Android
 * Auto reads it aloud if the sideload is allowed (Unknown sources) and
 * the phone is projecting. The text itself says what the phone sees.
 */
fun carCheckText(carAttached: Boolean): String =
  if (carAttached) {
    "Car check from Cab. Android Auto is attached. If you hear this, Kit will be read aloud."
  } else {
    "Car check from Cab. The phone does not see Android Auto. Plug into the car, then tap Test again."
  }
