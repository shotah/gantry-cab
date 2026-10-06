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

- [ ] **`act` (device actions).** Not shipped anywhere yet. When
      pendant lands routing: `device` / `kind=helm` / `caps` / `label`
      on the upgrade, `act` parse, AlarmKit executor, `needs_permission`
      when the AlarmKit prompt was not accepted. Full contract:
      [device_actions.md](device_actions.md).

## Already matched (per frontends.md)

Thread order (`seq` / `at`, `placeInThread`, highest-seq ack),
transcript hydrate + `shouldSpeak(kind, replay)`, `ThreadCache`,
draft / typing rules, photo caps + ladder + `SendError`, face /
backdrop / theme notices and the 82 / 80×40 / −2/−4 header hang,
`surface` `ios` / `carplay`, Google sign-in (nonce only from
`GET /api/auth/nonce`; a failed GET stops sign-in),
4401 drops the JWE.

`aims` (caps 5 / 14 / 13 / 3, not a turn, header badge is changes
since `helm` / `aimsSeen`, sheet copy, CarPlay shows nothing),
`todo` (cap 100, not a turn, header check-square badges changes
since `helm` / `todoSeen` keyed by slug, checkbox `/todo done <id>`
stays open, add and Full list close, CarPlay shows nothing),
`react` ignore-then-paint (context menu on a Kit bubble, CarPlay
read-only), `seen` on ack (connect with the thread up, and each live
`reply` / `push`; a sibling inbound or a seen ack drops the local
card), pocket voice (header mic when config says `voice`, hold bar,
`SFSpeechRecognizer`, `POST /api/tts` with `lang`), Settings →
Language (the same four ids, only when voice is published).

## Not Helm's to do

FCM / APNs lock-screen (public-scale only), Sign in with Apple
(pendant auth change first), a second Durable Object.
