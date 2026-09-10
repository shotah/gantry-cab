package com.gantree.cab.drive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.gantree.cab.MainActivity
import com.gantree.cab.R

object CabNotifier {
  const val CHANNEL = "kit"
  const val CONNECTED_ID = 7
  const val MESSAGE_ID = 42
  const val KEY_REPLY = "cab.reply"

  @Volatile
  private var historySlug = ""
  private var history = emptyList<KitTurn>()

  fun conversationId(slug: String): String = "cab-$slug"

  fun ensureChannel(ctx: Context) {
    val mgr = ctx.getSystemService(NotificationManager::class.java)
    val ch = NotificationChannel(CHANNEL, ctx.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_HIGH)
    ch.setShowBadge(true)
    ch.enableVibration(true)
    mgr.createNotificationChannel(ch)
  }

  fun connected(ctx: Context): Notification {
    ensureChannel(ctx)
    return NotificationCompat.Builder(ctx, CHANNEL)
      .setSmallIcon(R.drawable.ic_stat_cab)
      .setContentTitle(ctx.getString(R.string.app_name))
      .setContentText(ctx.getString(R.string.notify_connected))
      .setContentIntent(openApp(ctx))
      .setOngoing(true)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .build()
  }

  @Synchronized
  fun kitMessage(ctx: Context, slug: String, text: String) {
    ensureChannel(ctx)
    val shortcutId = conversationId(slug)
    val you = Person.Builder().setName("You").setKey("you").build()
    val kit = Person.Builder().setName(slug).setKey(shortcutId).build()
    publishConversation(ctx, slug, kit)
    val next = pushKitTurn(history, historySlug, slug, text, System.currentTimeMillis())
    historySlug = next.first
    history = next.second
    val style = NotificationCompat.MessagingStyle(you)
      .setConversationTitle(slug)
      .setGroupConversation(false)
    for (turn in history) {
      style.addMessage(turn.text, turn.at, kit)
    }
    val reply = NotificationCompat.Action.Builder(
      R.drawable.ic_stat_cab,
      ctx.getString(R.string.reply_label),
      serviceIntent(ctx, ReplyService.ACTION_REPLY),
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
      serviceIntent(ctx, ReplyService.ACTION_READ),
    )
      .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
      .setShowsUserInterface(false)
      .build()
    val n = NotificationCompat.Builder(ctx, CHANNEL)
      .setSmallIcon(R.drawable.ic_stat_cab)
      .setContentTitle(slug)
      .setContentText(text)
      .setContentIntent(openApp(ctx))
      .setStyle(style)
      .addAction(reply)
      .addAction(read)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setShortcutId(shortcutId)
      .setAutoCancel(true)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .build()
    ctx.getSystemService(NotificationManager::class.java).notify(MESSAGE_ID, n)
  }

  fun buzzPush(ctx: Context) {
    val v = ctx.getSystemService(Vibrator::class.java) ?: return
    if (!v.hasVibrator()) {
      return
    }
    v.vibrate(VibrationEffect.createOneShot(PUSH_BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE))
  }

  @Synchronized
  fun dismissKit(ctx: Context) {
    history = emptyList()
    historySlug = ""
    ctx.getSystemService(NotificationManager::class.java).cancel(MESSAGE_ID)
  }

  private fun publishConversation(ctx: Context, slug: String, kit: Person) {
    val shortcut = ShortcutInfoCompat.Builder(ctx, conversationId(slug))
      .setShortLabel(slug)
      .setLongLabel(slug)
      .setIcon(IconCompat.createWithResource(ctx, R.drawable.ic_stat_cab))
      .setIntent(Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
      .setPerson(kit)
      .setLongLived(true)
      .setCategories(setOf("android.shortcut.conversation"))
      .build()
    ShortcutManagerCompat.pushDynamicShortcut(ctx, shortcut)
  }

  private fun openApp(ctx: Context): PendingIntent {
    return PendingIntent.getActivity(
      ctx,
      0,
      Intent(ctx, MainActivity::class.java)
        .setAction(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
  }

  private fun serviceIntent(ctx: Context, action: String): PendingIntent {
    val i = Intent(ctx, ReplyService::class.java).setAction(action)
    return PendingIntent.getService(
      ctx,
      action.hashCode(),
      i,
      PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
  }
}
