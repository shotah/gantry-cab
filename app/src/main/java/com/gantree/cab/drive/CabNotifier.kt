package com.gantree.cab.drive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.gantree.cab.MainActivity
import com.gantree.cab.R

object CabNotifier {
  const val CHANNEL = "kit"
  const val CONNECTED_ID = 7
  const val MESSAGE_ID = 42
  const val KEY_REPLY = "cab.reply"

  fun ensureChannel(ctx: Context) {
    val mgr = ctx.getSystemService(NotificationManager::class.java)
    val ch = NotificationChannel(CHANNEL, ctx.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_HIGH)
    ch.setShowBadge(true)
    mgr.createNotificationChannel(ch)
  }

  fun connected(ctx: Context): Notification {
    ensureChannel(ctx)
    val open = PendingIntent.getActivity(
      ctx,
      0,
      Intent(ctx, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return NotificationCompat.Builder(ctx, CHANNEL)
      .setSmallIcon(R.drawable.ic_stat_cab)
      .setContentTitle(ctx.getString(R.string.app_name))
      .setContentText(ctx.getString(R.string.notify_connected))
      .setContentIntent(open)
      .setOngoing(true)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()
  }

  fun kitMessage(ctx: Context, slug: String, text: String) {
    ensureChannel(ctx)
    val you = Person.Builder().setName("You").setKey("you").build()
    val kit = Person.Builder().setName(slug).setKey("kit").build()
    val style = NotificationCompat.MessagingStyle(you)
      .setConversationTitle(slug)
      .addMessage(text, System.currentTimeMillis(), kit)
    val reply = NotificationCompat.Action.Builder(
      R.drawable.ic_stat_cab,
      ctx.getString(R.string.reply_label),
      serviceIntent(ctx, ReplyService.ACTION_REPLY, slug),
    )
      .addRemoteInput(
        RemoteInput.Builder(KEY_REPLY).setLabel(ctx.getString(R.string.reply_label)).build(),
      )
      .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
      .setShowsUserInterface(false)
      .build()
    val read = NotificationCompat.Action.Builder(
      R.drawable.ic_stat_cab,
      ctx.getString(R.string.mark_read),
      serviceIntent(ctx, ReplyService.ACTION_READ, slug),
    )
      .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
      .setShowsUserInterface(false)
      .build()
    val n = NotificationCompat.Builder(ctx, CHANNEL)
      .setSmallIcon(R.drawable.ic_stat_cab)
      .setStyle(style)
      .addAction(reply)
      .addAction(read)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
      .build()
    ctx.getSystemService(NotificationManager::class.java).notify(MESSAGE_ID, n)
  }

  fun dismissKit(ctx: Context) {
    ctx.getSystemService(NotificationManager::class.java).cancel(MESSAGE_ID)
  }

  private fun serviceIntent(ctx: Context, action: String, slug: String): PendingIntent {
    val i = Intent(ctx, ReplyService::class.java).setAction(action).putExtra(ReplyService.EXTRA_SLUG, slug)
    return PendingIntent.getService(
      ctx,
      action.hashCode(),
      i,
      PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
  }
}
