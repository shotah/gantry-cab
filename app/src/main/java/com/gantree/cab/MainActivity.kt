package com.gantree.cab

import android.Manifest
import android.app.NotificationManager
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gantree.cab.drive.CabNotifier
import com.gantree.cab.drive.MailboxService
import com.gantree.cab.drive.carCheckText
import com.gantree.cab.drive.carTestBlocked
import com.gantree.cab.mailbox.parseSlug
import com.gantree.cab.ui.CabScreen
import com.gantree.cab.ui.CabTheme

class MainActivity : ComponentActivity() {
  private val vm: CabViewModel by viewModels { CabViewModel.factory(application as CabApp) }

  private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
  private val askLoc = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
  private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) vm.sendPhoto(this, uri)
  }
  private val pickAvatar = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) vm.uploadAvatar(this, uri)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
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
      val theme by vm.theme.collectAsStateWithLifecycle()
      val font by vm.font.collectAsStateWithLifecycle()
      val photoSize by vm.photoSize.collectAsStateWithLifecycle()
      val gps by vm.gps.collectAsStateWithLifecycle()
      val cranes by vm.cranes.collectAsStateWithLifecycle()
      val face by vm.face.collectAsStateWithLifecycle()
      val up by vm.up.collectAsStateWithLifecycle()
      val hint by vm.hint.collectAsStateWithLifecycle()
      val lines by vm.lines.collectAsStateWithLifecycle()
      val catalog by vm.catalog.collectAsStateWithLifecycle()
      val faceHint by vm.faceHint.collectAsStateWithLifecycle()
      val signingIn by vm.signingIn.collectAsStateWithLifecycle()
      val authHint by vm.authHint.collectAsStateWithLifecycle()
      val typingUntil by vm.typingUntil.collectAsStateWithLifecycle()
      val sub by vm.sub.collectAsStateWithLifecycle()
      CabTheme(themeId = theme, fontId = font) {
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
          onSend = { text ->
            vm.persist()
            MailboxService.sendText(this, text)
          },
          dev = BuildConfig.DEV,
          onSample = vm::showSample,
          themeId = theme,
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
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    (application as CabApp).phoneResumed = true
  }

  override fun onPause() {
    (application as CabApp).phoneResumed = false
    super.onPause()
  }

  private fun notifyGranted(): Boolean =
    Build.VERSION.SDK_INT < 33 ||
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED

  private fun requestNotify() {
    if (!notifyGranted()) {
      askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    CabNotifier.kitMessage(this, parseSlug(slug) ?: "cab", carCheckText(app.carAttached))
  }

  private fun requestLoc() {
    val fine = Manifest.permission.ACCESS_FINE_LOCATION
    val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    if (ContextCompat.checkSelfPermission(this, fine) != PackageManager.PERMISSION_GRANTED) {
      askLoc.launch(arrayOf(fine, coarse))
    }
  }

  companion object {
    const val EXTRA_SAMPLE = "sample"
  }
}
