# Todo

Open work only. Merged from the 2026-09-10 fix / parity / security /
loop lists. Ordered **small → large**; within a size, **security high →
low**. A `v*` tag must always attach an installable APK — never fail the
release job for a missing Play keystore, R8, pinning, or Tink.

Mailbox checkout handoff (2026-09-12 Worker pass + docs pass: CSRF,
config `version`, native nonce issued-not-required, sibling fan-out,
hydrate): [pendant_handoff.md](pendant_handoff.md). Cab half of nonce
+ 4401 is in tree; remaining boxes are a tagged APK and walking
sibling inbound on the deployed origin. Do not wait for a pendant
agent to edit Kotlin.

Threat model: a phone that holds a **credential for the crane's room**
(Google session JWE or `MAILBOX_SECRET`) and a socket that **speaks for
the operator**. Losing the credential, sending it to the wrong host, or
letting another app read the thread are the failures that matter.

| Track | Rules |
| --- | --- |
| **Development** | Debug APK. Cleartext only to loopback. Spike may persist on loopback. Car host `ALLOW_ALL` (DHU). |
| **Sideload (the product)** | GitHub Release. `https://` Worker. Session expiry, payload caps, slug checks, car host allowlist, no cleartext, GPS off until toggled. Debug-signed is fine. |
| **Play Store** | Not a goal. Do not block sideload to chase a listing. Only exception worth knowing: a Cab **tile** in a real car needs a Play internal-testing install — Auto's Unknown sources does not admit Car App Library apps. |

## Small

- [x] **Strip harness / clock header from inbound `text` on send.**
      `stripHarnessContext` in `mailbox/Text.kt` (same rules as
      ai-gantry `harness.go` / pendant `lib/phone/text.ts`). `inbound()`
      strips before `capWireText`. Bubble uses the stripped string.
      `context` JSON is untouched. `WireTest` / `CabAppTest`.
- [x] **Transcript hydrate** — mailbox replays last 80 `inbound` /
      `reply` / `push` on connect with additive `replay: true`.
      `Mouth.ingest` already paints; `shouldSpeak(kind, replay)` skips
      Auto HUNs. Ship this APK with the pendant Worker that hydrates.
- [ ] **"Drop a pin" launcher shortcut.** Parity P2. Pendant has a PWA shortcut `/?pin=1`. Cab already pushes a conversation shortcut; add a static `shortcuts.xml` that starts `MailboxService.sendPin`.
- [ ] **Drop `DocsShotTest` coordinate assertions.** `img.getRGB(200, 500)` fails on any layout tweak and says nothing about behaviour. Keep a "renders without throwing" test until the screenshot replacement lands.
- [ ] **Tag slow/IO tests** (MockWebServer, Java2D) with a JUnit `Category` so `make watch` can exclude them.

## Medium

- [ ] **Wrap credentials at rest.** High. `CabPrefs` writes the session JWE, `MAILBOX_SECRET` and email to plain `shared_prefs/cab.xml`. Backup is off; root, a debug `run-as`, or a dump still reads both. Tink `AndroidKeysetManager` (what `EncryptedSharedPreferences` used; `androidx.security:security-crypto` is deprecated). Partial now: Google sign-in clears the spike; debug persists the spike only for `10.0.2.2` / `localhost` / `127.0.0.1`.
- [ ] **`gradle/verification-metadata.xml`.** Medium. Generate with `./gradlew --write-verification-metadata sha256` and commit. Wrapper jar is already validated by `setup-gradle@v4`.
- [ ] **Release minify + shrink.** Low. `isMinifyEnabled = true`, `isShrinkResources = true`. Faster `adb install` of the 51 MB APK; strips unused code and symbol names. Keep `proguard-rules.pro` for OkHttp/Compose. Debug stays unminified. Not a release-job gate.
- [ ] **ktlint + `.editorconfig`.** `indent_size = 2`, `max_line_length = 120`, `ktlint_code_style = intellij_idea`. Plugin `org.jlleitschuh.gradle.ktlint` (or detekt with formatting). Run in pre-commit (sub-second warm). Drop `kotlin.code.style=official` or align to 4-space.
- [ ] **Relisten on car connect.** `BootReceiver` covers reboot and APK update; `specialUse` removed the 6 h `dataSync` mute. Still open: a *Force stop* or an OEM battery killer leaves the car silent until Cab is opened. `CarConnection` only reports while the process lives, so this needs a manifest-safe wake (Bluetooth `ACL_CONNECTED` to the head unit, or FCM).
- [ ] **Walk sibling inbound.** Worker fan is shipped (`siblingPhoneTag` → `sub:<userId>` except the sender). Cab already paints `inbound` as you and skips HUN. Quiet sweep (`MailboxClient.sweep`) stays. Walk both mouths on a deployed origin: [sibling_phones.md](sibling_phones.md).

## Large

