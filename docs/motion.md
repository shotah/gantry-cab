# Motion — Kit hears when you leave and when you arrive

Two unprompted lines from the phone: **departure** when a trip has
really started, **arrival** when you have really stopped. No route,
no polling, no continuous GPS. The OS does the sensing; the Cab does
one small decision and one frame.

Kit already gets `geo` (with `speed_mps` and `heading`) on every
`inbound` and on an explicit `pin`. That is location *when you
speak*. This is location *when you move*, so Kit can go from
answering to anticipating: "heading to the gym?", "you're home —
the door is unlocked", "you've been at the trailhead 20 minutes."

Side effect worth naming: a **departure starts `MailboxService`**.
"Open Cab and send a line before you plug in" stops being a rule the
human has to remember. That is the Auto win FCM was going to buy,
without FCM.

Mailbox contract: [pendant_handoff.md](pendant_handoff.md). Service
lifecycle and why there is no always-on socket:
[fcm_design_and_todo.md](fcm_design_and_todo.md).

---

## 0. Decisions

| Question | Answer |
| --- | --- |
| Stack | Kotlin, `play-services-location` (already a dependency). Not Expo; `todo.md` says so. |
| Motion sensing | **Activity Recognition Transition API** — `IN_VEHICLE` / `ON_BICYCLE` / `WALKING` / `STILL` enter + exit. Sensor-fused, wakes a dead process, tells bike from car. |
| Place sensing | **Geofencing API** — an *anchor* pair at the last stop (150 m + 500 m, `EXIT`) and one *dwell* fence at a candidate stop (150 m, `DWELL`, 8 min loiter). The OS runs the timer. |
| Head unit | `CabApp.carAttached` (`CarConnection`, already observed). **Attached suppresses** stop candidates; **detach is a candidate**, not an instant arrival. Same 8 min dwell. |
| Bluetooth | No. Earbuds and watches drop too, `BLUETOOTH_CONNECT` on 31+, and a "which one is the car" picker. `CarConnection` covers the car; bikes and feet have no car. |
| Speed / distance rules | Gone as *sensors*. The transition API replaces "15 km/h for 2 min"; the anchor fences replace "1 km from origin." `speed_mps` still rides on the frame for Kit. |
| Wire | v1 rides on `pin` with additive `context.motion` + `context.mode`. v2 is a real `kind` on the Worker. |
| Consent | Own toggle, default off, per device. `ACCESS_BACKGROUND_LOCATION` ("Allow all the time") + `ACTIVITY_RECOGNITION`. Not a widening of "GPS on send." |
| Decision code | `mailbox/Motion.kt`, Android-free, JVM-tested on traces. The receiver and service stay thin. |

---

## 1. Success

- Drive to work with Auto: one `departure` within ~2 min of pulling
  out, one `arrival` ~8 min after the head unit drops. Red lights,
  Mercer, the I-5 merge: nothing.
- Same drive with no head unit: same two frames. A 3 min stop for
  gas: nothing.
- Bike to a trailhead, 5 min water stop on the way: `departure`
  (`mode: bike`), `arrival` at the trailhead, nothing in between.
- Walk the dog around the block: nothing. Walk past 500 m: a
  `departure` on foot; `arrival` home after.
- Phone locked, Cab closed, process dead before the drive: still
  works. Departure brings the socket up; Auto cards work for the
  rest of the drive.
- Toggle off: no receivers armed, no fences, no frames. Same APK
  behaves exactly as today.
- An old crane / Worker that has never heard of `motion` sees a
  `pin` with extra keys it ignores. Nothing breaks.

---

## 2. Already true (do not rebuild)

