# Setup

Sister of gantry-pendant. The Worker is the mailbox. This APK is another
phone. Android Auto is a `MessagingStyle` notification + a small
`CarAppService`.

## Build

```bash
make test            # no SDK: make test-scripts
make lint
make coverage        # JaCoCo + 70% bar
make check-app       # lint + test + coverage (one Gradle invocation)
make install-hooks   # pre-commit: tests; pre-push: lint + coverage
make watch           # continuous mailbox JVM tests
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
`scripts/pre-commit` (tests) and `scripts/pre-push` (lint + coverage)
into this checkout's `.git/hooks` (own remote, not the parent gantree
tree).

## Publish (GitHub Release)

This folder is its own git remote (like `repos/ai-gantry`), not a
path inside the gantree tree. `make release` refuses to tag a parent
repo. Same loop as ai-gantry: bump `VERSION`, commit, tag `v*` +
floating `latest`, push. The Release workflow builds the APK, attaches `gantry-cab-<version>.apk`,
and puts the **signing-cert SHA-1** in the release notes (plus a
`gantry-cab-<version>-sha1.txt` asset). That fingerprint is what
Google’s Android OAuth client wants — not the SHA-256 checksum next
to the APK.

```bash
make release                 # patch
make release BUMP=minor
make release TAG=v0.2.0
make release DRY_RUN=1
```

Sideload that APK onto a phone. Walk:
[sideload_to_android.md](sideload_to_android.md). That is the install
path — no emulator, no Play Store. The workflow always produces an
APK. Without `CAB_KEYSTORE_*` it is debug-signed (SHA-1 changes each
tag; uninstall to update if the key changed). Optional repo secrets
`CAB_KEYSTORE_BASE64`, `CAB_STORE_PASSWORD`, `CAB_KEY_ALIAS`,
`CAB_KEY_PASSWORD` keep one fingerprint. Bake the mailbox into the
GitHub APK with `CAB_MAILBOX_ORIGIN` and `CAB_GOOGLE_WEB_CLIENT_ID`
(Actions secrets; variables work too). Do not put a real Worker URL
in git.

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

Two OAuth clients in the **same** GCP project as pendant. Copying the
pendant **Web** client id into Cab is correct. You still need a **new
Android** client. Cab does not use a Google redirect / callback URI.

| Client | Type | Where it goes | Redirect / callback |
| --- | --- | --- | --- |
| Pendant’s existing Web application | Web | `CAB_GOOGLE_WEB_CLIENT_ID` (`.env`, or the GitHub secret). This is the ID token `aud`. | Pendant’s own Web callback — leave it. Do **not** add a Cab URL here. |
| **New** Android client | Android | GCP only. Never baked into the APK. | **None.** Android clients have no authorized redirect URIs. |

What actually happens: Play Services mints an ID token on the phone,
then Cab `POST`s `{ id_token, nonce }` to
`https://<mailbox>/api/auth/token`. That Worker URL is the only
“callback.” Allowlist is still `PENDANT_ALLOWED_USERS`.

1. APIs & Services → Credentials → **Create credentials** → OAuth
   client ID → **Android** (this is the new one, not a copy of Web).
   Application ID / package: `com.gantree.cab`.
   SHA-1: copy the **Signing certificate SHA-1** block from the
   [GitHub Release](https://github.com/shotah/gantry-cab/releases)
   you installed (or open the `-sha1.txt` asset). That is the
   signing certificate, not the SHA-256 checksum next to the APK
   (that hash is the file).

   The SHA-1 is the **keystore**, not the package name. Same key →
   same fingerprint forever. It does **not** change when you bump
   the version.

   GitHub APKs without `CAB_KEYSTORE_*` are signed with a fresh CI
   debug key every job, so that fingerprint **does** change every
   release — paste the new value, or set those secrets so Google
   only needs one SHA-1.

   Local sideload (`make apk` on this machine), not the GitHub APK:

   ```bash
   make signing-report
   ```

   Look for `SHA1:` under `Variant: debug` / `Config: debug`. That
   laptop debug key is a different cert than CI unless you share a
   keystore. If you later sign with `CAB_KEYSTORE_*`, add that
   keystore’s SHA-1 as well (a second Android client, same package).
2. Bake the Worker host and the **Web** client id — not the Android
   client id — at assemble time. Copy `.env.example` to `.env`:

   ```
   CAB_MAILBOX_ORIGIN=https://pendant.example.com
   CAB_GOOGLE_WEB_CLIENT_ID=<pendant Web client id>
   ```

   Same keys work as process env, or as `cab.mailboxOrigin` /
   `cab.googleWebClientId` in `local.properties`. Release APKs on
   GitHub read `CAB_MAILBOX_ORIGIN` and `CAB_GOOGLE_WEB_CLIENT_ID`
   from repo Actions secrets (or variables).

3. Rebuild / sideload, then Continue with Google. If the account
   picker opens and then says cancelled, the Android client’s SHA-1
   does not match this APK. The Web id in `.env` is fine. If Google
   succeeds but the bar stays **Offline**, that is the mailbox
   socket, not OAuth — you need a build that reconnects after
   sign-in, and you must be on that crane’s room list.

## Android Auto

- Desktop Head Unit from Android Studio shows the `CabCarAppService`
  conversation screen. A real car does **not**: Google's unknown-sources
  toggle covers notifications, media and parked apps, not Car App
  Library apps, and templated messaging is Play internal-testing only.
- Real car: Android Auto app → tap Version 10× → Developer settings →
  **Unknown sources**. That admits the `MessagingStyle` card — Kit read
  aloud, spoken reply — and that is the whole in-car product for a
  sideloaded GitHub APK. Settings → **Test car voice** proves the path.
- Voice reply is Auto's STT. We never run `SpeechRecognition` in the dash.
