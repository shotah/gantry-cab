package com.gantree.cab.drive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gantree.cab.CabApp

/** Brings the mailbox socket back after a reboot or an APK update. See [bootShouldListen]. */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val app = context.applicationContext as CabApp
    if (bootShouldListen(intent?.action, app.prefs.signedIn)) {
      MailboxService.start(context)
    }
  }
}