| Piece | Keep |
| --- | --- |
| `PhoneContext` / `encodeFrame` | `geo { lat, lon, accuracy_m, alt_m, heading, speed_mps }`, `at` (UTC ISO), `tz`, `battery`, `net`, `surface`. The frame shape is done. |
| `pinFrame(context)` | Control frame. `Mouth.ingest` does not paint it. |
| `MailboxService.freshGeo` / `peekGeo` | One-shot `getCurrentLocation(BALANCED)` with an 8 s cap; cached last-known for 2 min. |
| `MailboxService.sendBits` | Starts the FGS if the process is dead and runs the job on connect. `sendOrQueue` + `flushOutbox` hold up to 50 frames across a down socket. |
| `CabApp.carAttached` | `CarConnection(this).type.observeForever`. Fires while the process lives — which it does during a drive, because departure started it. |
| `surfaceHint(carAttached)` | `android` / `android_auto` already rides on every frame. |
| Settings → Access | Mic / location / notifications rows read `Permits`. Two more rows, same pattern. |
| `specialUse` FGS, not on boot | Stays. Motion never starts the socket except to send, and stops it when done (§4.6). |

---

## 3. Non-goals

- Continuous or periodic location. No `requestLocationUpdates` in
  the background, no "every 5 minutes."
- Naming places. The phone sends lat/lon; "home", "gym", "work" are
  crane memory. No on-device `Geocoder`.
- Route history on the phone or the Worker. The only stored
  location is the current anchor.
- Bluetooth scanning or pairing lists.
- Predicting the destination. `heading` + the first 500 m is Kit's
  problem if it wants one.
- iOS. Helm gets the same wire from `CLVisit` + `CMMotionActivity`
  in its own checkout.
- The ~200 m walk. The nearest real trip in the family is about one
  fence radius door to door. §7 says why it is marginal and what
  would actually fix it. Do not shrink the fences to chase it.

---

## 4. Design

### 4.1 Evidence, not sensors

Three sources, all delivered to one `BroadcastReceiver`
(`drive/MotionReceiver`) or already in the process:

```text
Transition API   IN_VEHICLE / ON_BICYCLE / WALKING / RUNNING / STILL   × ENTER / EXIT
Geofences        anchor-near (150 m, EXIT+ENTER)  anchor-far (500 m, EXIT)  dwell (150 m, DWELL+EXIT)
CarConnection    attached / detached                                     (process alive only)
```

Every event is an `Evidence` value fed to `Motion.step`. The pure
function returns the next state and a list of `Effect`s: plant a
fence, drop a fence, ask for one fix, emit a frame. The receiver
executes effects; it never decides.

### 4.2 States

```text
STOPPED(anchor)
   │ anchor-near EXIT
   ▼
MOVING(departed=false)  ── anchor-near ENTER ──► STOPPED   (silent: driveway, around the block)
   │ IN_VEHICLE / ON_BICYCLE ENTER   or   anchor-far EXIT
   │   → emit departure, drop anchor fences
   ▼
MOVING(departed=true)
   │ candidate: STILL ENTER / vehicle-or-bike EXIT / WALKING ENTER  (ignored while carAttached)
   │            carAttached → false                                 (always)
   │   → one fix → plant dwell fence here
   ▼
ARRIVING ── IN_VEHICLE / ON_BICYCLE ENTER  or  dwell EXIT ──► MOVING   (drop dwell; gas, coffee, red light)
   │ dwell DWELL (8 min)
   │   → emit arrival, anchor := here, plant anchor fences
   ▼
STOPPED(anchor)
```

Rules that fall out of it:

- **Departure needs two things:** you left the 150 m anchor *and*
  either the OS says vehicle/bike or you passed 500 m. Moving the
  car across the street, biking around the block, walking the dog:
  one of the two is missing, nothing is sent.
- **A candidate is cheap to be wrong about.** It costs one balanced
  fix and one fence. Anything that looks like motion again drops it.
- **Arrival is only ever the dwell fence firing.** No shortcuts.
  Head-unit detach, `STILL`, walking away from the car — each just
  plants the fence. This is the "detach plus a delay" you were
  reaching for: the delay is the same 8 min for every trigger, so
  gas and coffee are filtered the same way regardless of how the
  stop was noticed.
- **Attached means engine on.** While `carAttached`, `STILL` and
  vehicle `EXIT` are not candidates. A drawbridge, a ferry line, a
  20 min Mercer standstill: nothing, as long as Auto is up. Without
  Auto those become false arrivals at 8 min, followed by a departure
  when you roll — Kit reads a stop (§4.5).
