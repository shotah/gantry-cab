package com.gantree.cab

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gantree.cab.mailbox.AuthException
import com.gantree.cab.mailbox.parseSlug
import com.gantree.cab.ui.requestGoogleId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CabViewModel(private val app: CabApp) : ViewModel() {
  private val _origin = MutableStateFlow(app.prefs.origin)
  private val _slug = MutableStateFlow(app.prefs.slug)
  private val _spike = MutableStateFlow(app.prefs.spike)
  private val _email = MutableStateFlow(app.prefs.email)
  val origin = _origin.asStateFlow()
  val slug = _slug.asStateFlow()
  val spike = _spike.asStateFlow()
  val email = _email.asStateFlow()
  val up = app.mouth.up.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
  val hint = app.mouth.hint.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
  val lines = app.mouth.lines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun setOrigin(v: String) { _origin.value = v }
  fun setSlug(v: String) { _slug.value = v }
  fun setSpike(v: String) { _spike.value = v }

  fun persist() {
    app.prefs.origin = _origin.value
    app.prefs.slug = parseSlug(_slug.value) ?: _slug.value
    app.prefs.spike = _spike.value
  }

  fun signOut() {
    app.prefs.signOut()
    _email.value = ""
    app.mouth.setHint("signed out")
  }

  fun signIn(activity: Activity) {
    val web = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (web.isBlank()) {
      app.mouth.setHint("set cab.googleWebClientId in local.properties")
      return
    }
    persist()
    viewModelScope.launch {
      try {
        val google = requestGoogleId(activity, web)
        val session = withContext(Dispatchers.IO) {
          app.auth.token(_origin.value, google.idToken, google.nonce)
        }
        app.prefs.session = session.token
        app.prefs.email = session.email.orEmpty()
        _email.value = session.email.orEmpty()
        val me = withContext(Dispatchers.IO) { app.auth.me(_origin.value, session.token) }
        if (me.cranes.isNotEmpty() && parseSlug(_slug.value) !in me.cranes) {
          _slug.value = me.cranes.first()
          app.prefs.slug = me.cranes.first()
        }
        app.mouth.setHint("signed in as ${session.email ?: session.sub}")
      } catch (e: AuthException) {
        app.mouth.setHint("auth ${e.code}")
      } catch (e: Exception) {
        app.mouth.setHint(e.message ?: "google failed")
      }
    }
  }

  companion object {
    fun factory(app: CabApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CabViewModel(app) as T
      }
    }
  }
}
