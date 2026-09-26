# Helm parity — what the iPhone still owes

Cab is the Android mouth; [gantry-helm](https://github.com/shotah/gantry-helm)
(`repos/gantry-helm`) is the iPhone one. Same mailbox, same frames.
This page is the running list of what landed on Cab that Helm has
not matched yet, so a Helm agent has one place to look. The mailbox
contract for each row is pendant
[`docs/frontends.md`](https://github.com/shotah/gantry-pendant/blob/main/docs/frontends.md);
the Cab code named here is the reference paint. **Do not edit Helm
from this checkout.**

A row leaves this page when the Helm build ships it. Order is the
same as pendant's table: wire first, then UI.

## Wire (must not paint wrong or drop a turn)

- [ ] **`aims` (goals board).** New `kind`, crane only, no `text`.
      Helm `Mouth.ingest` must return before the bubble path; add it
      to the ignored-kind list and to `movesCursor`'s exclusions.
      Parse per frontends.md Aims board with the caps 5 / 14 / 13 / 3,
      drop a bad row not the board, drop a half-formed `block` /
      `effect` / week whole. Cab: `mailbox/Aims.kt`, `Mouth.aims`.
- [ ] **`react` ignore-then-paint.** frontends.md still lists Helm's
      four boxes open. Cab: `mailbox/React.kt`, `Mouth.applyReaction`,
      `ui/ChatTurn.kt` hold → palette.
- [ ] **`seen` on ack.** `WireFrame.seen` (parse `true` only), the two
      dismiss `if`s, the two send points (connect with thread on
      screen; per live `reply` / `push` painted). Cab:
      `MailboxService.onFrame`, `ackSeen`, `dismissKitOnFrame`.
- [ ] **`act` (device actions).** Not shipped anywhere yet. When
      pendant lands routing: `device` / `kind=helm` / `caps` / `label`
      on the upgrade, `act` parse, AlarmKit executor, `needs_permission`
      when the AlarmKit prompt was not accepted. Full contract:
      [device_actions.md](device_actions.md).

## UI (same words, same order)

- [ ] **Goals board.** Header target with `goals (n)`, hidden when the
      board is empty. Sheet: one card per aim — `area` + signed
      `rating30`, sentence, day grid (sign hue, magnitude weight,
      eventless outline, score under each cell), stamp line exactly
      `30d +1.4 · 7d +6 · streak 2 · asked`, week strip when `weeks`
      is there, trend line when present (`slope +0.3/wk · block 4/10
      (40%) · weight r -0.42 (n 9)`), then the `links` lines
      (`training → next-day weight r +0.38 (n 12)`). Buttons are
      turns: `/aims <area>`, `/aims`, `/aims rubric`; the sheet closes.
      **CarPlay: nothing.** Cab: `ui/GoalsBoard.kt`, `GoalsBoardTest`.
- [ ] **Reactions UI.** Context menu on a Kit bubble is the iOS shape;
      CarPlay read-only.
- [ ] **Pocket voice.** Header mic when `/api/auth/config` says
      `voice`; hold bar; `SFSpeechRecognizer` in the Language locale;
      `POST /api/tts` with `lang`. Cab: `ui/HoldToTalk.kt`, `KitVoice`,
      `mailbox/Speech.kt` / `Speaker.kt` / `Lang.kt`.
- [ ] **Settings → Language.** Same four ids, same labels, UserDefaults
      `helm` / `lang`. Only shown when voice is published.

## Already matched (per frontends.md)

Thread order (`seq` / `at`, `placeInThread`, highest-seq ack),
transcript hydrate + `shouldSpeak(kind, replay)`, `ThreadCache`,
draft / typing rules, photo caps + ladder + `SendError`, face /
backdrop / theme notices and the 82 / 80×40 / −2/−4 header hang,
`surface` `ios` / `carplay`, Google sign-in with server nonce,
4401 drops the JWE.

## Not Helm's to do

FCM / APNs lock-screen (public-scale only), Sign in with Apple
(pendant auth change first), a second Durable Object.