- **Arrivals may stand alone.** `MOVING(departed=false)` can reach
  `ARRIVING`: a walk to somewhere 200–500 m away with no vehicle.
  Kit sees `arrival` with no `departure` before it. The crane must
  not assume pairing.
- Vehicle/bike `ENTER` while `STOPPED` only records the mode. It is
  not evidence of leaving — that is what the near fence is for.

### 4.3 Constants

All in `mailbox/Motion.kt`. Tests reference them by name, not by
number.

| Constant | Value | Why |
| --- | --- | --- |
| `ANCHOR_NEAR_M` | 150 | Google's recommended geofence floor is 100–150 m. Below that a parking garage or a two-storey gym drifts you out. |
| `ANCHOR_FAR_M` | 500 | Departure on foot with no vehicle evidence. ~6 min walking. |
| `DWELL_RADIUS_M` | 150 | Same floor. |
| `DWELL_MS` | 8 min | Clears gas (3–5), most espresso (5–8), a red light, a platform wait. A 15 min café is an arrival — and then a departure; that pair *is* the information. |
| `DWELL_DETACHED_MS` | `= DWELL_MS` | A knob, not a feature. Shorten only after a month of frames says detach never lies. |
| `FENCE_RESPONSIVENESS_MS` | 60 s | Android 8+ background location gives geofences ~2 min latency anyway. Do not chase seconds. |
| `FIX_FRESH_MS` | 60 s | Reuse `lastLocation` younger than this for the candidate fence instead of a new fix. |
| `IDLE_STOP_MS` | 2 min | Motion-owned socket stops this long after the outbox drains (§4.6). |

### 4.4 Wire (v1 — no Worker change)

A `pin` with two additive `context` keys. Closed sets; junk is
dropped on the way out (`motionOnWire`, `modeOnWire`), same as
`surface` / `input`.

```json
{
  "kind": "pin",
  "id": "…",
  "context": {
    "at": "2026-09-17T16:15:00Z",
    "tz": "America/Los_Angeles",
    "geo": { "lat": 0.0, "lon": 0.0, "accuracy_m": 18.0, "speed_mps": 0.0 },
    "surface": "android",
    "motion": "arrival",
    "mode": "vehicle"
  }
}
```

- `motion`: `departure` | `arrival`.
- `mode`: `vehicle` | `bike` | `foot`. Omitted when the transition
  API never said (activity permission denied, or nothing fired
  before the far fence). Transit is `vehicle`.
- `geo` is the fence's `triggeringLocation` for arrival and the
  far/near exit or a fresh fix for departure. Use the existing keys
  (`accuracy_m`, not `accuracy`); do not add `latitude` /
  `longitude`.
- `at` on **arrival** is when the stop *began* (the candidate fix),
  not when the dwell confirmed it — Kit should not have to subtract
  8 minutes. Departure `at` is now.
- `id` is a fresh UUID so the outbox flush after a reconnect cannot
  double-deliver. `pinFrame` has no `id` today; adding one is
  additive.
- `battery` / `net` / `surface` ride along as they already do.
- Not `inbound`. Not fanned to siblings as a bubble. Not `text`.
- **Open question for pendant:** confirm the Worker forwards a
  `pin`'s full `context` to the crane rather than whitelisting `geo`.
  If it whitelists, v1 needs one pendant line before Kit can see the
  keys. Until the crane reads `motion`, an automatic pin looks like
  a dropped pin — Kit may answer it. That is the reason v2 exists.

v2, when a month of pins says the signal is worth a contract:
`kind: "motion"` on the Worker, never hydrate-replayed (or replayed
with `replay: true` so nothing chimes), a `[motion]` stamp on the
crane the way `[surface]` / `[input]` are. Pendant names it; Cab
matches. Not this checkout.

### 4.5 What Kit should do with it (crane, not Cab)

Written here so the wire is designed for it; the work is
`ai-gantry`.

- Ambient stamp, not a turn. `[motion: arrival vehicle]` next to
  `[surface]`. Do not reply unless there is something to say.
