package com.gantree.cab.drive

import android.app.Application
import androidx.car.app.messaging.model.CarMessage
import androidx.car.app.messaging.model.ConversationCallback
import androidx.car.app.messaging.model.ConversationItem
import androidx.car.app.model.CarText
import androidx.core.app.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Why the car needs a starter card: `ConversationItem` (Car API 7) refuses an
 * empty message list, and its **Reply** is the only voice-in a template app
 * gets. So an empty thread used to build nothing tappable on the head unit;
 * [carMessages] always hands the item at least Kit's first card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CarConversationContractTest {
  private val you = Person.Builder().setName("You").setKey("you").build()
  private val kit = kitPerson("kit")
  private val callback = object : ConversationCallback {
    override fun onMarkAsRead() {}
    override fun onTextReply(replyText: String) {}
  }

  private fun item(turns: List<CarTurn>): ConversationItem =
    ConversationItem.Builder(
      CabNotifier.conversationId("kit"),
      CarText.create("kit"),
      you,
      turns.map { turn ->
        CarMessage.Builder()
          .setSender(if (turn.fromYou) you else kit)
          .setBody(CarText.create(turn.text))
          .setReceivedTimeEpochMillis(turn.at)
          .setRead(turn.read)
          .build()
      },
      callback,
    ).build()

  @Test
  fun theLibraryRefusesAnEmptyThread() {
    val refused = assertThrows(IllegalStateException::class.java) { item(carTurns(emptyList())) }
    assertEquals("Message list cannot be empty.", refused.message)
  }

  @Test
  fun theStarterIsACardTheHostCanShowWithReply() {
    val built = item(carMessages(emptyList(), "kit", 1_000L))
    assertEquals(1, built.messages.size)
    val card = built.messages.single()
    assertEquals("Nothing said yet. Tap Reply and talk to Kit.", card.body.toString())
    assertEquals(kit.name, card.sender?.name)
    assertEquals(true, card.isRead)
    assertEquals(1_000L, card.receivedTimeEpochMillis)
  }
}
