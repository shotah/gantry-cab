package com.gantree.cab

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gantree.cab.drive.CabNotifier
import com.gantree.cab.drive.MailboxService
import com.gantree.cab.drive.carCheckText
import com.gantree.cab.drive.carTestBlocked
import com.gantree.cab.mailbox.cameraShotUri
import com.gantree.cab.mailbox.parseSlug
import com.gantree.cab.ui.CabScreen
import com.gantree.cab.ui.CabTheme
import com.gantree.cab.ui.Permits

class MainActivity : ComponentActivity() {
  private val vm: CabViewModel by viewModels { CabViewModel.factory(application as CabApp) }

  /** What Settings → Access paints. Re-read after every prompt and on resume (the OS Settings page changes it too). */
  private val permits = mutableStateOf(Permits())
  /** Access → Enable location: flip send-on-turns on with the grant, like pendant `GeoEnable`. */
  private var latchGps = false

  private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { readPermits() }
  private val askLoc = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
    readPermits()
    if (latchGps && grants[Manifest.permission.ACCESS_FINE_LOCATION] == true && !vm.gps.value) {
      vm.toggleGps()
    }
    latchGps = false
  }
  private val askMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { readPermits() }
  private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) vm.stagePhoto(this, uri)
  }
  private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
    if (taken) vm.stagePhoto(this, cameraShotUri(this))
  }
  private val pickAvatar = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) vm.uploadAvatar(this, uri)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    readPermits()
    requestNotify()
    if (BuildConfig.DEV) {
      intent.getStringExtra(EXTRA_SAMPLE)?.let { vm.showSample(it) }
    }
    if ((application as CabApp).prefs.signedIn) {
      MailboxService.start(this)
    }
    setContent {
      val origin by vm.origin.collectAsStateWithLifecycle()
      val slug by vm.slug.collectAsStateWithLifecycle()
      val spike by vm.spike.collectAsStateWithLifecycle()
      val email by vm.email.collectAsStateWithLifecycle()
      val painted by vm.painted.collectAsStateWithLifecycle()
      val font by vm.font.collectAsStateWithLifecycle()
      val photoSize by vm.photoSize.collectAsStateWithLifecycle()
      val backdropOn by vm.backdropOn.collectAsStateWithLifecycle()
      val followTheme by vm.followTheme.collectAsStateWithLifecycle()
      val gps by vm.gps.collectAsStateWithLifecycle()
      val cranes by vm.cranes.collectAsStateWithLifecycle()
      val face by vm.face.collectAsStateWithLifecycle()
      val backdrop by vm.backdrop.collectAsStateWithLifecycle()
      val up by vm.up.collectAsStateWithLifecycle()
      val hint by vm.hint.collectAsStateWithLifecycle()
      val lines by vm.lines.collectAsStateWithLifecycle()
      val catalog by vm.catalog.collectAsStateWithLifecycle()
      val faceHint by vm.faceHint.collectAsStateWithLifecycle()
      val signingIn by vm.signingIn.collectAsStateWithLifecycle()
      val authHint by vm.authHint.collectAsStateWithLifecycle()
      val typingUntil by vm.typingUntil.collectAsStateWithLifecycle()
      val sub by vm.sub.collectAsStateWithLifecycle()
      val stagedPhoto by vm.stagedPhoto.collectAsStateWithLifecycle()
      val voiceOn by vm.voice.collectAsStateWithLifecycle()
      val voiceOffered by vm.voiceOffered.collectAsStateWithLifecycle()
      val speakPhase by vm.speakPhase.collectAsStateWithLifecycle()
      val lang by vm.lang.collectAsStateWithLifecycle()
      val granted by permits
      CabTheme(themeId = painted, fontId = font) {
        CabScreen(
          origin = origin,
          slug = slug,
          spike = spike,
          email = email,
          googleReady = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank(),
          up = up,
          hint = hint,
          lines = lines,
          onOrigin = vm::setOrigin,
          onSlug = vm::setSlug,
          onSpike = vm::setSpike,
          onConnect = {
            vm.persist()
            MailboxService.start(this)
          },
          onGoogle = { vm.signIn(this) },
          onSignOut = vm::signOut,
          authHint = authHint,
          signingIn = signingIn,
          onSend = { text -> vm.sendDraft(this, text) },
          dev = BuildConfig.DEV,
          onSample = vm::showSample,
          themeId = painted,
          fontId = font,
          gpsOn = gps,
          catalog = catalog,
          avatarBytes = face,
          faceHint = faceHint,
          cranes = cranes,
          onTheme = vm::setTheme,
          onFont = vm::setFont,
          onGpsToggle = {
            if (!gps) {
              requestLoc()
            }
            vm.toggleGps()
          },
          onPhoto = {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          onCamera = {
            try {
              takePhoto.launch(cameraShotUri(this))
            } catch (_: ActivityNotFoundException) {
              (application as CabApp).mouth.setHint("No camera app on this phone.")
            }
          },
          onPhotoClear = vm::clearStagedPhoto,
          photo = stagedPhoto,
          onAvatar = {
            pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          onPin = {
            vm.persist()
            MailboxService.sendPin(this)
          },
          typingUntil = typingUntil,
          sub = sub,
          onCarTest = { carTest(slug) },
          photoSizeId = photoSize,
          onPhotoSize = vm::setPhotoSize,
          backdropBytes = backdrop,
          backdropOn = backdropOn,
          followTheme = followTheme,
          onBackdropToggle = vm::toggleBackdrop,
          onFollowToggle = vm::toggleFollowTheme,
          voiceOffered = voiceOffered,
          voiceOn = voiceOn,
          speakPhase = speakPhase,
          permits = granted,
          onVoiceToggle = vm::toggleVoice,
          onVoice = { text -> vm.sendVoice(this, text) },
          onHoldStart = vm::hushVoice,
          onMicAsk = ::requestMic,
          onLocAsk = {
            latchGps = true
            requestLoc()
          },
          onNotifyAsk = ::requestNotify,
          langId = lang,
          onLang = vm::setLang,
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    readPermits()
    (application as CabApp).phoneResumed = true
    // The thread is on screen: the shown turns are read. Same as Auto's mark-as-read
    // and a swipe — otherwise the card stacks until the next Kit reply re-posts the backlog.
    CabNotifier.dismissKit(this)
    // What the browser sent while this was in the background comes over on a connect flush.
    MailboxService.sweep()
  }

  override fun onPause() {
    (application as CabApp).phoneResumed = false
    super.onPause()
  }

  private fun has(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

  private fun notifyGranted(): Boolean =
    Build.VERSION.SDK_INT < 33 || has(Manifest.permission.POST_NOTIFICATIONS)

  private fun readPermits() {
    permits.value = Permits(
      mic = has(Manifest.permission.RECORD_AUDIO),
      location = has(Manifest.permission.ACCESS_FINE_LOCATION),
      notify = notifyGranted(),
    )
  }

  private fun requestNotify() {
    if (!notifyGranted()) {
      askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  /** Hold bar without the grant, or Access → Enable microphone. Never from the car pane. */
  private fun requestMic() {
    if (!has(Manifest.permission.RECORD_AUDIO)) {
      askMic.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  /**
   * Settings → Test car voice. Same [CabNotifier.kitMessage] path as a real
   * Kit reply, so Android Auto reads it if the sideload is allowed. When
   * Android itself would drop the card, ask / open the switch instead.
   */
  private fun carTest(slug: String) {
    if (!notifyGranted()) {
      requestNotify()
      return
    }
    CabNotifier.ensureChannel(this)
    val nm = getSystemService(NotificationManager::class.java)
    val enabled = nm.areNotificationsEnabled()
    val importance = nm.getNotificationChannel(CabNotifier.CHANNEL)?.importance
    if (carTestBlocked(enabled, importance)) {
      val fix = if (enabled) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
          .putExtra(Settings.EXTRA_CHANNEL_ID, CabNotifier.CHANNEL)
      } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      }
      startActivity(fix.putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
      return
    }
    val app = application as CabApp
    CabNotifier.kitMessage(this, parseSlug(slug) ?: "cab", carCheckText(app.carAttached), app.face)
  }

  private fun requestLoc() {
    val fine = Manifest.permission.ACCESS_FINE_LOCATION
    val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    if (!has(fine)) {
      askLoc.launch(arrayOf(fine, coarse))
    }
  }

  companion object {
    const val EXTRA_SAMPLE = "sample"
  }
}