- Name the place from memory ("home", "the gym", "Alki"). The phone
  never will.
- `arrival` then `departure` within ~15 min is a **stop**, not a
  destination. Coffee, gas with the engine off, a ferry line, a
  drawbridge without Auto.
- Do not assume pairing. Arrivals stand alone (§4.2).
- `at` on arrival is the start of the stop.

### 4.6 Process and socket

The engine must work with the process dead and must not bring back
an always-on socket. Both are already policy
([fcm_design_and_todo.md](fcm_design_and_todo.md) §4.6).

- **`MotionReceiver`** takes the transition and geofence
  `PendingIntent`s (`getBroadcast`, `FLAG_MUTABLE` on 31+ — Play
  Services fills the extras). Play Services wakes the process for
  each event. It loads `MotionState` from prefs, runs `step`, runs
  the effects, saves state.
- **Fix** for a candidate: `lastLocation` if younger than
  `FIX_FRESH_MS`, else `getCurrentLocation(BALANCED)` inside
  `goAsync()` with the same 8 s cap `freshGeo` uses. Arrival and
  departure frames need no fix — the geofence event carries
  `triggeringLocation`.
- **Send** goes through `MailboxService.sendBits { it.motion(frame) }`.
  Android 12+ lets a geofence / activity-transition event start a
  foreground service from the background (it is on the exemption
  list). `motion()` uses `sendOrQueue`, not the fire-and-forget path
  `pin()` uses, so a down socket queues the frame.
- **Idle stop.** `MailboxService` learns who started it: `user`
  (Activity, Connect, send, spoken reply, car app) or `motion`. A
  motion-owned session stops itself `IDLE_STOP_MS` after the outbox
  drains **if** nothing else wants the socket: `!phoneResumed`,
  `!carAttached`, outbox empty. Any user start upgrades the owner to
  `user`, and user sessions keep today's behaviour exactly — a
  locked phone in a pocket still gets Kit's HUN. Departure with a
  head unit attached therefore keeps the socket up for the drive,
  which is the point; a bike departure sends one frame and goes
  quiet two minutes later.
- **Boot.** Fences and transition registrations do not survive a
  reboot. `MotionReceiver` also takes `BOOT_COMPLETED` /
  `MY_PACKAGE_REPLACED` and, only if the toggle is on, re-registers
  from the saved anchor. No fix, no socket, no FGS, no
  notification. This is not "boot relisten" — that ticket was about
  holding a WebSocket from boot. This is asking Play Services to
  keep watching. `fcm_design_and_todo.md` §4.6 should say so when
  this lands.
- **Disarm** (drop fences, remove transition request, clear state)
  on: toggle off, sign-out / `4401`, location or activity permission
  revoked, location services off. Re-arm when the toggle is on and
  the human is signed in with the grants.

### 4.7 Permissions and Settings

```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver android:name=".drive.MotionReceiver" android:exported="true">
  <!-- Explicit PendingIntents from Play Services need no filter.
       BOOT_COMPLETED is a protected broadcast; only the system sends it. -->
  <intent-filter>
    <action android:name="android.intent.action.BOOT_COMPLETED" />
    <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
  </intent-filter>
</receiver>
```

No new FGS type. The socket is still `specialUse`; the engine never
holds location in the service.

**Settings → Trips.** One switch, default off, per device:

> **Tell Kit when I leave and arrive**
> One line when you leave a place, one when you've stayed somewhere
> 8 minutes. Nothing in between. Needs location "all the time" and
> physical activity.

Turning it on walks the grants in order: fine location (if not
already), then background location (Android 11+ opens system
settings — the switch shows "needs Allow all the time" until it
comes back granted), then physical activity. Any denial leaves the
switch off with that hint. **Access** gets two rows: *Location —
all the time* and *Physical activity*. First arm takes one fix and
makes it the anchor; you are presumably home.

`CabPrefs`: `motion` (`on` / `off`), `motionState` (the serialized
`MotionState`: phase, anchor, mode, `departed`, candidate). Nothing
else. Wrap with the rest of credentials-at-rest when that ticket
happens; until then it is one lat/lon, no worse than the JWE.

