package com.gantree.cab.drive

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.RemoteInput
import com.gantree.cab.CabApp

class ReplyService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_REPLY -> {
      ACTION_REPLY -> {
        val text = RemoteInput.getResultsFromIntent(intent)
          ?.getCharSequence(CabNotifier.KEY_REPLY)
          ?.toString()
          ?.trim()
          .orEmpty()
        if (text.isNotEmpty()) {
          MailboxService.sendText(this, text)
        }
      }
      ACTION_READ -> CabNotifier.dismissKit(this)
    }
    stopSelf(startId)
    return START_NOT_STICKY
  }

  companion object {
    const val ACTION_REPLY = "com.gantree.cab.REPLY"
    const val ACTION_READ = "com.gantree.cab.READ"
    const val EXTRA_SLUG = "slug"
  }
}
