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
import com.gantree.cab.mailbox.PHOTO_JPEG_BYTES_MAX
import com.gantree.cab.mailbox.PhotoResult
import com.gantree.cab.mailbox.describePhotoError
import com.gantree.cab.mailbox.jpegFromUri
import com.gantree.cab.mailbox.googleSignInHint
import com.gantree.cab.mailbox.mailboxSignedInHint
import com.gantree.cab.mailbox.normalizeMailboxOrigin
import com.gantree.cab.mailbox.parseSlug
import com.gantree.cab.mailbox.paintedTheme
import com.gantree.cab.mailbox.persistSpikeAllowed
import com.gantree.cab.mailbox.photoDataUrl
import com.gantree.cab.mailbox.photoEdge
import com.gantree.cab.mailbox.photoErrorToken
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.ui.requestGoogleId
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import kotlin.coroutines.cancellation.CancellationException
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
  private val _photoSize = MutableStateFlow(app.prefs.photoSize)
  private val _backdropOn = MutableStateFlow(app.prefs.backdrop)
  private val _followTheme = MutableStateFlow(app.prefs.followTheme)
  private val _gps = MutableStateFlow(app.prefs.gps)
  private val _cranes = MutableStateFlow<List<String>>(emptyList())
  private val _face = MutableStateFlow<ByteArray?>(null)
  private val _backdrop = MutableStateFlow<ByteArray?>(null)
  private val _signingIn = MutableStateFlow(false)
  private val _authHint = MutableStateFlow("")
  private val _sub = MutableStateFlow("")
  private val _fetchOrigin = MutableStateFlow(app.prefs.origin)
  val origin = _origin.asStateFlow()
  val slug = _slug.asStateFlow()
  val spike = _spike.asStateFlow()
  val email = _email.asStateFlow()
  val theme = _theme.asStateFlow()
  val font = _font.asStateFlow()
  val photoSize = _photoSize.asStateFlow()
  val backdropOn = _backdropOn.asStateFlow()
  val followTheme = _followTheme.asStateFlow()
  val gps = _gps.asStateFlow()
  val cranes = _cranes.asStateFlow()
  val face = _face.asStateFlow()
  val backdrop = _backdrop.asStateFlow()
  val signingIn = _signingIn.asStateFlow()
  val authHint = _authHint.asStateFlow()
  val sub = _sub.asStateFlow()
  val up = app.mouth.up.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
  val hint = app.mouth.hint.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
  val lines = app.mouth.lines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
  val catalog = app.mouth.catalog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
  val avatarRev = app.mouth.avatarRev.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
  val painted = combine(_followTheme, app.mouth.roomTheme, _theme) { follow, room, mine ->
    paintedTheme(follow, room, mine)
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5_000),
    paintedTheme(app.prefs.followTheme, app.mouth.roomTheme.value, app.prefs.theme),
  )
  val faceHint = app.mouth.faceHint.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
  val typingUntil = app.mouth.typingUntil.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

  init {
    viewModelScope.launch {
      combine(_fetchOrigin, _slug, app.mouth.avatarRev) { origin, slug, rev -> Triple(origin, slug, rev) }
        .collectLatest { (origin, slug, rev) ->
          val room = parseSlug(slug) ?: return@collectLatest
          if (origin.isBlank() || app.prefs.bearer.isBlank()) {
            return@collectLatest
          }
          if (_face.value == null) {
            paintFace(withContext(Dispatchers.IO) { app.avatar.cached(origin, room) })
          }
          paintFace(app.avatar.fetch(origin, room, app.prefs.bearer, rev))
        }
    }
    viewModelScope.launch {
      combine(_fetchOrigin, _slug, app.mouth.backdropRev, _backdropOn) { origin, slug, rev, on ->
        Triple(origin, slug, rev to on)
      }.collectLatest { (origin, slug, rest) ->
        val (rev, on) = rest
        if (!on) {
          _backdrop.value = null
          return@collectLatest
        }
        val room = parseSlug(slug) ?: return@collectLatest
        if (origin.isBlank() || app.prefs.bearer.isBlank()) {
          return@collectLatest
        }
        if (_backdrop.value == null) {
          paintBackdrop(withContext(Dispatchers.IO) { app.avatar.cached(origin, room, "/api/backdrop") })
        }
        paintBackdrop(app.avatar.fetch(origin, room, app.prefs.bearer, rev, "/api/backdrop"))
      }
    }
    viewModelScope.launch {
      combine(_fetchOrigin, _slug) { origin, slug -> origin to slug }
        .collectLatest { (origin, slug) ->
          val room = parseSlug(slug) ?: return@collectLatest
          if (origin.isBlank() || app.prefs.bearer.isBlank()) {
            return@collectLatest
          }
          if (app.mouth.roomTheme.value.isEmpty()) {
            app.mouth.setRoomTheme(app.prefs.roomTheme(room))
          }
          val got = withContext(Dispatchers.IO) { app.theme.fetch(origin, room, app.prefs.bearer) }
          if (got != null) {
            app.mouth.setRoomTheme(got)
            app.prefs.putRoomTheme(room, got)
          }
        }
    }
  }

  /** Same bytes stay put — no re-decode when the mailbox confirms what disk already painted. */
  private fun paintFace(bytes: ByteArray?) {
    if (bytes contentEquals _face.value) {
      return
    }
    _face.value = bytes
    app.face = bytes
  }

  private fun paintBackdrop(bytes: ByteArray?) {
    if (!(bytes contentEquals _backdrop.value)) {
      _backdrop.value = bytes
    }
  }

  fun setOrigin(v: String) { _origin.value = v }
  fun setSpike(v: String) { _spike.value = v }

  fun setSlug(v: String) {
    _slug.value = v
    _face.value = null
    app.face = null
    app.mouth.setAvatarRev(0)
    app.mouth.setBackdropRev(0)
    app.mouth.setRoomTheme("")
    app.mouth.setFaceHint("")
  }

  fun setTheme(v: String) {
    _theme.value = v
    app.prefs.theme = v
    if (_followTheme.value) {
      _followTheme.value = false
      app.prefs.followTheme = false
    }
  }

  fun setFont(v: String) {
    _font.value = v
    app.prefs.font = v
  }

  fun setPhotoSize(v: String) {
    _photoSize.value = v
    app.prefs.photoSize = v
  }

  fun toggleBackdrop() {
    val next = !_backdropOn.value
    _backdropOn.value = next
    app.prefs.backdrop = next
  }

  fun toggleFollowTheme() {
    val next = !_followTheme.value
    _followTheme.value = next
    app.prefs.followTheme = next
    if (!next) {
      val shown = paintedTheme(true, app.mouth.roomTheme.value, _theme.value)
      _theme.value = shown
      app.prefs.theme = shown
    }
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
    app.sampleShown = true
    _slug.value = scene.slug
    _email.value = scene.email
    app.mouth.replace(scene.lines, scene.up, scene.hint)
    if (scene.typing) {
      app.mouth.ingest(WireFrame(kind = "typing"))
    }
    app.mouth.setAvatarRev(0)
    app.mouth.setBackdropRev(0)
    app.mouth.setRoomTheme("")
    app.mouth.setFaceHint("")
  }

  fun persist() {
    val origin = normalizeMailboxOrigin(_origin.value)
    _origin.value = origin
    app.prefs.origin = origin
    parseSlug(_slug.value)?.let { app.prefs.slug = it }
    if (app.prefs.session.isNotBlank()) {
      _spike.value = ""
      app.prefs.spike = ""
    } else {
      app.prefs.putSpike(
        _spike.value,
        persistSpikeAllowed(origin, false, BuildConfig.DEV),
      )
    }
    _fetchOrigin.value = origin
  }

  fun signOut() {
    app.prefs.signOut()
    _email.value = ""
    _cranes.value = emptyList()
    _sub.value = ""
    _face.value = null
    app.face = null
    app.mouth.setHint("signed out")
    MailboxService.stop(app)
  }

  fun sendPhoto(ctx: Context, uri: Uri) {
    val edge = photoEdge(_photoSize.value)
    viewModelScope.launch {
      try {
        val jpeg = withContext(Dispatchers.IO) {
          jpegFromUri(ctx.contentResolver, uri, edge, PHOTO_JPEG_BYTES_MAX)
        }
        when (val got = photoDataUrl(jpeg)) {
          is PhotoResult.Ok -> {
            persist()
            MailboxService.sendPhoto(ctx, got.url)
          }
          is PhotoResult.Err -> app.mouth.setHint(describePhotoError(got.error))
        }
      } catch (e: Exception) {
        app.mouth.setHint(describePhotoError(photoErrorToken(e.message)))
      }
    }
  }

  fun uploadAvatar(ctx: Context, uri: Uri) {
    viewModelScope.launch {
      try {
        val jpeg = withContext(Dispatchers.IO) {
          jpegFromUri(ctx.contentResolver, uri, AVATAR_EDGE, AVATAR_MAX_BYTES)
        }
        val room = parseSlug(_slug.value) ?: run {
          app.mouth.setFaceHint("Talking to needs a crane slug like kit.")
          return@launch
        }
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
      _authHint.value = "rebuild with CAB_GOOGLE_WEB_CLIENT_ID"
      return
    }
    if (_signingIn.value) {
      return
    }
    persist()
    viewModelScope.launch {
      _signingIn.value = true
      _authHint.value = "Opening Google…"
      try {
        val google = requestGoogleId(activity, web)
        val session = withContext(Dispatchers.IO) {
          app.auth.token(_origin.value, google.idToken, google.nonce)
        }
        app.prefs.session = session.token
        app.prefs.sessionExp = session.exp
        app.prefs.email = session.email.orEmpty()
        _spike.value = ""
        _email.value = session.email.orEmpty()
        val me = withContext(Dispatchers.IO) { app.auth.me(_origin.value, session.token) }
        _cranes.value = me.cranes
        _sub.value = me.sub.ifBlank { session.sub }
        if (me.cranes.isNotEmpty() && parseSlug(_slug.value) !in me.cranes) {
          _slug.value = me.cranes.first()
          app.prefs.slug = me.cranes.first()
        }
        _authHint.value = mailboxSignedInHint(session.email.orEmpty(), me.cranes)
        app.mouth.setHint(mailboxSignedInHint(session.email.orEmpty(), me.cranes))
        MailboxService.start(activity)
      } catch (e: CancellationException) {
        throw e
      } catch (e: NoCredentialException) {
        _authHint.value = googleSignInHint(e)
      } catch (e: GetCredentialException) {
        _authHint.value = googleSignInHint(e)
      } catch (e: AuthException) {
        _authHint.value = "Mailbox auth ${e.code}\n${e.body.take(400)}"
      } catch (e: Exception) {
        _authHint.value = googleSignInHint(e)
      } finally {
        _signingIn.value = false
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
