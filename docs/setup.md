# Setup

Sister of gantry-pendant. The Worker is the mailbox. This APK is another
phone. Android Auto is a `MessagingStyle` notification + a small
`CarAppService`.

## Build

```bash
make test            # no SDK: make test-scripts
make lint
make coverage        # JaCoCo + 70% bar
make install-hooks   # pre-commit: lint + test + coverage
make build           # debug APK → app/build/outputs/apk/debug/
make apk             # sideload APK → app/build/outputs/apk/release/
```

Needs JDK 21 (`JAVA_HOME`). Android Studio: open this folder, Sync,
Run (API 28+). Make writes `local.properties` from `~/Android/Sdk`
(or `ANDROID_HOME`) if that file is missing. Studio and Make both
read `VERSION` for `versionName` / `versionCode`.

This APK is a normal phone app (launcher + chat + `MessagingStyle`
heads-up). Auto is extra, not the only mouth. The pendant PWA in
Chrome is a different client.

Debug builds are **dev mode**: chips for `unsigned` / `empty` / `thread`
/ `ping` / `down` (same Ada/Kit copy as pendant). `make shot` records
those into `assets/docs`. Release ignores the `sample` extra.

The 70% bar is line coverage of the JVM mailbox wire (`Wire` +
`MailboxUrl`), same idea as gantree gating `lib/yard`. Android UI and
the OkHttp client are out of that number. `make install-hooks` copies
`scripts/pre-commit` into this checkout's `.git/hooks` (own remote,
not the parent gantree tree).

## Publish (GitHub Release)

This folder is its own git remote (like `repos/ai-gantry`), not a
path inside the gantree tree. `make release` refuses to tag a parent
repo. Same loop as ai-gantry: bump `VERSION`, commit, tag `v*` +
floating `latest`, push. The Release workflow builds the APK and
attaches `gantry-cab-<version>.apk`.

```bash
make release                 # patch
make release BUMP=minor
make release TAG=v0.2.0
make release DRY_RUN=1
```

Sideload that APK (or Play **internal** testing). Walk:
[sideload_to_android.md](sideload_to_android.md). Without a Play
keystore the APK is debug-signed. Optional repo secrets:
`CAB_KEYSTORE_BASE64`, `CAB_STORE_PASSWORD`, `CAB_KEY_ALIAS`,
`CAB_KEY_PASSWORD`. Bake the mailbox into the GitHub APK with
`CAB_MAILBOX_ORIGIN` and `CAB_GOOGLE_WEB_CLIENT_ID` (Actions secrets;
variables work too). Do not put a real Worker URL in git.

Coverage badge: Actions pushes `badges/coverage.svg` to `gh-pages`.
Repo Settings → Pages → branch `gh-pages` / root, once.

## Loopback (no Google)

1. Pendant: `npm run dev` (binds `127.0.0.1:3000`).
2. Android emulator: origin `http://10.0.2.2:3000`, slug `kit`, spike
   secret from pendant `.dev.vars` `MAILBOX_SECRET`.
3. Listen. Type. `/crane` tab on the laptop is the other socket.

A physical phone on LAN needs pendant bound to the LAN IP. The
default `vinext dev -H 127.0.0.1` will not see it.

## Google (production Worker)

Same GCP project as pendant's **Web application** client.

1. APIs & Services → Credentials → OAuth client ID → **Android**.
   Package `com.gantree.cab`. SHA-1 = debug keystore (Android Studio
   Gradle → signingReport) until you have a release key.
2. Bake the Worker host and Web client id at assemble time — not in
   source. Copy `.env.example` to `.env`:

   ```
   CAB_MAILBOX_ORIGIN=https://pendant.example.com
   CAB_GOOGLE_WEB_CLIENT_ID=<pendant Web client id>
   ```

   Same keys work as process env, or as `cab.mailboxOrigin` /
   `cab.googleWebClientId` in `local.properties`. Release APKs on
   GitHub read `CAB_MAILBOX_ORIGIN` and `CAB_GOOGLE_WEB_CLIENT_ID`
   from repo Actions secrets (or variables).

   `setServerClientId` is the Web client, so the ID token `aud` is
   what `POST /api/auth/token` already verifies. The Android client is
   only so Play Services will issue the token.

3. Cab: Google → Connect. Allowlist is still the crane's
   `PENDANT_ALLOWED_USERS` (Google `sub`), same as the PWA.

## Android Auto

- Desktop Head Unit from Android Studio, or a car.
- Real car: Android Auto app → tap Version 10× → Developer settings →
  **Unknown sources**. Google keeps tightening this; Play **internal
  testing** is the less-fragile path for a family list.
- Voice reply is Auto's STT. We never run `SpeechRecognition` in the dash.