---

## 5. Battery

What it costs, and how you know:

- Transition API: what the OS already spends for driving mode.
- Fences: two at rest, three while arriving. Fence evaluation is
  cell / Wi-Fi unless a fence edge is near.
- One balanced fix per *candidate* — a handful a day. Zero fixes for
  the frames themselves.
- FGS only around events: ~2 min for a bike / foot event; the drive,
  with a head unit.

Measure before trusting: a week of the sideload with the switch on
vs off, Settings → Battery per-app. If candidates are firing at
every red light without Auto, the first knob is *not* a smaller fix
— it is requiring a vehicle `EXIT` to be ≥ 60 s old before spending
one (an alarm, cancelled by a vehicle `ENTER`). Do not build that
until the numbers say to.

Doze and OEM killers: same as FCM. Play Services delivers these
events under Doze; Samsung "sleeping apps" and Xiaomi will still
stop the receiver. "Unrestricted battery" stays in the Auto setup.

---

## 6. Security and privacy

Threat model is unchanged — a phone that speaks for the operator —
plus one new fact: **the phone now sends location without a tap.**

- Off by default. Per device. Plain copy says exactly what goes out
  and when. Family beta means the family said yes on their own
  phone.
- Same `context.geo` shape that already rides on a send. No new
  precision, no history, no route.
- Two frames per trip. Someone reading the room learns where the
  phone stops, which a pin already told them. They do not learn the
  path.
- The Worker stores nothing new for v1. The crane's transcript
  holds the frames the same way it holds pins.
- Spike (`MAILBOX_SECRET`) sessions: allowed, lab only, same as pins.
- Sign-out / `4401` disarms. A yanked phone stops reporting.
- Not fanned to siblings as a thread bubble. Sibling phones do not
  see each other's trips unless the crane chooses to say so.
- `BOOT_COMPLETED` re-arm reads one anchor from prefs and talks to
  Play Services. It opens no socket and needs no credential.

---

## 7. Thresholds and the cases

Your list, plus the Seattle ones.

| Case | What happens | Frames |
| --- | --- | --- |
| Driveway shuffle, moving the car | vehicle `ENTER`, never leaves 150 m, `STILL` | none |
| Slow traffic (Mercer, I-5 merge) | still `IN_VEHICLE`; crawling clears 150 m in minutes anyway | none |
| Stopped traffic, Auto attached | `STILL` ignored while attached | none |
| Stopped traffic > 8 min, no Auto | candidate at 150 m → dwell fires | false `arrival`, then `departure` — Kit reads a stop |
| Drawbridge / I-90 tunnel closure | as above | none with Auto; a stop without |
| Ferry line | as above, 20+ min | `arrival` at the dock; `departure` on the far side |
| Gas, engine off, 3–5 min | candidate (detach or `WALKING`) → vehicle `ENTER` drops it | none |
| Coffee 6 min | same | none |
| Coffee 15 min | dwell at 8; near exit + vehicle on leaving | `arrival`, `departure` 7 min later |
| Bike, 5 min trail stop | bike `EXIT` → candidate → bike `ENTER` drops it | none |
| Bike to a trailhead, 20 min | dwell | `arrival` (`mode: bike`) |
| Red light on a bike (90 s) | may cost a candidate fix; dwell never fires | none |
| Dog walk around the block | near `EXIT`, near `ENTER` | none |
| Walk past 500 m | far `EXIT` | `departure` (`foot`); `arrival` home later |
| Transit: walk, train, walk | platform wait is a candidate the train drops; train is `vehicle` | `departure` (`vehicle`), `arrival` (`vehicle`) |
| Bus stop wait > 8 min | dwell | false `arrival` at the stop, then `departure` |
| Phone left in the car overnight | `STILL` → dwell | `arrival` where the car is — correct |
| Toggle on at home | one fix → anchor | none |
| Reboot | re-arm from prefs | none |

