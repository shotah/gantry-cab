# FCM lock-screen (public service — not this beta)

How a **huge public Cab** would hear Kit when the process is dead,
the way WhatsApp does, without anyone opening the app first.

This is **not** demo, POC, or family-beta work. Do not build it to
catch up, to hide a foreground service, or to make the grocery run
work if you forgot to open Cab. Sideload Auto is: **open Cab, send a
line, lock the phone, drive.** Reboot? Open Cab again. The shade
shows Kit's message card, not a second "listening" bubble.

Pendant Web Push (`PUT /api/push`, VAPID) is the PWA cousin and is
also still optional. Cab does **not** `PUT /api/push`.

Pick this ticket only if Cab is a public service (Play-scale, people
who never open the app except when the shade pings). Until then the
boxes stay closed.

Web Push: pendant [`lib/push/`](../../gantry-pendant/lib/push/),
[`docs/architecture.md`](../../gantry-pendant/docs/architecture.md).
Auto cards: [android_auto_setup.md](android_auto_setup.md).

---

## 0. What this is

Android Auto never talks to WhatsApp's socket. It reads a
`MessagingStyle` card that already landed in the shade. Cab already
posts that card (`CabNotifier.kitMessage`). WhatsApp can be fully
dead because **Play Services** wakes it with a high-priority FCM
data message; WhatsApp then posts the bubble.

Family Cab does not do that. The mailbox WebSocket runs after you
open the app (Connect / send / spoken reply). Android still requires
a silent foreground-service notice for that socket — a **Mailbox /
Connected** status, never a second Kit conversation. It does not
come back on boot. FCM is the wake that would let the process stay
dead until Kit actually speaks.

```text
beta:    you open Cab ──wss──► MailboxService (this session) ──► kitMessage → Auto
public:  Worker ──FCM──► Play Services ──► Cab (seconds) ──► kitMessage → Auto
         Worker ──wss──► MailboxService only while the mouth is actually open
```

---

## 1. Success

- Kit `reply` / `push` still becomes the Tim / Kit **message card**.
  Auto still reads it and still offers Reply. `RemoteInput` still
  becomes `inbound` with `surface: android_auto` when a head unit is
  attached.
- After Google sign-in, you can **Force stop** Cab (or reboot and not
  open it) and still get that card. No need to open Cab first.
- A spoken / shade reply may still start `MailboxService` for a few
  seconds (`ReplyService` already does). That is the same rule as a
  WhatsApp call bar: FGS only while the work needs a process.
- Opening Cab still dials `wss`. The live thread, drafts, and
  hydrate do not move onto FCM.
- Spike (`MAILBOX_SECRET`) never registers. Sibling live-delivery is
  per Google `sub`; FCM is too.
- An old APK with no FCM stays valid: it just needs you to open Cab
  before the drive.

---

## 2. Already true (do not rebuild)

| Piece | Keep |
| --- | --- |
| `CabNotifier.kitMessage` | The Auto contract (`MessagingStyle`, reply, mark-as-read, `MESSAGE_ID` 42, `VISIBILITY_PRIVATE`). Covered by `CabNotifierAutoContractTest`. |
| `shouldSpeak` / `shouldPost` | `reply` / `push` only; skip `inbound`, skip `replay`, skip while the phone thread is resumed, skip while the Cab Auto **conversation** screen is the mouth. |
| `notifyBody` | Empty text → `"ping"`; photo → `"Photo"`. Pendant Web Push says `"New message"` — do not switch Cab to that. |
| `ReplyService` → `MailboxService.sendText` | Already starts the FGS if the socket process is dead, queues inbound, flushes on connect. |
| `ThreadCache` | Paint before hydrate. First `ack` `since` is the highest cached seq. |
| Hydrate `replay: true` | History must not chime. A wake-only ping **without** a body would hydrate the new reply as `replay` and Auto would stay silent — that is why FCM must carry enough to post the card itself. |
| PWA `PUT /api/push` | VAPID Web Push, host allowlist, `p:` rows on the DO. Cab never uses it. Chrome-on-Android Web Push hitting `fcm.googleapis.com` is **not** native FCM. |
| `notifyOffline` kinds | Same gate as Web Push: `kind === "push" \|\| kind === "reply"`. Worker already fans even when a socket looks live (frozen Android). Client dedups. |
| Cookie CSRF | PWA-only. OkHttp stays `CookieJar.NO_COOKIES` + `Authorization: Bearer`. |
| Sideload | Play Store is not a goal. FCM does **not** need a Play listing. It needs Play **Services** on the phone (family phones have it). |
| `CAB_KEYSTORE_*` | One stable SHA-1 for Google Sign-In. FCM fingerprints are the same SHA-1. Rotating debug keys per GitHub Release will require a new Firebase fingerprint the same way Sign-In already does. |

