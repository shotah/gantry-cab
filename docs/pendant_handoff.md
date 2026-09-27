# Pendant mailbox — Cab handoff

Mailbox checkout (`gantry-pendant`) did a 2026-09-12 Before go-live
pass, then a docs pass the same day (`audit_todo.md`, `frontends.md`,
`sibling_phones.md`, `security.md`). **This file is the Cab work.**
Do not wait for a pendant agent to touch Kotlin. Product / security
work that is already Cab-local stays in [todo.md](todo.md).

Wire: pendant
[`docs/frontends.md`](https://github.com/shotah/gantry-pendant/blob/main/docs/frontends.md)
(PWA, Cab, and Helm).
Voice: pendant
[`docs/voice.md`](https://github.com/shotah/gantry-pendant/blob/main/docs/voice.md).
Sibling: [sibling_phones.md](sibling_phones.md). Audit leftovers:
pendant
[`docs/audit_todo.md`](https://github.com/shotah/gantry-pendant/blob/main/docs/audit_todo.md).

A **ship / walk** line is done when it works on a sideload APK against
the hydrating Worker. Code-complete items below are `[x]` in this
checkout; the remaining boxes are a tagged APK and a sibling walk on
the deployed Worker (fan is already in `worker/mailbox.ts`). Pendant
still owns session revocation (`iat` floor / `jti`) and **requiring**
stored nonces.

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
  semver) and `dev`. `AuthConfig` keeps `mode` / `google` / `voice` and
  drops unknown keys. `AuthApiTest.configIgnoresAdditiveKeys`. `voice`
  (missing → `false`) is whether the Worker has a TTS key and
  `VOICE` is not `off`; it gates the header mic and the hold bar.
- **Push host allowlist** is PWA Web Push (`PUT /api/push`). Cab does
  not subscribe that way. Do not invent a Cab push URL.
- **Google return-to** (`/?slug=` on the OAuth state cookie) is the PWA
  browser flow. Cab never hits `GET /api/auth/google`.
- **`GET /api/auth/logout` is gone.** Cab does not use it (no cookie).
  Sign-out is `CabPrefs.signOut()` (drops the JWE).
- **Header face hang.** 82.dp circle, 40×80 empty `navigationIcon`
  slot, nudge **-2.dp x / -4.dp y**, 2.dp `line` stroke. The circle is
  a Scaffold overlay (`zIndex` above the bar), not inside `TopAppBar`,
  so clip never eats it. Empty / Google door stay the centered hero.
  `DocsShot.kt` `paintHeader` uses the same 82 / 80×40 / −2/−4 numbers.
  Hint text starts right of the hang.
- **Caption + attach: one inbound.** Compose stages the JPEG; Send
  emits one `inbound` with `text` + `images[0]`. Attach itself never
  sends. `MailboxService.sendTurn`, `composeHasTurn`.
- **Thread cache.** `ThreadCache` paints the last room before hydrate;
  connect `ack` `since` is the highest cached seq (`MailboxService`
  `cursorOf` + `remember`). Same mailbox contract as the PWA IndexedDB
  cache. Not Room. Face / wallpaper: `BlobCache` + `If-None-Match`
  (`AvatarApi`).

---

## Lockstep — Cab is ready; do not require stored nonces yet

Pendant owns the Worker half. `GET /api/auth/nonce` exists. POST still
accepts a missing row so an old APK can sign in.

- [x] **Server-issued native nonce (Cab half).** `AuthApi.nonce()` GETs
      `/api/auth/nonce`. 404 / junk / empty → `mintNonce()` (same
      `SecureRandom` as before). That value goes to Google Sign-In and
      `POST /api/auth/token`. Files: `mailbox/AuthApi.kt`,
      `ui/Google.kt`, `CabViewModel`. Tests: `AuthApiTest`.
      **Pendant now issues the route** (5 min, consume-once). POST
      still accepts a missing row so an old APK can sign in. Replay
      of a consumed server nonce is 401. **Do not require stored
      nonces until this APK is the sideload.**
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
      with the hydrating Worker (and the nonce fetch above).
- [ ] **Walk sibling inbound** on the deployed Worker. Fan is
      shipped (`siblingPhoneTag` → `sub:<userId>` except the sender).
      Cab already paints `inbound` as you and skips HUN. Keep
      `MailboxClient.sweep`. Walk: [sibling_phones.md](sibling_phones.md).
- [x] **Reactions (Cab half).** `kind: react` is not a turn:
      `Mouth.ingest` lands the emoji on the named bubble (empty
      text clears) and never paints a stray `👍` bubble. Long-press
      a Kit `reply` / `push` (450 ms, 10 px cancel) opens the same
      palette as the PWA; tap the chip to reopen; picking the set
      emoji clears. Socket down does nothing. Auto is read-only.
      `ReactTest` / `MouthTest` / `ChatTurnTest`. Contract: pendant
      `docs/frontends.md` Reactions. A phone `react` while the crane
      is down is queued on the Worker (`q:crane:<bubble id>`, latest
      wins) — not on this APK. Cab still sends only while the socket
      is up and ignores `seq` / `at` on a `react`.
- [x] **Goals board (Cab half).** `kind: aims` is not a turn:
      `Mouth.ingest` replaces `Mouth.aims` (empty array clears, no
      array keeps the last board) and never paints a bubble;
      `movesCursor` skips it. `mailbox/Aims.kt` parses the pendant
      shape with the same caps (5 aims, 14 days, 13 weeks, 3 links),
      drops a bad row not the board, drops a half-formed `block` /
      `effect` / week whole, and builds the `[aims]` stamp line, the
      trend line, and the `/aims` footer line word for word. Header
      target whenever the board has rows (`ui/GoalsBoard.kt`); **the
      badge is changes, not aims** — `changedAims` against the board
      last opened (`CabPrefs.aimsSeen`, area → row JSON, mirrors
      `pendant.aimsSeen`), marked seen on open and while open, so a
      reconnect replay of the same board badges nothing and a quiet
      board is a bare target. The sheet is one card per aim (signed
      `rating30`, sentence, day grid, stamp, week strip, trend), then
      the links. Every button is a visible turn through the normal
      send: `/aims <area>`, `/aims`, `/aims rubric`. **Auto: nothing.**
      `AimsTest` / `MouthTest` / `GoalsBoardTest`. Contract: pendant
      `docs/frontends.md` Aims board. Waiting on the crane to send
      the frame (ai-gantry `docs/aims-progress.md`).
- [x] **Tasks board (Cab half).** `kind: todo` is not a turn:
      `Mouth.ingest` replaces `Mouth.todo` (empty array clears, no
      array keeps the last list, order kept) and never paints a
      bubble; `movesCursor` skips it. `mailbox/Todo.kt` parses the
      pendant shape (positive `id`, slug `[a-z0-9][a-z0-9_-]*`, text
      collapsed and capped at 240 runes, `at` a date), cap 100, drops
      a bad row or a repeated id/slug not the list. Header check-square
      whenever the list has rows (`ui/TasksBoard.kt`); **the badge is
      changes, not tasks**, keyed by slug (`CabPrefs.todoSeen`,
      mirrors `pendant.todoSeen`), marked seen on open and while open.
      Sheet: checkbox, words, `#id · slug · age` (age after the first
      day). Tick sends `/todo done <id>`, stays open, strikes the row,
      and will not send twice; the next frame settles every tick. Add
      sends `add to my list: <words>` and "Full list" sends `/todo`;
      both close the sheet. Past 10 open, the pocket-list footer.
      **Auto: nothing.** `TodoTest` / `MouthTest` / `TasksBoardTest`.
      Contract: pendant `docs/frontends.md` Tasks board. Waiting on
      the crane to send the frame (ai-gantry `docs/tasks.md`).

---

## Optional / later

Same later as pendant Mouth UI. Do not build these "to catch up"
unless that walk is the ticket ([todo.md](todo.md) Not this version).

- [ ] Parse `version` from `/api/auth/config` (settings chip, "mailbox
      is newer than this APK"). Additive; skip is fine — unknown keys
      are already dropped.
- [x] **Pocket voice** (pendant `docs/voice.md`, Cab parity). Header
      mic left of the cog (only when `/api/auth/config` says `voice`)
      flips typing ↔ hold-to-talk, remembered in `CabPrefs.voice`.
      Voice on swaps the compose row for one wide hold bar
      (`ui/HoldToTalk.kt`): press listens on `SpeechRecognizer`
      (`ui/HoldListener.kt`), growing hypotheses fold to one line
      (`mailbox/Speech.kt`), release sends `inbound` + `input:
      spoken` (`MailboxService.sendSpoken`), slide-off aborts, a
      watchdog ends a hung session. The next live `reply` after a hold
      is read via `POST /api/tts` with the Bearer session
      (`mailbox/TtsApi.kt`, `KitVoice`), markdown → words first
      (`mailbox/Speakable.kt`); `push` / `replay` / typed turns never
      speak, `error` disarms (`mailbox/Speaker.kt`). Header says
      `Live · voice…` / `· speaking`. Settings → Access: Enable
      microphone / location / notifications. **Settings → Language**
      (`en` / `ja` / `zh` / `vi`, default `en`, `CabPrefs.lang` = the
      same ids as `pendant.lang`; only shown when voice is published):
      the hold bar listens in its BCP-47 (`EXTRA_LANGUAGE` +
      `EXTRA_LANGUAGE_PREFERENCE`, `zh` → `zh-CN`, `vi` → `vi-VN`) and `/api/tts` gets
      additive `lang` so the Worker swaps the Chirp locale and keeps
      the speaker (`mailbox/Lang.kt`; pendant `frontends.md`
      Language). Not on the mailbox wire. **Auto is untouched**:
      host STT in, Auto reads the card out; `KitVoice` is
      `MainActivity`-only and the recognizer never runs on the
      template.
- [ ] FCM lock-screen — public-scale only, not demo/beta. Design:
      [fcm_design_and_todo.md](fcm_design_and_todo.md).

---

## Watch

- Do not send `seq` / `at` from the phone. The mailbox stamps those.
- Do not concatenate `PhoneContext` into inbound `text`. `inbound()`
  runs `stripHarnessContext` so a pasted `[current time]` / `[location`
  footer is not speech. Keep `context` JSON (`geo` and the optional
  keys). PWA inbound `context` is **geo only**; Cab may still stamp
  `at` / `tz` / battery / net (additive, ignored). `surface` and
  `input: spoken` (Auto host STT, and the handheld hold bar) are read
  by the crane as the `[surface]` / `[input]` stamps — closed sets,
  junk is dropped. Typed compose stays untagged.
- No audio on the wire. STT is the phone's, TTS is `POST /api/tts`
  bytes played from memory (`MediaDataSource`), never a file.
- Language is not a `context` key. Send the id (`ja`), never the
  recognizer tag (`ja-JP`) — the Worker drops what it does not know
  and speaks its configured voice. Growing the set is one row in
  `LANGUAGES` here and the same row in pendant `lib/phone/lang.ts`.
- Additive JSON is fine. A new required field or `kind` needs a Cab
  change or mailbox tolerance for the old APK.
- Do not start Expo to catch up the PWA.
- Do not add a `CookieJar` that stores `pendant_session`.
- `GET /api/auth/nonce` is issued. POST still accepts a locally minted
  nonce (`missing` row). After pendant **requires** stored nonces, an
  old APK that only mints locally cannot sign in.