**The ~200 m walk.** Home to the climbing gym in this family is
about one fence radius as the crow flies, ~300 m on foot. With
Wi-Fi-assisted fixes at both ends (two buildings, 20–50 m accuracy)
the near fence will usually register the exit and the dwell fence
will usually fire, and you will get a standalone `arrival`
(`foot`). Sometimes it will not. Do not shrink `ANCHOR_NEAR_M` to
100 to make it reliable: 100 m is Google's floor, indoor GPS in a
two-storey gym next to a garage drifts further than that, and the
two fences would overlap. The thing that resolves places 200 m
apart with no location at all is **known Wi-Fi networks as
places** (home SSID gone = left home; gym SSID joined = at the
gym). That is a separate, later feature (§9). Until then Kit learns
you are at the gym the way it does today: you tell it.

Why the spec's numbers moved:

- **50 m radius** → 150. Below the platform's floor and inside GPS
  drift.
- **15 km/h for 2 min** → transition API. The speed rule needed
  location samples every ~30 s for two minutes, which is the
  battery cost the spec was trying to avoid, and it fires late on a
  crawl and misses a slow bike start. `speed_mps` still rides on the
  frame.
- **1 km from origin** → 150 m + (vehicle/bike or 500 m). One
  kilometre was hiding every trip inside the neighbourhood.
- **Bluetooth disconnect = instant stopped** → `CarConnection`
  detach = one more candidate, same dwell. Detach means engine off,
  not trip over; gas with the engine off would have fired.
- **8 min dwell** stays. It is the one number in the spec that was
  already right.

---

## 8. Tests

`test/…/mailbox/MotionTest.kt`, JVM, no Robolectric. Each row in
§7 is a trace: a list of `(offsetMs, Evidence)` fed through `step`,
asserting the emitted frames and the final state. Fence
`triggeringLocation`s are literal `Geo`s; no Android types.

Also:

- `MotionState` ↔ JSON round-trip (what `CabPrefs.motionState`
  holds). Junk → fresh `STOPPED` with no anchor, never a crash.
- `WireTest`: `motionOnWire` / `modeOnWire` closed sets; a pin with
  `motion` encodes the keys; a pin without does not; `id` present.
- `CabPrefs` toggle default off; state survives a re-read.
- `MailboxService` owner: motion-started + `!resumed` +
  `!carAttached` + empty outbox → stops; user start upgrades and
  never idle-stops. Extract the decision (`shouldIdleStop`) into
  `Motion.kt` so it is a JVM test, per the `todo.md` rule of
  extracting decisions over more Robolectric.

The receiver and the Play Services calls are thin enough to walk,
not unit-test. Walk is the gate:

1. Switch on at home. Settings shows both grants green. No frame.
2. Drive to work with Auto. Crane transcript shows one `pin` with
   `motion: departure`, `mode: vehicle` within ~2 min; Auto cards
   work without having opened Cab. One `arrival` ~8 min after
   unplugging. Nothing in between across at least one long light.
3. Same drive, phone not plugged in. Same two frames. Stop for gas
   on the way: nothing.
4. Bike somewhere with a stop. `mode: bike`, one arrival.
5. Walk around the block. Nothing. Walk to the store past 500 m.
   `departure` on foot.
6. Force stop Cab, reboot, drive. Still two frames.
7. Switch off. Drive. Nothing. `dumpsys location` shows no Cab
   fences.
8. Sign out with the switch on. Drive. Nothing.
9. A week on: Settings → Battery, Cab is not in the top ten.

---

## 9. Later

- **Known Wi-Fi networks as places.** Home / gym / work SSID join
  and leave as evidence. Zero location, resolves the 200 m case,
  and is the strongest "actually home" signal there is. Needs the
  location grant to read the SSID (Android 8+ ties them) and a
  Settings list of "these networks are places." Own doc when it is
  the ticket.
- **`kind: motion`** on the Worker (§4.4 v2). Pendant names it.
- **Helm.** `CLVisit` is the iOS dwell fence, `CMMotionActivity`
  the transition API. Same two frames, same keys.
- **Departure alarm gate** (§5) if candidates are expensive without
  Auto.
