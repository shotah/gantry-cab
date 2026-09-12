package com.gantree.cab.drive

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Android Auto only reads a card that meets
 * developer.android.com/training/cars/communication/notification-messaging:
 * `MessagingStyle`, a reply `Action` (`SEMANTIC_ACTION_REPLY`, no UI, one
 * `RemoteInput`, mutable `PendingIntent` to a Service) and a mark-as-read
 * `Action` (`SEMANTIC_ACTION_MARK_AS_READ`, no UI). This posts a Kit reply
 * the way [MailboxService] does and checks the built [Notification].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CabNotifierAutoContractTest {
  private val app: Application = ApplicationProvider.getApplicationContext()
  private val nm: NotificationManager = app.getSystemService(NotificationManager::class.java)

  @Before
  fun forgetLastRun() {
    CabNotifier.dismissKit(app)
  }

  private fun post(text: String): Notification {
    CabNotifier.kitMessage(app, "kit", text)
    return shadowOf(nm).getNotification(CabNotifier.MESSAGE_ID) ?: error("no Kit card posted")
  }

  private fun style(n: Notification): NotificationCompat.MessagingStyle =
    NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) ?: error("no MessagingStyle")

  private fun action(n: Notification, semantic: Int): NotificationCompat.Action =
    (0 until NotificationCompat.getActionCount(n))
      .mapNotNull { NotificationCompat.getAction(n, it) }
      .firstOrNull { it.semanticAction == semantic }
      ?: error("no action with semantic $semantic")

  @Test
  fun kitCardIsAConversationAutoCanRead() {
    val n = post("leave by 8")
    assertEquals(Notification.CATEGORY_MESSAGE, n.category)
    assertNotNull(n.smallIcon)
    val style = style(n)
    assertEquals("You", style.user.name.toString())
    assertNull(style.conversationTitle)
    assertFalse(style.isGroupConversation)
    val m = style.messages.single()
    assertEquals("leave by 8", m.text.toString())
    assertEquals("Kit", m.person?.name.toString())
    assertNull(m.person?.icon)
    assertTrue(m.timestamp > 0L)
  }

  @Test
  fun replyActionMeetsAutoRules() {
    val reply = action(post("yo"), NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
    assertFalse(reply.showsUserInterface)
    val inputs = reply.remoteInputs ?: error("reply has no RemoteInput")
    assertEquals(1, inputs.size)
    assertEquals(CabNotifier.KEY_REPLY, inputs.single().resultKey)
    val pi = shadowOf(reply.actionIntent)
    assertTrue(pi.isService)
    assertTrue((pi.flags and PendingIntent.FLAG_MUTABLE) != 0)
    assertEquals(ReplyService::class.java.name, pi.savedIntent.component?.className)
    assertEquals(ReplyService.ACTION_REPLY, pi.savedIntent.action)
  }

  @Test
  fun markAsReadActionMeetsAutoRules() {
    val read = action(post("yo"), NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
    assertFalse(read.showsUserInterface)
    assertNull(read.remoteInputs)
    val pi = shadowOf(read.actionIntent)
    assertTrue(pi.isService)
    assertEquals(ReplyService.ACTION_READ, pi.savedIntent.action)
  }

  @Test
  fun swipingTheCardAwayForgetsTheHistory() {
    val del = shadowOf(post("one").deleteIntent)
    assertTrue(del.isService)
    assertEquals(ReplyService.ACTION_READ, del.savedIntent.action)
  }

  @Test
  fun repliesAppendUntilMarkedRead() {
    post("one")
    assertEquals(listOf("one", "two"), style(post("two")).messages.map { it.text.toString() })
    CabNotifier.dismissKit(app)
    assertNull(shadowOf(nm).getNotification(CabNotifier.MESSAGE_ID))
    assertEquals(listOf("three"), style(post("three")).messages.map { it.text.toString() })
  }

  @Test
  fun channelIsHighSoAutoHeadsUp() {
    post("yo")
    assertEquals(NotificationManager.IMPORTANCE_HIGH, nm.getNotificationChannel(CabNotifier.CHANNEL).importance)
  }

  @Test
  fun kitCardUsesTheAvatarBitmapInsteadOfALetter() {
    CabNotifier.kitMessage(app, "kit", "yo", solidJpeg())
    val n = shadowOf(nm).getNotification(CabNotifier.MESSAGE_ID) ?: error("no Kit card posted")
    val person = style(n).messages.single().person ?: error("no sender")
    assertNotNull(person.icon)
    assertEquals(IconCompat.TYPE_BITMAP, person.icon?.type)
    assertNotNull(n.getLargeIcon())
    assertNotNull(ShortcutManagerCompat.getDynamicShortcuts(app).firstOrNull { it.id == "cab-kit" })
  }

  @Test
  fun conversationShortcutMatchesTheCard() {
    val n = post("yo")
    assertEquals("cab-kit", n.shortcutId)
    val shortcut = ShortcutManagerCompat.getDynamicShortcuts(app).firstOrNull { it.id == "cab-kit" }
      ?: error("no conversation shortcut")
    assertTrue(shortcut.categories?.contains("android.shortcut.conversation") == true)
    assertEquals("Kit", shortcut.shortLabel.toString())
    assertEquals("Kit", shortcut.longLabel.toString())
  }

  @Test
  fun oneToOneCardDoesNotRepeatTheSlug() {
    CabNotifier.kitMessage(app, "tim", "How was that debug session?")
    val n = shadowOf(nm).getNotification(CabNotifier.MESSAGE_ID) ?: error("no Kit card posted")
    val style = style(n)
    assertNull(style.conversationTitle)
    assertFalse(style.isGroupConversation)
    assertEquals("Tim", style.messages.single().person?.name.toString())
    assertEquals("How was that debug session?", style.messages.single().text.toString())
    assertEquals("Tim", n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    assertNull(n.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE))
    val shortcut = ShortcutManagerCompat.getDynamicShortcuts(app).firstOrNull { it.id == "cab-tim" }
      ?: error("no conversation shortcut")
    assertEquals("Tim", shortcut.shortLabel.toString())
  }

  @Test
  fun spokenReplyFromAutoWakesTheMailboxSocket() {
    val reply = action(post("where are you"), NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
    val fired = Intent(shadowOf(reply.actionIntent).savedIntent)
    val said = Bundle().apply { putCharSequence(CabNotifier.KEY_REPLY, "  on my way ") }
    RemoteInput.addResultsToIntent(reply.remoteInputs ?: error("no RemoteInput"), fired, said)
    Robolectric.buildService(ReplyService::class.java, fired).create().startCommand(0, 1)
    val started = shadowOf(app).nextStartedService ?: error("reply did not start the mailbox socket")
    assertEquals(MailboxService::class.java.name, started.component?.className)
  }
}
