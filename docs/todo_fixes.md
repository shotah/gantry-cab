# Fix now

Flaws found by reading the tree on 2026-09-10 (v0.1.6). Ordered by blast
radius. Each item names the file, why it matters, and the smallest fix.
Security-flavoured items are cross-listed in
[security_todo.md](security_todo.md); dev-loop items live in
[iteration_improvements_todo.md](iteration_improvements_todo.md).

## P0 — breaks in the field or loses data

- [x] **`dataSync` foreground service has no `onTimeout`; Android 15+ kills it after 6 h.**
  `MailboxService.onTimeout` now stops the socket, sets
  `mailboxTimeoutHint()`, and `stopSelf()`. Still open: switch the FGS
  type to `remoteMessaging` or add FCM wake so the socket can last a drive
  longer than 6 h.

- [x] **Bearer token is sent to half-typed hosts on every keystroke.**
  Avatar fetch now uses `_fetchOrigin` (updated only in `persist()`),
  skips an unparseable slug / blank bearer, and `AvatarApi.fetch` cancels
  the OkHttp call when the collector moves on.

- [x] **Messages are dropped silently when the socket is down.**
  `MailboxService.sendBlocking`: `app.outbound(...)` adds the bubble, then
  `client?.send(frame) != true` only sets a hint. The frame is gone, the UI
  shows it as sent. Same in `pinBlocking`.
  Fix: an outbox (`ArrayDeque<WireFrame>`) flushed in `MailboxClient.onOpen`
  after the `ackSince`, or mark the `ChatLine` failed (`kind = "failed"`) with
  a tap-to-retry. Test the flush order with `MockWebServer`.

- [x] **Reconnect loop never backs off and never stops.**
  `mailboxShouldRetry` / `mailboxRetryDelayMs` (2 s → 60 s, stop on 401/403/404).
  `CabPrefs.sessionExp` + `liveBearer` refuse an expired Google session.

## P1 — bugs and races

- [x] **400 ms race in `MailboxService.sendBits`.** Sends now queue on a
  pending list and flush from `onStartCommand` after `instance` is set.
  Still not an outbox: the socket may not be open yet.

- [x] **Every `MainActivity.onCreate` reopens the WebSocket.**
  `connect()` returns early when a client is already up for the same
  `(origin, slug, bearer)`.

- [x] **`MailboxClient` mutates `seen` / `lastSeenId` from two threads.**
  `synchronized(seen)` around add/evict; `@Volatile lastSeenId`.

- [x] **Each send blocks up to 4 s on GPS.** Text send uses `peekGeo()`
  (`lastLocation` + 120 s cache). Pin still waits for a current fix.

- [x] **Android Auto screen never refreshes.** `CabThreadScreen` collects
  `mouth.lines` and `invalidate()`s. `setTitle` stays (minCarApiLevel 1;
  `setHeader` needs a higher Car API).

- [x] **Notification history is one message deep.** Last 6 turns per slug
  in `MessagingStyle`; cleared on mark-as-read.

- [x] **Foreground "Listening for Kit" shows even when nothing connects.**
  Blocked connect path `stopSelf()`s.

- [x] **`signIn` swallows coroutine cancellation.** Rethrows
  `CancellationException`; catches `NoCredentialException` /
  `GetCredentialException` explicitly.

- [x] **Main-thread disk writes.** `CabPrefs.write` / `signOut` use `apply()`.

- [x] **Server-supplied crane names are trusted.** `AuthApi.me` filters
  through `parseSlug`; upload/persist drop the `?: slug` fallback.

- [x] **Inbound image payloads are unbounded.** `decodeDataUrl` /
  `acceptInboundImage` cap at `IMAGE_BYTES_MAX`; text capped at
  `TEXT_CHARS_MAX` (8000).

- [x] **`versionCode` collides at minor or patch ≥ 100.**
  `require(minor in 0..99 && patch in 0..99)`.

## P2 — housekeeping

- [x] Delete stale Paparazzi output: `app/src/test/snapshots/images/*.png`
  (for `AutoShotTest` / `ShotTest`, which no longer exist) and
  `app/build/paparazzi/`. `assets/docs/` is the real shot output.
- [x] `ReplyService.EXTRA_SLUG` is set by `CabNotifier.serviceIntent` and
  never read. Remove it (or use it) so nobody starts trusting it.
- [x] `.env` carries an `ANDROID_GOOGLE_WEB_CLIENT_ID` key that nothing
  reads (`build.gradle.kts` reads `CAB_GOOGLE_WEB_CLIENT_ID` /
  `cab.googleWebClientId`). Remove the key so a future reader does not think
  it is wired.
- [x] `gantry-cab-0.1.5.apk` (51 MB) sits at the repo root while `VERSION`
  is `v0.1.6`. Gitignored, but stale; move to `dist/` or delete.
- [ ] `androidx.fragment:fragment-ktx` looks unused (no
  `androidx.fragment` import) but lint `InvalidFragmentVersionForActivityResult`
  requires Fragment ≥ 1.3.0 for `registerForActivityResult`. Leave it.
- [ ] Dependency drift flagged by the last lint run: `credentials` 1.3.0 →
  1.6.0, `lifecycle-*` 2.8.7 → 2.11.0, `activity-*` 1.10.0 → 1.13.0,
  `core-ktx` 1.15.0 → 1.19.0, `googleid` 1.1.1 → 1.2.0,
  `play-services-location` 21.3.0 → 21.4.0, Gradle 9.6.0 → 9.7.1. Bump, and
  add Dependabot (see iteration doc) so this stops accumulating.
- [ ] `DocsShotTest` asserts pixel colours at hard-coded coordinates
  (`img.getRGB(200, 500)`), so any layout tweak in `DocsShot.kt` fails
  tests that say nothing about behaviour. And `DocsShot.kt` (500 lines) is a
  hand-painted copy of the Compose UI, so the README screenshots drift from
  the app. Replacement path is in the iteration doc.
- [x] `MainActivity.requestBits` asks for location on every `onCreate` when
  not granted. Ask when the user turns GPS on in the attach menu instead.
- [ ] Kotlin incremental cache corruption was observed this session
  (`app/build/kotlin/compileDebugKotlin/.../source-to-classes.tab_i`),
  which forces a 20 s full recompile. Cause is two Gradle clients (Studio +
  CLI/pre-commit) writing the same `app/build`. Not a code bug — see the
  iteration doc for the workflow fix.

## Verify with

```bash
make test-scripts
./gradlew testDebugUnitTest lintDebug createDebugUnitTestCoverageReport
make coverage-gate
```
