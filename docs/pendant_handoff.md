# Pendant mailbox — Cab handoff

Mailbox checkout (`gantry-pendant`) did a 2026-09-12 Before go-live
pass. **This file is the Cab work.** Do not wait for a pendant agent
to touch Kotlin. Product / security work that is already Cab-local
stays in [todo.md](todo.md).

Wire: pendant
[`docs/frontends.md`](https://github.com/shotah/gantry-pendant/blob/main/docs/frontends.md).
Voice: pendant
[`docs/voice.md`](https://github.com/shotah/gantry-pendant/blob/main/docs/voice.md).
Sibling: [sibling_phones.md](sibling_phones.md). Audit leftovers:
pendant
[`docs/audit_todo.md`](https://github.com/shotah/gantry-pendant/blob/main/docs/audit_todo.md).

A **ship / walk** line is done when it works on a sideload APK against
the hydrating Worker. Code-complete items below are `[x]` in this
checkout; the remaining boxes are a tagged APK, a Worker fan-out, or
pendant still holding the other half.

---

## Already true (do not "fix")

These landed on the Worker. Old Cab APKs stay valid.

- **Cookie CSRF is PWA-only.** Mutating `/api/*` with a
  `pendant_session` cookie needs `Sec-Fetch-Site` `same-origin` /
  `none` (or a matching `Origin`). OkHttp uses `CookieJar.NO_COOKIES`
  on `AuthApi` and `MailboxClient` and sends `Authorization: Bearer
  <jwe>` with **no** session cookie. Missing `Sec-Fetch-Site` is
  allowed. Do not add a CookieJar that stores `pendant_session`.
- **`GET /api/auth/config`** may include additive `version` (package.json
  semver) and `dev`. `AuthConfig` keeps `mode` / `google` and drops
  unknown keys. `AuthApiTest.configIgnoresAdditiveKeys`.
- **Push host allowlist** is PWA Web Push (`PUT /api/push`). Cab does
  not subscribe that way. Do not invent a Cab push URL.
- **Google return-to** (`/?slug=` on the OAuth state cookie) is the PWA
  browser flow. Cab never hits `GET /api/auth/google`.
- **`GET /api/auth/logout` is gone.** Cab does not use it (no cookie).
  Sign-out is `CabPrefs.signOut()` (drops the JWE).
- **Header face hang.** 82.dp circle, 40×80 slot, nudge **-2.dp x /
  -4.dp y**, 2.dp `line` stroke, avatar drawn above the bar (not clipped
  by `TopAppBar`). Empty / Google door stay the centered hero.
  `DocsShot.kt` `paintHeader` matches. Hint text starts right of the
  hang.
- **Caption + attach: one inbound.** Compose stages the JPEG; Send
  emits one `inbound` with `text` + `images[0]`. Attach itself never
  sends. `MailboxService.sendTurn`, `composeHasTurn`.
- **Thread cache.** `ThreadCache` paints the last room before hydrate;
  connect `ack` `since` is the highest cached seq (`MailboxService`
  `cursorOf` + `remember`). Same mailbox contract as the PWA IndexedDB
  cache. Not Room.

---

## Lockstep — Cab is ready; do not require the Worker first

Pendant still owns the Worker half. Cab will talk to the new routes
when they exist and keeps working when they do not.

- [x] **Server-issued native nonce (Cab half).** `AuthApi.nonce()` GETs
      `/api/auth/nonce`. 404 / junk / empty → `mintNonce()` (same
      `SecureRandom` as before). That value goes to Google Sign-In and
      `POST /api/auth/token`. Files: `mailbox/AuthApi.kt`,
      `ui/Google.kt`, `CabViewModel`. Tests: `AuthApiTest`. **Pendant
      must not require the route until a Cab APK with this fetch is
      the sideload.** Then: add `GET /api/auth/nonce` (one-time, ~5
      min), then require it.
- [x] **4401 / handshake 401 drops the JWE (Cab half).** Close `4401`
      (yanked `sub`, expired session on the next frame, later an `iat`
      floor) and HTTP 401 on upgrade stop retry, call
      `CabPrefs.signOut()`, and hint "sign in again". HTTP **403**
      (wrong room, same human) does **not** drop the session.
      `mailboxCloseDropsAuth`, `MailboxClient.onAuthLost`. **Pendant
      session revocation** (`iat` floor / `jti` denylist) is still
      open on the Worker — close those sockets `4401` the way yank
      already does. Do not invent a Cab-only revoke.

---

## Ship / walk (mailbox already there)

- [ ] **Ship the APK that skips Auto HUN on `replay`.**
      `shouldSpeak(kind, replay)` is in tree. An old APK still
      paints and still toasts every hydrate frame. Tag a sideload
      with the hydrating Worker (and the nonce / 4401 bits above).
- [ ] **Walk sibling inbound** after the Worker fans Ada's send to
      her other open sockets (`sub:<userId>` except the sender).
      Cab already paints `inbound` as you and skips HUN. Keep
      `MailboxClient.sweep`. Walk: [sibling_phones.md](sibling_phones.md).

---

## Optional / later

Same later as pendant Mouth UI. Do not build these "to catch up"
unless that walk is the ticket ([todo.md](todo.md) Not this version).

- [ ] Parse `version` from `/api/auth/config` (settings chip, "mailbox
      is newer than this APK"). Additive; skip is fine — unknown keys
      are already dropped.
- [ ] Handheld mic → compose (`SpeechRecognizer`), no auto-send. Auto
      stays host STT. Design: pendant `docs/voice.md`.

---

## Watch

- Do not send `seq` / `at` from the phone. The mailbox stamps those.
- Additive JSON is fine. A new required field or `kind` needs a Cab
  change or mailbox tolerance for the old APK.
- Do not start Expo to catch up the PWA.
- Do not add a `CookieJar` that stores `pendant_session`.
- `GET /api/auth/nonce` is optional until pendant requires it. After
  that, an old APK that only mints locally cannot sign in.