- **Stay attached.** If a departure starts the socket and a head
  unit is seen, keep it until detach — already what the owner rule
  does.

---

## 10. Todo

Ordered so each box is walkable on its own. Nothing below changes
the Worker.

### Cab (`gantry-cab`)

- [ ] **`mailbox/Motion.kt`.** `Evidence`, `Effect`, `MotionState`,
      `step`, constants (§4.3), `motionOnWire` / `modeOnWire`,
      state JSON round-trip, `shouldIdleStop`. `MotionTest` with
      every §7 row as a trace. No Android imports.
- [ ] **`Wire.kt`.** `PhoneContext.motion` / `.mode`; `encodeFrame`
      writes them through the closed sets; `pinFrame` takes an
      optional `id`. `WireTest`.
- [ ] **`CabPrefs.motion` / `motionState`.** Default off. Junk state
      → fresh.
- [ ] **`drive/MotionReceiver`.** Transition + geofence
      `PendingIntent`s (`FLAG_MUTABLE`), `BOOT_COMPLETED` /
      `MY_PACKAGE_REPLACED` re-arm, `goAsync` fix, effects →
      `GeofencingClient` / `MailboxService.sendBits`. Thin.
- [ ] **`MailboxService.motion(frame)`** via `sendOrQueue`;
      `startedBy` owner; idle stop for motion-owned sessions only.
      `CabViewModel` / `MainActivity` / `CabCarAppService` starts
      mark `user`.
- [ ] **Manifest.** Four permissions, one receiver (§4.7). No new
      FGS type.
- [ ] **Settings → Trips** switch + grant walk; **Access** rows for
      background location and physical activity. `Permits` grows two
      fields. Copy as §4.7.
- [ ] **Docs.** `screens.md` (Settings shot), `sideload_to_android.md`
      ("Location is optional" → two sentences), `todo.md` link,
      `fcm_design_and_todo.md` §4.6 note that a re-arm receiver is
      not boot relisten.
- [ ] **Walk** §8 on the sideload. A week of battery before the
      switch is mentioned to the family.

### Pendant (`gantry-pendant`) — v1 has one question, v2 has work

- [ ] Confirm `pin` `context` reaches the crane whole (§4.4). If it
      is whitelisted to `geo`, forward `motion` / `mode`.
- [ ] Later: `kind: motion`, not hydrated as a chime, not fanned as
      a sibling bubble. `frontends.md` row.

### Crane (`ai-gantry`)

- [ ] Read `context.motion` / `context.mode` off a `pin` into a
      `[motion]` stamp. Ambient, not a turn (§4.5).
- [ ] Place naming from memory; stop-vs-destination rule; no pairing
      assumption.

---

## 11. Watch

- **Do not send `seq` / `at` at the top level.** `at` inside
  `context` is ours and is the start of the stop for arrivals.
- **Do not paint motion pins in the thread.** `Mouth.ingest` already
  drops `pin`; keep it that way. The PWA is pendant's call.
- **Do not make detach an instant arrival.** It is a candidate. If a
  month of frames says detach never lies, shorten
  `DWELL_DETACHED_MS`; do not skip the fence.
- **Do not add `requestLocationUpdates` to "improve" anything.** The
  moment there is a location stream in the background, this is a
  tracker and the battery story is gone.
- **`FLAG_MUTABLE`.** Both `PendingIntent`s. Play Services fills the
  extras; an immutable one delivers empty events on 31+.
- **`INITIAL_TRIGGER_DWELL`** on the dwell fence, or the OS waits for
  an `ENTER` that already happened.
- **Fences vanish** on reboot, on location toggled off, on app data
  clear. Re-arm handles boot; the Settings row handles the rest.
- **Two fences 185 m apart with 150 m radii overlap.** That is why
  the ~200 m walk is "usually," not "always."
- **A standalone `arrival` is legal.** A crane that waits for the
  matching `departure` will wait forever on foot trips.
- **Idle stop must never touch a user session.** If a pocket HUN
  stops arriving after this lands, the owner flag is wrong.
