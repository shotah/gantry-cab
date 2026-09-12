package com.gantree.cab.drive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gantree.cab.CabApp
import com.gantree.cab.mailbox.BatteryHint
import com.gantree.cab.mailbox.GEO_CACHE_MS
import com.gantree.cab.mailbox.GEO_LAST_KNOWN_MS
import com.gantree.cab.mailbox.SWEEP_EVERY_MS
import com.gantree.cab.mailbox.Geo
import com.gantree.cab.mailbox.MailboxClient
import com.gantree.cab.mailbox.PhoneContext
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.applyEmoji
import com.gantree.cab.mailbox.batteryHint
import com.gantree.cab.mailbox.cursorOf
import com.gantree.cab.mailbox.geoFromFix
import com.gantree.cab.mailbox.geoHint
import com.gantree.cab.mailbox.mailboxAuthLostHint
import com.gantree.cab.mailbox.mailboxConnectError
import com.gantree.cab.mailbox.mailboxSocketHint
import com.gantree.cab.mailbox.mailboxTimeoutHint
import com.gantree.cab.mailbox.netHint
import com.gantree.cab.mailbox.notifyBody
import com.gantree.cab.mailbox.parseSlug
import com.gantree.cab.mailbox.sessionExpired
import com.gantree.cab.mailbox.pinFrame
import com.gantree.cab.mailbox.sendGeoHint
import com.gantree.cab.mailbox.surfaceHint
import com.gantree.cab.mailbox.watchingThread
import com.gantree.cab.outbound
import com.gantree.cab.spoken
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MailboxService : LifecycleService() {
  private var client: MailboxClient? = null
  private var target: Triple<String, String, String>? = null
  @Volatile
  private var geoCache: Pair<Geo, Long>? = null
  private val outbox = ArrayDeque<WireFrame>()
  private val outboxLock = Any()

  override fun onCreate() {
    super.onCreate()
    CabNotifier.ensureChannel(this)
    if (Build.VERSION.SDK_INT >= 34) {
      startForeground(CabNotifier.CONNECTED_ID, CabNotifier.connected(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(CabNotifier.CONNECTED_ID, CabNotifier.connected(this))
    }
    lifecycleScope.launch {
      while (true) {
        delay(SWEEP_EVERY_MS)
        sweep()
      }
    }
  }

  /**
   * Quiet catch-up: what another mouth of yours sent, or what a frozen socket
   * missed, folds into the thread by id and seq. No down state, no clear.
   */
  fun sweep() {
    val app = application as CabApp
    if (watchingThread(app.phoneResumed, app.carThreadVisible)) {
      client?.sweep()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    if (!connect()) {
      return START_NOT_STICKY
    }
    instance = this
    takePending().forEach { it(this) }
    return START_STICKY
  }

  override fun onTimeout(startId: Int) {
    giveUp(mailboxTimeoutHint())
  }

  override fun onTimeout(startId: Int, fgsType: Int) {
    giveUp(mailboxTimeoutHint())
  }

  override fun onDestroy() {
    client?.stop()
    client = null
    target = null
    if (instance === this) {
      instance = null
    }
    super.onDestroy()
  }

  private fun giveUp(hint: String) {
    val app = application as CabApp
    client?.stop()
    client = null
    target = null
    app.mouth.setUp(false)
    app.mouth.setHint(hint)
    stopSelf()
  }

  /** @return false if this start should not stay foreground. */
  private fun connect(): Boolean {
    val app = application as CabApp
    val slugRaw = app.prefs.slug
    val nowSec = System.currentTimeMillis() / 1000L
    val expired = app.prefs.session.isNotBlank() &&
      sessionExpired(app.prefs.sessionExp, nowSec)
    val bearer = app.prefs.bearer
    val origin = app.prefs.origin
    val blocked = mailboxConnectError(slugRaw, bearer, expired)
    if (blocked != null) {
      app.mouth.setUp(false)
      app.mouth.setHint(blocked)
      stopSelf()
      return false
    }
    val slug = parseSlug(slugRaw) ?: run {
      stopSelf()
      return false
    }
    val next = Triple(origin, slug, bearer)
    if (client != null && target == next) {
      return true
    }
    client?.stop()
    client = null
    app.mouth.setHint("Connecting to mailbox…")
    val mailbox = MailboxClient(
      onFrame = { frame ->
        val fresh = app.mouth.ingest(frame)
        if (frame.kind == "face") {
          refreshFace(app, slug)
        }
        if (frame.kind == "theme") {
          app.prefs.putRoomTheme(slug, app.mouth.roomTheme.value)
        }
        if (fresh && frame.spoken()) {
          val kind = frame.kind
          val body = notifyBody(frame.text, !frame.images.isNullOrEmpty())
          if (shouldPost(app.phoneResumed, app.carAttached, kind, app.carThreadVisible)) {
            CabNotifier.kitMessage(this, slug, body, app.face)
          } else if (shouldBuzz(app.phoneResumed, app.carAttached, kind)) {
            CabNotifier.buzzPush(this)
          }
        }
      },
      onState = { up ->
        app.mouth.setUp(up)
        if (up) {
          app.mouth.setHint("")
          flushOutbox()
        }
      },
      onError = { err, res ->
        app.mouth.setHint(mailboxSocketHint(res?.code, err.message))
      },
      onAuthLost = {
        app.prefs.signOut()
        giveUp(mailboxAuthLostHint())
      },
    )
    client = mailbox
    target = next
    // Ack from what is already on the device so the mailbox skips what we hold.
    val held = cursorOf(app.mouth.lines.value)
    held.id?.let { mailbox.remember(it, held.seq) }
    mailbox.start(origin, slug, bearer)
    refreshFace(app, slug)
    return true
  }

  private fun refreshFace(app: CabApp, slug: String) {
    val origin = app.prefs.origin
    val bearer = app.prefs.bearer
    if (origin.isBlank() || bearer.isBlank()) {
      return
    }
    val rev = app.mouth.avatarRev.value
    lifecycleScope.launch {
      app.face = app.avatar.fetch(origin, slug, bearer, rev)
    }
  }

  fun send(text: String, photo: String? = null) {
    Thread {
      sendBlocking(text, photo)
    }.start()
  }

  fun pin() {
    Thread {
      pinBlocking()
    }.start()
  }

  private fun sendBlocking(text: String, photo: String?) {
    val trimmed = applyEmoji(text, text.length, "send").text.trim()
    if (trimmed.isEmpty() && photo.isNullOrEmpty()) {
      return
    }
    val app = application as CabApp
    val gpsOn = app.prefs.gps
    val geo = if (gpsOn) peekGeo() else null
    val ctx = phoneContext(geo)
    sendGeoHint(gpsOn, geo)?.let { app.mouth.setHint(it) }
    val frame = app.outbound(trimmed, ctx, photo?.let { listOf(it) })
    client?.remember(frame.id ?: return)
    sendOrQueue(frame)
  }

  private fun pinBlocking() {
    val app = application as CabApp
    if (!app.prefs.gps) {
      app.mouth.setHint("GPS off")
      return
    }
    val geo = freshGeo() ?: peekGeo()
    app.mouth.setHint(geoHint(true, geo))
    if (geo == null) {
      return
    }
    val frame = pinFrame(phoneContext(geo))
    if (client?.send(frame) != true) {
      app.mouth.setHint("socket down — reconnecting")
    }
  }

  private fun sendOrQueue(frame: WireFrame) {
    if (client?.send(frame) == true) {
      return
    }
    synchronized(outboxLock) {
      while (outbox.size >= 50) {
        outbox.removeFirst()
      }
      outbox.addLast(frame)
    }
    (application as CabApp).mouth.setHint("socket down — reconnecting")
  }

  private fun flushOutbox() {
    val frames = synchronized(outboxLock) {
      val all = outbox.toList()
      outbox.clear()
      all
    }
    val leftover = ArrayDeque<WireFrame>()
    var sending = true
    for (frame in frames) {
      if (sending && client?.send(frame) == true) {
        continue
      }
      sending = false
      leftover.addLast(frame)
    }
    if (leftover.isNotEmpty()) {
      synchronized(outboxLock) {
        leftover.addAll(outbox)
        outbox.clear()
        outbox.addAll(leftover)
      }
    }
  }

  private fun phoneContext(geo: Geo?): PhoneContext {
    return PhoneContext(
      at = Instant.now().toString(),
      tz = TimeZone.getDefault().id,
      geo = geo,
      battery = peekBattery(),
      net = peekNet(),
      surface = surfaceHint((application as CabApp).carAttached),
    )
  }

  private fun peekBattery(): BatteryHint? {
    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
    val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val sticky = runCatching {
      registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()
    val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val plugged = (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
    val charging = plugged ||
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
      status == BatteryManager.BATTERY_STATUS_FULL
    return batteryHint(pct, charging)
  }

  private fun peekNet(): String {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return "unknown"
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "unknown"
    return netHint(
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
    )
  }

  private fun hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
  }

  /** Cached or last-known only. Never waits for a satellite lock on the send path. */
  private fun peekGeo(): Geo? {
    if (!hasLocationPermission()) {
      geoCache = null
      return null
    }
    val now = System.currentTimeMillis()
    geoCache?.let { (geo, at) ->
      if (now - at <= GEO_CACHE_MS) {
        return geo
      }
    }
    val got = lastKnownGeo() ?: return null
    geoCache = got to now
    return got
  }

  private fun lastKnownGeo(): Geo? {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
      return null
    }
    return try {
      val fused = LocationServices.getFusedLocationProviderClient(this)
      val loc = Tasks.await(fused.lastLocation, GEO_LAST_KNOWN_MS, TimeUnit.MILLISECONDS) ?: return null
      geoFromAndroid(loc)
    } catch (_: Exception) {
      null
    }
  }

  /** Explicit pin: one short current-location attempt, then last known. */
  private fun freshGeo(): Geo? {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
      return null
    }
    return try {
      val fused = LocationServices.getFusedLocationProviderClient(this)
      val loc = Tasks.await(
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
        8,
        TimeUnit.SECONDS,
      ) ?: return null
      val geo = geoFromAndroid(loc)
      geoCache = geo to System.currentTimeMillis()
      geo
    } catch (_: Exception) {
      null
    }
  }

  private fun geoFromAndroid(loc: Location): Geo = geoFromFix(
    lat = loc.latitude,
    lon = loc.longitude,
    accuracyM = loc.accuracy.toDouble(),
    altM = loc.altitude.takeIf { loc.hasAltitude() },
    heading = loc.bearing.toDouble().takeIf { loc.hasBearing() },
    speedMps = loc.speed.toDouble().takeIf { loc.hasSpeed() },
  )

  companion object {
    @Volatile
    private var instance: MailboxService? = null
    private val pending = ArrayDeque<(MailboxService) -> Unit>()
    private val pendingLock = Any()

    fun start(ctx: Context) {
      val i = Intent(ctx, MailboxService::class.java)
      ctx.startForegroundService(i)
    }

    fun stop(ctx: Context) {
      ctx.stopService(Intent(ctx, MailboxService::class.java))
    }

    fun sendText(ctx: Context, text: String) {
      sendTurn(ctx, text, null)
    }

    /** Caption and photo on one inbound. Attach itself never calls this. */
    fun sendTurn(ctx: Context, text: String, photo: String?) {
      sendBits(ctx) { it.send(text, photo) }
    }

    fun sendPin(ctx: Context) {
      sendBits(ctx) { it.pin() }
    }

    /** The thread just came onto a screen. Nothing to do when no socket is up. */
    fun sweep() {
      instance?.sweep()
    }

    private fun sendBits(ctx: Context, fn: (MailboxService) -> Unit) {
      val svc = instance
      if (svc != null) {
        fn(svc)
        return
      }
      synchronized(pendingLock) { pending.addLast(fn) }
      start(ctx)
    }

    private fun takePending(): List<(MailboxService) -> Unit> {
      synchronized(pendingLock) {
        val jobs = pending.toList()
        pending.clear()
        return jobs
      }
    }
  }
}
