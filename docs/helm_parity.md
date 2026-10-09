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

## UI (phone only; CarPlay shows nothing new)

- [ ] **Mood themes.** Fifteen ids: Plain `boom` / `paper` / `ink`, then
      six feelings × dark / light (`marquee`/`lemonade`, `neon`/`fizz`,
      `rain`/`mist`, `fuse`/`grit`, `siren`/`flare`, `static`/`flicker`).
      A stored retired id (`inlay`, `lamp`, `noir`, `ember`, `tide`,
      `bloom`, `chalk`, `foam`, `petal`) falls to `boom`; a notice with
      one of those is ignored. Settings groups Plain and Moods; a tap
      writes the id and turns follow off. Kit's room id wears a Kit tag
      while follow is on. CarPlay stays the car's palette. Cab:
      `Look.kt` `THEME_IDS`, `CabPalette.kt`, `CabSettings` theme menu.
- [ ] **Avatar sheet.** Tap the header face → a sheet, not the photo
      picker: the face large (160 pt, same ring as the header), the
      crane's name, then **Copy** (the JPEG on the pasteboard as an
      image, label flips to "Copied", sheet stays up), **Share**
      (system share sheet over the JPEG, closes), **Replace** (the
      photo picker the tap used to open, closes). Copy and Share only
      when the room has set a face; the bundled default is nobody's
      work, so that sheet is Replace alone. The Google door still does
      not open it. Cab: `ui/AvatarSheet.kt`, `JpegIo.avatarShareUri`
      / `shareImageIntent`, `AvatarSheetTest`.
- [ ] **Copy text from a bubble.** Hold on **any** bubble with words
      — yours too, socket down too — opens the bubble menu, not only
      Kit's reactable ones. First row **Copy text**: the raw markdown
      of the bubble goes on the pasteboard and the menu closes. The
      emoji rows sit under it only when `canReact` (Kit `reply` /
      `push`, live). A photo-only bubble has no copy row. Cab:
      `React.canCopy` / `canHold`, `ui/ChatTurn.kt` `BubbleMenu`,
      `ChatTurnTest`.
- [ ] **Task priority.** No wire change: the crane leads a row's `text`
      with `!! ` (urgent) or `! ` (high); nothing is normal (crane
      `internal/memory/tools.go`). Keep `text` raw (the seen-badge keys
      on it). Tasks sheet: stable sort urgent → high → rest, oldest
      first inside each rank; paint the marker as a coloured tag ahead
      of the words (`!!` error / `!` tertiary, accessibility label
      "urgent" / "high"), not as the first word. `!!!`, a glued
      `!!file`, or a bare `!!` are words, not a marker. Cab:
      `Todo.todoPriority` / `todoWords` / `sortTodo`,
      `ui/TasksBoard.kt`, `TodoTest`, `TasksBoardTest`.

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
