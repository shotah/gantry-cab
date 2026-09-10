# Sideload Cab onto a phone (and Android Auto)

Cab is one APK: chat on the phone, and the same app in Android Auto.
Needs Android 9 or newer. Not on the Play Store, so the phone has to
allow installing an app from a file.

You will also need mailbox details from whoever runs
[gantry-pendant](https://github.com/shotah/gantry-pendant): the Worker
URL, the agent name, and the access secret. Cab does not work by
itself.

## 1. Get the APK

On the phone, open
[github.com/shotah/gantry-cab/releases](https://github.com/shotah/gantry-cab/releases)
in a browser. Latest release → Assets → `gantry-cab-<version>.apk`.
That is the intended install: a real phone, no emulator, no Play Store.

If there is no APK on that page, there is nothing to sideload yet.
Ask the person who maintains this repo, or build from source
([setup.md](setup.md)).

## 2. Install it

Android will warn that the file is not from Play. That is expected.

1. Open the downloaded `.apk`.
2. If install is blocked: Settings → Apps → the app that downloaded
   it (Chrome, Files, Files by Google, …) → **Install unknown apps**
   → allow. Then open the download again.
3. Install. Open **Cab**.
4. Allow **notifications** (Android Auto reads Kit from those).
   Location is optional (GPS on send).

The unknown-apps switch is on the **downloader**, not on Cab.

### Optional: install from a computer

USB debugging: Settings → About phone → tap **Build number** 7× →
Developer options → **USB debugging**. Plug in and trust the computer.

```bash
adb devices
adb install -r path/to/gantry-cab-<version>.apk
```

If the phone already has Cab signed with a different key:

```bash
adb uninstall com.gantree.cab
adb install path/to/gantry-cab-<version>.apk
```

## 3. Connect to the mailbox

Open Cab and fill:

| Field | What to put |
| --- | --- |
| Mailbox origin | The pendant Worker URL, `https://…`. This APK will not talk `http://`. |
| Agent name | The room, often `kit`. |
| Agent access secret | The secret that Worker accepts for this phone. |

Tap **Listen**. Stay on the screen until the socket comes up. Type
on the phone; Kit's replies show in the thread and as a heads-up
notification.

The GitHub APK only has the Google **button** if the release job was
given `CAB_GOOGLE_WEB_CLIENT_ID` (pendant’s **Web** client id — copying
that is correct). Play Services still needs a **separate Android**
OAuth client in the same GCP project: package `com.gantree.cab`, SHA-1
of this APK, **no** redirect URI. Copy the SHA-1 from that
release’s notes (or the `-sha1.txt` asset) — not the SHA-256 next
to the APK. Cab posts the token to the mailbox
`/api/auth/token`; Google is not told a Cab callback. Details:
[setup.md](setup.md#google-production-worker). Otherwise the phone
secret is the way in.

A leftover origin of `http://10.0.2.2:3000` is the emulator default.
On a real phone that address is not the mailbox — replace it with the
https Worker URL.

## 4. Android Auto

Same install. After **Listen** is connected:

1. Install **Android Auto** from Play if the phone does not already
   have it (some Pixels include it).
2. Open Android Auto (or Settings → Connected devices → Android Auto).
   Scroll to **Version** and tap it **10 times** until developer mode
   unlocks.
3. Menu → **Developer settings** → **Unknown sources** → on. Google
   moves this toggle; if it is missing, Cab will work on the phone
   but not in the car until that setting exists again.
4. Plug the phone into the car.
5. Cab should appear in the Auto app list. Open it and tap **Reply**
   on the Kit conversation to speak first. Kit is also read as a
   message notification while you are in maps or music. Spoken text
   uses Auto's own voice typing (not Google Assistant); Cab sends
   that inbound.

If Cab never shows in the car: unknown sources is off, notifications
were denied, or Listen never connected. The phone app can still
work.

## If it will not connect

| What happened | What to try |
| --- | --- |
| Install blocked | Allow unknown apps on Chrome / Files, then reopen the `.apk`. |
| Continue with Google opens then “cancelled” | Need a **new Android** OAuth client (package `com.gantree.cab` + this APK’s cert SHA-1 from the GitHub Release notes). GitHub’s SHA-256 next to the APK is the file hash, not that fingerprint. See [setup.md](setup.md#google-production-worker). |
| Continue with Google works, but Offline and Message is dead | Google is identity. Live is the mailbox socket. Sideload a build that reconnects after sign-in (0.1.5 could start the service before the session was saved). If the hint says HTTP 403, you’re not on that crane’s room list. |
| Live, send works, Kit never answers | The phone is in the Worker room. The **crane** still has to be running, and your Google email/`sub` must be on that crane’s `PENDANT_ALLOWED_USERS` (recreate the crane after changing it). GPS omitted is not a failed send. |
| Message hangs / “GPS omitted” after send | Old APK waited up to 4s for a fresh GPS fix before painting the bubble. Sideload a build that uses last-known location on send. |
| `http://` origin | This release APK blocks plain HTTP. Use the https Worker URL. |
| Cab missing in Android Auto | Unknown sources off, or notifications off. |
| Update over an older Cab fails | Uninstall Cab, then install the new APK. |