- [ ] **`:mailbox` JVM module.** Everything the 70 % bar covers is Android-free (`Wire`, `Text`, `MailboxUrl`, `MailboxConnect`, `Emoji`, `Slash`, `Photo`, `Jpeg`, `Look`, `GeoHint`, `Avatar`, `GoogleHint`, `Mouth`/`ChatLine`; OkHttp: `AuthApi`, `AvatarApi`, `MailboxClient`). `include(":mailbox")` with `org.jetbrains.kotlin.jvm` (match AGP's Kotlin, currently 2.4.20). `./gradlew :mailbox:test` then skips AGP — ~2 s cold for the code you touch most. `org.json`: `compileOnly` + `testImplementation`, platform class wins on device (or `kotlinx.serialization`). Point `scripts/jacoco-pct.sh` at `mailbox/build/reports/jacoco/test/jacocoTestReport.xml`; add `MailboxClient` to the gated list. `:app` stays thin (Activity, services, Compose, ViewModel, prefs).
- [ ] **Test the Android side.** `MailboxService`, `CabViewModel`, `CabPrefs`, `CabCarAppService` have no tests. `CabNotifier` + `ReplyService` are covered by `CabNotifierAutoContractTest` (Robolectric, SDK 34: the Android Auto notification contract and the spoken-reply → socket path — keep that one, it is the car). For the rest prefer extracting decisions into `:mailbox` (`retryDelay`, `shouldReconnect`, outbox flush order) over more Robolectric. Unlocks after the module split.
- [ ] **Replace hand-painted screenshots.** `DocsShot.kt` is 500 lines of Java2D that must track every Compose change. Use Compose Preview Screenshot Testing (`com.android.compose.screenshot`) or Roborazzi against `@Preview`s in `ShotScenes.kt`. `Type.kt` (bundled Noto Sans) is what LayoutLib needed. `make shot` becomes a Gradle `updateScreenshots` task, not `CAB_WRITE_SHOTS=1`. After the UI settles.

## Watch — not a ticket

- **`fragment-ktx`:** unused import-wise, but lint `InvalidFragmentVersionForActivityResult` requires Fragment ≥ 1.3.0 for `registerForActivityResult`. Leave it.
- **`EXTRA_SAMPLE`:** only when `BuildConfig.DEV`. Release ignores it. Keep the gate.
- **Google Sign-In:** prefers `GET /api/auth/nonce`, else `SecureRandom`
  (`mintNonce`). Web client id is public; Android client id is not in
  the APK. `aud` check is on the Worker — keep `PENDANT_ALLOWED_USERS`
  tight. Close `4401` / handshake 401 drops the stored JWE; 403 does not.
- **`MAILBOX_SECRET`:** a phone that has it *is* the operator. Lab-only. Rotate on device loss; prefer Google sessions off the emulator.
- **No certificate pinning.** Fine for a Cloudflare Worker + system trust store. Revisit only if the Worker moves behind a custom CA.
- **Notifications `VISIBILITY_PRIVATE`.** Keep.
- **Studio Sync/Make and CLI/pre-commit** writing the same `app/build` corrupts the Kotlin incremental cache (20 s full recompile). If it happens: `rm -rf app/build/kotlin` (no `clean`).
- **Keep the daemon:** never `--no-daemon` locally. Bigger heaps and `org.gradle.daemon.idletimeout=10800000` belong in `~/.gradle/gradle.properties` (`-Xmx6g`, Kotlin daemon `-Xmx4g`) — in-repo stays at `-Xmx2048m` so GitHub runners do not OOM.
- **Live Edit** + `@Preview`s in `ShotScenes.kt` before a full install.
- **Secrets in `.env` only.** `make ensure-sdk` may write `local.properties` with a mailbox origin; documented precedence already prefers `.env`.
- Real phones: sideload the **release** APK. Debug allows cleartext to loopback and is `isDebuggable`.

## Not this version

Pendant does not have these either (`gantry-pendant` `docs/todo.md`). Do not build them "to catch up":

- Failed + retry on an unacked send; copy on long-press
- Painted timestamps / day chips
- Crane presence (`asleep` / `queued`) separate from socket `live`
- Stop-a-turn, quote / reply-to, inline Yes / No, one non-image file
- Mute pings, photo lightbox, voice into compose, share target
- Reactions, read receipts, edit / delete, presence frames, delta streaming kinds

FCM lock-screen when the process is dead is later — same later as pendant Web Push.

## Release

1. Tag `v*` → GitHub attaches `gantry-cab-<version>.apk`. Sideload that.
2. Optional: `CAB_KEYSTORE_*` → one stable SHA-1. Without it, copy this release’s SHA-1 into GCP.
3. `CAB_MAILBOX_ORIGIN` is `https://…`; `CAB_GOOGLE_WEB_CLIENT_ID` is the pendant **Web** id.
4. `make test-scripts` passes (`cab-bake` proves no personal host in tree).
5. `PENDANT_ALLOWED_USERS` on the Worker still matches the family list.

```bash
make test-scripts
./gradlew lintDebug testDebugUnitTest createDebugUnitTestCoverageReport
make coverage-gate
```
