# Security — vulnerabilities and things to watch

Threat model in one line: a phone that holds a **credential for the crane's
room** (Google session JWE or the shared `MAILBOX_SECRET`) and a persistent
socket that **speaks for the operator** to an agent. Losing the credential,
leaking it to the wrong host, or letting another app read the thread are the
failures that matter. The Worker (gantry-pendant) does the authorisation;
this doc is only the Android side and the release pipeline.

Severity is impact × likelihood as sideloaded, single-family software.
Items marked **fix** are also in [todo_fixes.md](todo_fixes.md).

## Where the line is

The product is a **GitHub Release APK sideloaded onto a real phone**.
No emulator, no Play Store. A `v*` tag must always attach an
installable APK. Never fail the release job for a missing Play
keystore, R8, pinning, or Tink.

| Track | What it is | Rules |
| --- | --- | --- |
| **Development** | `make build` debug APK. Emulator, DHU, sample extras, `http://10.0.2.2`. `BuildConfig.DEV`. | Cleartext only to loopback. Spike may persist on loopback. Car host `ALLOW_ALL` (DHU). Not how the family phone is installed. |
| **Sideload (the product)** | GitHub Release `assembleRelease`. `https://` Worker. Real Android Auto. | Session expiry, payload caps, slug checks, car host allowlist, no cleartext, GPS off until toggled. Debug-signed is fine. Optional `CAB_KEYSTORE_*` only to keep one Google SHA-1. |
| **Play Store** | Not a goal. | Do not block sideload to chase a store listing. |

Phone-side hardening in this doc applies to the **sideload** APK.
Lab-only posture stays behind `BuildConfig.DEV`.

## High

- [x] **Bearer sent to whatever host is half-typed.** (**fix**)
  Avatar fetch uses `_fetchOrigin` (updated only in `persist()`), skips a
  blank bearer / bad slug, and `AvatarApi.fetch` is cancellable
  (`Call.cancel()` when `collectLatest` moves on). Still do not type a
  Worker URL and hit Connect until it is the real host.

- [x] **Any app on the phone can read the thread through the car service.**
  `createHostValidator` uses `ALLOW_ALL_HOSTS_VALIDATOR` only when
  `BuildConfig.DEV` (Desktop Head Unit). Release uses
  `hosts_allowlist_sample`.

- [ ] **Credentials at rest in plain `SharedPreferences`.**
  `CabPrefs` writes the session JWE, `MAILBOX_SECRET` and email to
  `shared_prefs/cab.xml`. `android:allowBackup="false"` is set (good), but
  root, a debug build (`run-as com.gantree.cab` reads the file — debug is
  `isDebuggable = true`), or a forensic dump exposes both credentials.
  Mitigation: Keystore-wrapped AEAD (Tink `AndroidKeysetManager`, which is
  what `EncryptedSharedPreferences` used underneath — `androidx.security:security-crypto`
  itself is deprecated, so go to Tink directly). **Partial now:** Google
  sign-in clears the spike; a debug APK persists the spike only for
  `10.0.2.2` / `localhost` / `127.0.0.1` (in-memory otherwise). Tink is
  still the real fix.

## Medium

- [x] **Release APKs are debug-signed unless `CAB_KEYSTORE_*` is set.**
  That is allowed. GitHub Release always builds; the notes say whether
  the SHA-1 is stable or “changes every GitHub Release”. A keystore is
  optional convenience for Google Sign-In, not a gate. Play Store is
  not a goal.

- [x] **Session lifetime is not enforced on the phone.** (**fix**)
  `CabPrefs.sessionExp` stores JWT `exp` (unix seconds). `liveBearer`
  refuses an expired session (no spike fallback). Connect stops with a
  re-auth hint. Worker 401/403/404 still stop the retry loop.

- [x] **Unbounded inbound payloads from the mailbox.** (**fix**)
  `parseFrame` caps `text` at `TEXT_CHARS_MAX` (8000) and inbound images
  at `IMAGE_BYTES_MAX`. `commands` was already capped.

- [x] **Server-controlled identifiers used unvalidated.** (**fix**)
  `AuthApi.me` filters `cranes` through `parseSlug`; persist/upload drop
  the `?: slug` fallbacks.

