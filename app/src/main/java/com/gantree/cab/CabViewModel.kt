package com.gantree.cab

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gantree.cab.dev.sampleScene
import com.gantree.cab.drive.MailboxService
import com.gantree.cab.mailbox.AuthException
import com.gantree.cab.mailbox.AvatarUpload
import com.gantree.cab.mailbox.AVATAR_EDGE
import com.gantree.cab.mailbox.AVATAR_MAX_BYTES
import com.gantree.cab.mailbox.CHAT_PHOTO_EDGE
import com.gantree.cab.mailbox.IMAGE_BYTES_MAX
import com.gantree.cab.mailbox.PhotoResult
import com.gantree.cab.mailbox.jpegFromUri
import com.gantree.cab.mailbox.normalizeMailboxOrigin
import com.gantree.cab.mailbox.parseSlug
import com.gantree.cab.mailbox.photoDataUrl
import com.gantree.cab.ui.requestGoogleId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CabViewModel(private val app: CabApp) : ViewModel() {
  private val _origin = MutableStateFlow(app.prefs.origin)
  private val _slug = MutableStateFlow(app.prefs.slug)
  private val _spike = MutableStateFlow(app.prefs.spike)
  private val _email = MutableStateFlow(app.prefs.email)
  private val _theme = MutableStateFlow(app.prefs.theme)
  private val _font = MutableStateFlow(app.prefs.font)
  private val _gps = MutableStateFlow(app.prefs.gps)
  private val _cranes = MutableStateFlow<List<String>>(emptyList())
  private val _face = MutableStateFlow<ByteArray?>(null)
  val origin = _origin.asStateFlow()
  val slug = _slug.asStateFlow()
  val spike = _spike.asStateFlow()
  val email = _email.asStateFlow()
  val theme = _theme.asStateFlow()
  val font = _font.asStateFlow()
  val gps = _gps.asStateFlow()
  val cranes = _cranes.asStateFlow()
  val face = _face.asStateFlow()
  val up = app.mouth.up.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
  val hint = app.mouth.hint.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
  val lines = app.mouth.lines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
  val catalog = app.mouth.catalog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
  val avatarRev = app.mouth.avatarRev.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
  val faceHint = app.mouth.faceHint.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

  init {
    viewModelScope.launch {
      combine(_origin, _slug, app.mouth.avatarRev) { origin, slug, rev -> Triple(origin, slug, rev) }
        .collectLatest { (origin, slug, rev) ->
          val room = parseSlug(slug) ?: slug
          _face.value = withContext(Dispatchers.IO) {
            app.avatar.fetch(origin, room, app.prefs.bearer, rev)
          }
        }
    }
  }

  fun setOrigin(v: String) { _origin.value = v }
  fun setSpike(v: String) { _spike.value = v }

  fun setSlug(v: String) {
    _slug.value = v
    app.mouth.setAvatarRev(0)
    app.mouth.setFaceHint("")
  }

  fun setTheme(v: String) {
    _theme.value = v
    app.prefs.theme = v
  }

  fun setFont(v: String) {
    _font.value = v
    app.prefs.font = v
  }

  fun toggleGps() {
    val next = !_gps.value
    _gps.value = next
    app.prefs.gps = next
    app.mouth.setHint(if (next) "GPS attaches on send if the OS allows it." else "GPS off")
  }

  fun showSample(id: String) {
    if (!BuildConfig.DEV) {
      return
    }
    val scene = sampleScene(id) ?: return
    _slug.value = scene.slug
    _email.value = scene.email
    app.mouth.replace(scene.lines, scene.up, scene.hint)
    app.mouth.setAvatarRev(0)
    app.mouth.setFaceHint("")
  }

  fun persist() {
    val origin = normalizeMailboxOrigin(_origin.value)
    _origin.value = origin
    app.prefs.origin = origin
    app.prefs.slug = parseSlug(_slug.value) ?: _slug.value
    app.prefs.spike = _spike.value
  }

  fun signOut() {
    app.prefs.signOut()
    _email.value = ""
    _cranes.value = emptyList()
    app.mouth.setHint("signed out")
  }

  fun sendPhoto(ctx: Context, uri: Uri) {
    viewModelScope.launch {
      try {
        val jpeg = withContext(Dispatchers.IO) {
          jpegFromUri(ctx.contentResolver, uri, CHAT_PHOTO_EDGE, IMAGE_BYTES_MAX)
        }
        when (val got = photoDataUrl(jpeg)) {
          is PhotoResult.Ok -> {
            persist()
            MailboxService.sendPhoto(ctx, got.url)
          }
          is PhotoResult.Err -> app.mouth.setHint(got.error)
        }
      } catch (e: Exception) {
        app.mouth.setHint(e.message ?: "bad photo")
      }
    }
  }

  fun uploadAvatar(ctx: Context, uri: Uri) {
    viewModelScope.launch {
      try {
        val jpeg = withContext(Dispatchers.IO) {
          jpegFromUri(ctx.contentResolver, uri, AVATAR_EDGE, AVATAR_MAX_BYTES)
        }
        val room = parseSlug(_slug.value) ?: _slug.value
        when (val got = withContext(Dispatchers.IO) {
          app.avatar.upload(_origin.value, room, app.prefs.bearer, jpeg)
        }) {
          is AvatarUpload.Ok -> {
            app.mouth.setFaceHint("")
            app.mouth.setAvatarRev(got.rev)
          }
          is AvatarUpload.Err -> app.mouth.setFaceHint(got.error)
        }
      } catch (e: Exception) {
        app.mouth.setFaceHint(e.message ?: "could not read that image")
      }
    }
  }

  fun signIn(activity: Activity) {
    val web = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (web.isBlank()) {
      app.mouth.setHint("rebuild with CAB_GOOGLE_WEB_CLIENT_ID")
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
        _cranes.value = me.cranes
        if (me.cranes.isNotEmpty() && parseSlug(_slug.value) !in me.cranes) {
          _slug.value = me.cranes.first()
          app.prefs.slug = me.cranes.first()
        }
        app.mouth.setHint("signed in as ${session.email ?: session.sub}")
        MailboxService.start(activity)
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
