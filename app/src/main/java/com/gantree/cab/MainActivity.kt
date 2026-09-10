package com.gantree.cab

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.gantree.cab.drive.MailboxService
import com.gantree.cab.ui.CabScreen
import com.gantree.cab.ui.CabTheme

class MainActivity : ComponentActivity() {
  private val vm: CabViewModel by viewModels { CabViewModel.factory(application as CabApp) }

  private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
  private val askLoc = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestBits()
    setContent {
      val origin by vm.origin.collectAsStateWithLifecycle()
      val slug by vm.slug.collectAsStateWithLifecycle()
      val spike by vm.spike.collectAsStateWithLifecycle()
      val email by vm.email.collectAsStateWithLifecycle()
      val up by vm.up.collectAsStateWithLifecycle()
      val hint by vm.hint.collectAsStateWithLifecycle()
      val lines by vm.lines.collectAsStateWithLifecycle()
      CabTheme {
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
          onSend = { text ->
            vm.persist()
            MailboxService.sendText(this, text)
          },
        )
      }
    }
  }

  private fun requestBits() {
    if (Build.VERSION.SDK_INT >= 33
      && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
      != PackageManager.PERMISSION_GRANTED
    ) {
      askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val fine = Manifest.permission.ACCESS_FINE_LOCATION
    val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    if (ContextCompat.checkSelfPermission(this, fine) != PackageManager.PERMISSION_GRANTED) {
      askLoc.launch(arrayOf(fine, coarse))
    }
  }
}
