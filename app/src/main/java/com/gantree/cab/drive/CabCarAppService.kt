package com.gantree.cab.drive

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.messaging.model.CarMessage
import androidx.car.app.messaging.model.ConversationCallback
import androidx.car.app.messaging.model.ConversationItem
import androidx.car.app.model.Action
import androidx.car.app.model.CarText
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.core.app.Person
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gantree.cab.BuildConfig
import com.gantree.cab.CabApp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CabCarAppService : CarAppService() {
  override fun createHostValidator(): HostValidator {
    if (BuildConfig.DEV) {
      return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
    return HostValidator.Builder(applicationContext)
      .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
      .build()
  }

  override fun onCreateSession(sessionInfo: SessionInfo): Session = CabSession()
}

class CabSession : Session() {
  override fun onCreateScreen(intent: Intent): Screen {
    val app = carContext.applicationContext as CabApp
    if (app.prefs.signedIn) {
      MailboxService.start(carContext)
    }
    return CabThreadScreen(carContext)
  }
}

class CabThreadScreen(carContext: CarContext) : Screen(carContext) {
  private val you = Person.Builder().setName("You").setKey("you").build()
  private val conversationCallback = object : ConversationCallback {
    override fun onMarkAsRead() {
      CabNotifier.dismissKit(carContext)
    }

    override fun onTextReply(replyText: String) {
      val text = replyText.trim()
      if (text.isNotEmpty()) {
        MailboxService.sendText(carContext, text)
      }
    }
  }

  init {
    val app = carContext.applicationContext as CabApp
    lifecycle.addObserver(
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_START -> app.carThreadVisible = true
          Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> app.carThreadVisible = false
          else -> {}
        }
      },
    )
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        app.mouth.lines
          .map { carTurns(it) }
          .distinctUntilChanged()
          .collect { invalidate() }
      }
    }
  }

  override fun onGetTemplate(): Template {
    val app = carContext.applicationContext as CabApp
    val slug = app.prefs.slug.ifBlank { "cab" }
    val kit = Person.Builder().setName(slug).setKey(CabNotifier.conversationId(slug)).build()
    val messages = carTurns(app.mouth.lines.value).map { turn ->
      val body = CarText.create(turn.text)
      CarMessage.Builder()
        .setSender(if (turn.fromYou) you else kit)
        .setBody(body)
        .setReceivedTimeEpochMillis(turn.at)
        .setRead(turn.fromYou)
        .build()
    }
    val conversation = ConversationItem.Builder(
      CabNotifier.conversationId(slug),
      CarText.create(slug),
      you,
      messages,
      conversationCallback,
    ).build()
    return ListTemplate.Builder()
      .setHeader(
        Header.Builder()
          .setStartHeaderAction(Action.APP_ICON)
          .setTitle(slug)
          .build(),
      )
      .setSingleList(ItemList.Builder().addItem(conversation).build())
      .build()
  }
}