- [x] **Debug build is a weaker security posture on a real phone.**
  Debug `network_security_config.xml` allows cleartext only to `10.0.2.2`,
  `localhost`, and `127.0.0.1`. Release still blocks cleartext. Keep using
  a release APK on real phones.

- [ ] **CI/CD least privilege and supply chain.**
  - [x] `ci.yml` default `contents: read`; badge push is its own job with
    `contents: write` on `main`.
  - [x] `release.yml` checkout `persist-credentials: false`; token only on
    the `latest` tag push.
  - [x] Dependabot weekly for `gradle` and `github-actions`.
  - [ ] Third-party actions pinned by tag (`android-actions/setup-android@v3`,
    `gradle/actions/setup-gradle@v4`, `actions/*@v4`). Pin to commit SHAs
    and let Dependabot bump them.
  - [ ] No `gradle/verification-metadata.xml`; every dependency is trusted by
    coordinates only. Generate with `./gradlew --write-verification-metadata sha256`
    and commit. `setup-gradle@v4` already validates the wrapper jar (keep).
  - [ ] `latest` is a force-pushed tag and `make release` moves it from a
    laptop. Add a tag protection rule for `v*` and `latest` so only the
    release workflow / maintainers can move them.
  - [ ] Release notes tell users to paste a SHA-1 from GitHub into GCP —
    that is the intended trust path, so protect the repo/branch that
    produces it (require PR review on `main`, signed tags).

- [x] **Reconnect storm is a self-DoS.** (**fix**) Backoff 2 s → 60 s;
  stop on 401/403/404.

## Low / watch

- [x] `PendingIntent.FLAG_MUTABLE` on the reply/mark-read actions is
  required for `RemoteInput`. `ReplyService` is `exported="false"` (good).
  `EXTRA_SLUG` is gone so nothing can start trusting it.
- [ ] `MainActivity` honours an `EXTRA_SAMPLE` extra only when
  `BuildConfig.DEV`. Keep the gate; release ignores it (verified in code).
- [ ] Google Sign-In: nonce is `SecureRandom` and sent to the Worker for
  verification; the Web client id is baked in and is public by design; the
  Android client id is never in the APK. Correct. The `aud` check lives on
  the Worker — keep `PENDANT_ALLOWED_USERS` tight.
- [ ] `MAILBOX_SECRET` is a static room password: a phone that has it *is*
  the operator. Settings copy already calls it lab-only. Rotate on device
  loss; prefer Google sessions off the emulator.
- [ ] No certificate pinning. Acceptable for a Cloudflare-fronted Worker
  with the system trust store; release blocks cleartext (good). Revisit only
  if the Worker moves behind a custom CA.
- [x] Location: GPS toggle **defaults off** (`CabPrefs.gps`). Existing
  installs that already stored `"on"` stay on. Fine location is still
  used when the user turns GPS on (including "Drop a pin").
- [ ] R8 is off in release (`isMinifyEnabled = false`): full symbol names
  and all unused code ship in a 51 MB APK. Enable minify + shrink; keep
  `proguard-rules.pro` for OkHttp/Compose if anything breaks.
- [ ] Notifications are `VISIBILITY_PRIVATE` (content hidden on a locked
  lock screen). Keep.
- [x] No `android.util.Log` calls in `app/src/main`. `cab-bake.test.sh`
  greps for that and for a real `.apps.googleusercontent.com` client id.
- [ ] Dependencies are behind (credentials 1.3.0 → 1.6.0, lifecycle 2.8.7 →
  2.11.0, googleid 1.1.1 → 1.2.0, play-services-location 21.3.0 → 21.4.0,
  Gradle 9.6.0 → 9.7.1 per the last lint run). Security fixes in
  `credentials` / `play-services` land in those bumps. Dependabot weekly.

## Quick checklist for a release

1. Tag `v*` → GitHub attaches `gantry-cab-<version>.apk`. Sideload that.
2. Optional: `CAB_KEYSTORE_*` → one stable SHA-1. Without it, copy this
   release’s SHA-1 into GCP (or uninstall if the key changed).
3. `CAB_MAILBOX_ORIGIN` is `https://…`; `CAB_GOOGLE_WEB_CLIENT_ID` is the
   pendant **Web** id.
4. `make test-scripts` passes (`cab-bake` proves no personal host in tree).
5. `PENDANT_ALLOWED_USERS` on the Worker still matches the family list.
