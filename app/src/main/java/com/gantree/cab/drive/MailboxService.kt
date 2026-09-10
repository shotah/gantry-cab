package com.gantree.cab.drive

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Looper
import androidx.lifecycle.LifecycleService
import com.gantree.cab.CabApp
import com.gantree.cab.outbound
import com.gantree.cab.spoken
import com.gantree.cab.mailbox.Geo
import com.gantree.cab.mailbox.MailboxClient
import com.gantree.cab.mailbox.PhoneContext
import com.gantree.cab.mailbox.parseSlug
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.time.Instant
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MailboxService : LifecycleService() {
  private var client: MailboxClient? = null

  override fun onCreate() {
    super.onCreate()
    instance = this
    CabNotifier.ensureChannel(this)
    if (Build.VERSION.SDK_INT >= 34) {
      startForeground(CabNotifier.CONNECTED_ID, CabNotifier.connected(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      startForeground(CabNotifier.CONNECTED_ID, CabNotifier.connected(this))
    }
    connect()
  }

  override fun onDestroy() {
    client?.stop()
    client = null
    if (instance === this) {
      instance = null
    }
    super.onDestroy()
  }

  private fun connect() {
    val app = application as CabApp
    val slug = parseSlug(app.prefs.slug) ?: return
    val bearer = app.prefs.bearer
    if (bearer.isBlank()) {
      return
    }
    val mailbox = MailboxClient(
      onFrame = { frame ->
        app.mouth.ingest(frame)
        if (frame.spoken()) {
          val text = frame.text?.trim().orEmpty().ifEmpty { "ping" }
          CabNotifier.kitMessage(this, slug, text)
        }
      },
      onState = { up -> app.mouth.setUp(up) },
    )
    client = mailbox
    mailbox.start(app.prefs.origin, slug, bearer)
  }

  fun send(text: String) {
    Thread {
      sendBlocking(text)
    }.start()
  }

  private fun sendBlocking(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
      return
    }
    val app = application as CabApp
    val frame = app.outbound(trimmed, phoneContext())
    client?.remember(frame.id ?: return)
    if (client?.send(frame) != true) {
      app.mouth.setHint("socket down — reconnecting")
    }
  }

  private fun phoneContext(): PhoneContext {
    val geo = lastGeo()
    return PhoneContext(
      at = Instant.now().toString(),
      tz = TimeZone.getDefault().id,
      geo = geo,
    )
  }

  private fun lastGeo(): Geo? {
    return try {
      val fused = LocationServices.getFusedLocationProviderClient(this)
      val loc = Tasks.await(
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
        4,
        TimeUnit.SECONDS,
      ) ?: return null
      Geo(lat = loc.latitude, lon = loc.longitude, accuracyM = loc.accuracy.toDouble())
    } catch (_: Exception) {
      null
    }
  }

  companion object {
    @Volatile
    private var instance: MailboxService? = null

    fun start(ctx: Context) {
      val i = Intent(ctx, MailboxService::class.java)
      ctx.startForegroundService(i)
    }

    fun sendText(ctx: Context, text: String) {
      val svc = instance
      if (svc != null) {
        svc.send(text)
        return
      }
      start(ctx)
      android.os.Handler(Looper.getMainLooper()).postDelayed({
        instance?.send(text)
      }, 400)
    }
  }
}