---

## 3. Non-goals

- Play Store listing, a Cab tile in a real car, or Car App Library
  unknown-sources. Unchanged: [android_auto_setup.md](android_auto_setup.md) §0 / §7.
- Native APNs / iOS. Cloudflare will not send APNs for us. iOS native
  is a later mouth; Web Push is its lock-screen if it stays a PWA.
- Replacing the mailbox WebSocket with HTTP `getUpdates`.
- Putting JPEGs, session JWEs, or GPS on FCM (4 KB data-message cap;
  those bytes are the thread).
- FCM on the emulator spike (`MAILBOX_SECRET` / `10.0.2.2`). Lab
  socket stays Listen.
- Huawei / no-GMS devices.
- Letting a listed human point the Worker at an arbitrary URL. Native
  tokens are opaque; they are not Web Push endpoints.

---

## 4. Design

### 4.1 Two paths, one card

| Path | When | Who posts the card |
| --- | --- | --- |
| Socket | Cab Activity visible, or a reply is in flight, or (phase 2) a head unit is attached **and** the process is already up | `MailboxService` `onFrame` → `shouldPost` → `kitMessage` |
| FCM | Process dead, Doze, Force stop, idle after we drop always-on Listen | `FirebaseMessagingService` → same `kitMessage` |

The Worker **always** fans FCM on `reply` / `push` for that human's
stored tokens, the same way it always fans Web Push (frozen sockets
look live). Cab **dedups**. Do not add "skip FCM if a phone socket is
open" — that is how a Doze Cab goes silent.

Dedup key is the frame `id` (already how `Mouth.ingest` folds
hydrate). Keep a small recently-notified id set in `CabNotifier` /
`NotifyGate`. If that `id` already chimed this process, skip. A
process that was dead has an empty set, so the FCM card is the first
and only one.

When the socket later delivers the same `id` (hydrate `replay`, or
live if the FGS came back), `Mouth.ingest` drops it as a known id.
`shouldSpeak` also skips `replay`. Belt and braces.

### 4.2 FCM payload (data-only, high priority)

Hard rule: **data message**, never a `notification` payload.
Play Services displays notification-messages itself when Cab is in
the background and does **not** run our code until the user taps.
That card has no `MessagingStyle`, no Reply, no Auto contract.

```text
POST https://fcm.googleapis.com/v1/projects/<project>/messages:send

message.token              = the device token
message.android.priority    = HIGH
message.android.ttl        = 3600s   (match mailbox QUEUE_TTL, 1 h)
message.data.kind           = "reply" | "push"
message.data.id             = wire id
message.data.slug          = room slug
message.data.body           = notifyBody(text, photo)  — Cab's ping/Photo
message.data.photo          = "1" if images[0], else omit
```

All `data` values are strings. Cap `body` so the whole message stays
under 4 KB (140 chars matches pendant `notifyBody`; Auto already
speaks a preview). Full text lives on the transcript; opening Cab
hydrates it.

Do not send `inbound`. Do not send `draft` / `error` / `face`. A
push with no text and no photo is `body = "ping"`.

### 4.3 After the card: do not start Listen

`FirebaseMessagingService.onMessageReceived`:

1. Parse `kind` / `id` / `slug` / `body`. Junk → return.
2. If `kind` is not `reply` / `push`, or `id` already notified, return.
3. `CabNotifier.kitMessage(ctx, slug, body, app.face)` — last cached
   face, no avatar GET on this thread.
4. Fold a stub line into `Mouth` / `ThreadCache` so opening Cab is
   not missing the turn (`id` + `kind` + text, no `seq`; hydrate
   fills `seq`).
5. **Do not** `MailboxService.start`. The session socket stays off
   until the user opens Cab or replies.

Spoken / shade Reply already does `MailboxService.sendBits` →
`startForegroundService` if the instance is null. Connect, flush
outbox, hydrate. Then (phase 2) stop the service when the socket is
idle and no head unit is attached.

`CarConnection` only reports while the process lives. First Kit
message of a drive can wake via FCM; if we then see a head unit, we
**may** keep the FGS for the rest of the drive so follow-up replies
skip the FCM hop. That is an optimization, not required for Auto to
read the first card.

### 4.4 Mailbox: new route, new store

Cab does not invent a URL the Worker does not have. Pendant adds a
native-token door next to Web Push. Suggested shape (pendant names
it; Cab matches):

```text
PUT    /api/fcm     { "slug", "token" }     Bearer JWE
DELETE /api/fcm     { "slug", "token" }     Bearer JWE
```

- Handshake is the same as `/api/push`: listed Google `sub`, room
  allow, `limitAuthRequest`, body cap (~8 KB is plenty). 401 drops
  nothing new; Cab already drops the JWE on handshake 401 / 4401.
- 404 when FCM Worker secrets are missing (same as no VAPID → 404
  on `/api/push`). Cab treats 404 as "no lock-screen" and stays on
  Listen. Do not toast.
- Spike / missing `sub` → 401. Do not store a token under `_`.
- Token is an opaque FCM registration string, max ~4 KB, charset
  conservative (`[A-Za-z0-9_:-]+` or "non-empty, no spaces, ≤4096").
  It is **not** an `https://` endpoint. Do not run it through
  `parseHttpsEndpoint` / `pushHostAllowed`.
- DO prefix **`f:`** (Web Push stays `p:`). Cap **8 tokens per
  `sub`**, drop oldest, same as `PUSH_PER_USER`. Yank from the room
  list prunes `f:` the way `prunePushForRoom` prunes `p:`.
- Fan from `notifyOffline` (or a sibling `notifyFcm`) **after**
  live sockets, best-effort, same frame. `UNREGISTERED` /
  `NOT_FOUND` drops that row (Web Push 404/410). Other failures
  stay (pendant audit already wants a fail counter — share it).
- Worker OAuth: FCM HTTP v1 needs a Google **service account**
  access token (`https://www.googleapis.com/auth/firebase.messaging`).
  Store the JSON as a Worker secret. Sign a JWT on the Worker; cache
  the access token (~1 h). Do not put that JSON in the APK.
- Non-blocking fan is already an open pendant audit box
  (`notifyOffline` awaits every push). Do not make FCM worse; fire
  and forget if that ticket is still open.

Do **not** overload `PUT /api/push` with a native token. The
allowlist, VAPID, and Chrome endpoint shape are a different threat
model.

### 4.5 Cab: register, refresh, sign-out

- Firebase BOM + `firebase-messaging`. Initialize with
  `FirebaseOptions` from `BuildConfig` (same pattern as
  `CAB_GOOGLE_WEB_CLIENT_ID`), **not** a required
  `google-services.json` / Gradle plugin. Missing IDs → no
  registration; APK still builds. Never fail `make apk` / the
  Release job for empty FCM fields.
- After Google session is stored, fetch the token and `PUT /api/fcm`.
  `onNewToken` does the same PUT.
- `CabPrefs.signOut` / 4401: `DELETE /api/fcm` best-effort, then
  `FirebaseMessaging.deleteToken()`. Do not leave a token on the DO
  for a yanked session.
- Token + last registered `sub`/slug in prefs so a slug change
  re-PUTs. Wrap with the rest of credentials-at-rest when that
  ticket happens (Tink); until then it is no worse than the JWE in
  `cab.xml`.

Proposed env (client, public):

```text
CAB_FIREBASE_PROJECT_ID=
CAB_FIREBASE_APPLICATION_ID=   # 1:…:android:…
CAB_FIREBASE_API_KEY=           # browser/Android key, not the SA
CAB_FIREBASE_SENDER_ID=
```

Worker (secret):

```text
FCM_SERVICE_ACCOUNT=            # JSON: project_id, client_email, private_key
```

Same GCP project as Google Sign-In is the least new surface: enable
Firebase + FCM API, add Android app `com.gantree.cab`, paste the
**same SHA-1** already in the GitHub Release notes.

### 4.6 What the beta already dropped

The always-on **"Listening for Kit"** Kit-channel card is gone. There
is no `BootReceiver`. Auto setup is "open Cab, send a line, drive."
FCM does not need a dual-run with that chip.

When (if) this ships for a public service, FCM posts the Kit card
from a dead process. `MailboxService` stays a session socket (open
app / Connect / send / spoken reply). Do not bring back boot
relisten to "make FCM easier."

OEM battery killers can still block FCM. "Unrestricted battery"
stays in the Auto setup. That is also why WhatsApp sometimes goes
silent on Xiaomi.

---

## 5. Security

Threat model is unchanged: a phone that holds a **credential for the
crane's room** and a path that **speaks for the operator**. FCM adds
a **device token that can make Cab's shade speak**.

- Tokens live on the crane's Durable Object, keyed by Google `sub`.
  Yank drops them. Do not log tokens. Do not persist them in Worker
  KV.
- A stolen FCM token lets someone **send** a high-priority ping to
  that phone (preview text). It does not let them send `inbound` as
  Ada. Speaking still needs the JWE on the socket.
- The service account can push to **any** token in this Firebase
  project. Keep the project to Cab. Do not reuse it for unrelated
  apps.
- Preview `body` is Kit's words. Same `VISIBILITY_PRIVATE` as today
  once we post. The FCM hop itself is Google; do not put secrets in
  `data`.
- Rate: reuse per-`sub` auth limits on PUT. Fan volume is Kit's
  mouth (family chat). High-priority FCM that nobody opens gets
  deprioritized by Google — do not "fix" that with a tighter loop.
- CSRF / cookie: none. Bearer only.

Stolen phone: OS lock, Google sign-out other sessions, yank `sub`
(4401), rotate crane bearer — same list as pendant
`docs/security.md`. DELETE `/api/fcm` is step 2.5.

---

## 6. Tests

Pendant (Vitest, mirror `test/push/`):

- Parse PUT/DELETE: good token, empty, URL-shaped, too long, missing
  slug, spike principal rejected.
- Fan: `reply` / `push` only; `user_id` targets that `sub`; no
  `user_id` (cron `push`) hits every stored token; `inbound` skipped.
- `UNREGISTERED` → gone; 5xx stays.
- Payload builder: `ping` / `Photo` / truncated body; no
  `notification` key.

Cab (JVM, no Robolectric Firebase):

- `parseFcmData` / `shouldPost` / notified-id dedup. Photo flag,
  missing id, wrong kind.
- `AuthApi` PUT/DELETE against MockWebServer (404 = no-op, 401 =
  existing auth-lost path, 200 stores nothing locally except "we
  registered").
- Do **not** weaken `CabNotifierAutoContractTest`. The card from FCM
  must be the same `kitMessage` builder.

Walk is the gate, not coverage.

---

## 7. Todo

Ordered **pendant mailbox first** (Cab cannot subscribe a route that
404s), then Cab, then a public-scale walk. None of this is the
sideload beta.

### Pendant (`gantry-pendant`)

- [ ] **Worker secrets.** Firebase service account JSON.
      `readFcm(env)` null → `/api/fcm` 404, `notifyOffline` skips FCM
      the way missing VAPID skips Web Push.
- [ ] **`lib/push/fcm.ts` (or `lib/fcm/`).** Token parse, store key
      `f:<sub>:<hash>`, upsert/drop/prune, HTTP v1 send, gone
      handling. Tests next to `test/push/`.
- [ ] **`PUT|DELETE /api/fcm`.** Same handshake as `/api/push`. Bearer
      JWE. No cookie. Body cap. CSRF does not apply.
- [ ] **`notifyOffline` also fans native tokens.** Same kinds, same
      `webPushUserIds` targeting. Do not wait on the reply handler if
      the non-blocking audit is still open.
- [ ] **frontends.md / security.md / architecture.md.** Additive
      `/api/fcm`. Cab still must not `PUT /api/push`. Native APNs
      stays later.
- [ ] **Gantree Settings** does not need to mint this (VAPID is
      already leftover). Wrangler secret is enough.

### Cab (`gantry-cab`)

- [ ] **Firebase client IDs in `.env` / `BuildConfig`.** Empty is
      legal. `.env.example` comments. Never commit a service account.
- [ ] **`firebase-messaging`.** `FirebaseMessagingService` in the
      manifest. `POST_NOTIFICATIONS` already there. No new FGS type.
- [ ] **Register / refresh / sign-out** as §4.5.
- [ ] **`onMessageReceived` → `kitMessage` + cache stub + id
      dedup.** No `MailboxService.start`.
- [ ] **Settings copy / Test car voice** stay. Auto copy is already
      "open Cab before you drive," not "must already be Live."
- [ ] **ProGuard** keep for Firebase if minify ever turns on (that
      ticket is separate and not a release-job gate).

### After it works (public-scale only)

- [ ] Optional: stop the session FGS when idle (no Activity, no
      `carAttached`, empty outbox) and rely on FCM for the next Kit
      line. Not required to ship FCM.
- [ ] Optional: if FCM wake sees `carAttached`, stay connected until
      disconnect.

### Walk (public service — not the family sideload)

Google on Cab, FCM secrets on the origin, Play Services, battery
unrestricted:

1. Sign in. Settings / log proves `PUT /api/fcm` 200.
2. **Force stop** Cab. Type in the crane (or PWA). Kit `reply` →
   Kit/Tim **message card** on the lock screen. Auto reads it.
3. Reply from the card without opening Cab. `inbound` reaches the
   crane.
4. Cab open on screen: Kit reply paints in the thread, **no** extra
   HUN. FCM may still arrive; id dedup means Auto does not re-read.
5. Bob in the same room does not get Ada's FCM. Cron `push` with no
   `user_id` hits Ada and Bob if both registered.
6. Sign out: further Kit replies do not wake this phone.
7. Spike Cab never registers; Force stop + spike stays silent.
8. Missing Worker secrets: Cab 404s, no crash, open-Cab-then-drive
   still works.

---

## 8. Watch

- **Duplicate Auto reads** = lost id, or `kitMessage` appending a
  second `KitTurn` for the same text. Cap history is 6; a dup still
  speaks. Dedup before `pushKitTurn`.
- **Hydrate + FCM.** Wake-only `{id}` without `body` plus
  `replay: true` = silent car. Always put `body` on the wire to FCM.
- **`google-services` plugin.** Skip it. It is another way for the
  Release job to fail on a missing file.
- **SHA-1.** Firebase Android app fingerprints must include every
  key that signs a sideload (machine debug, `CAB_KEYSTORE_*`).
- **Doze / OEM.** FCM is the Google-blessed wake; it is not a
  promise on a sleeping-apps list. Battery unrestricted stays.
- **High-priority quota.** Family Kit volume is fine. Do not send
  FCM for typing, `ack`, or sibling `inbound`.
- **pendant_handoff.md** "Do not invent a Cab push URL" means do not
  send VAPID subscriptions from OkHttp. `/api/fcm` is mailbox
  work, in that checkout, when this ticket is the ticket.
