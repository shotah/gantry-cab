# Feature parity with the pendant web client

Cab v0.1.6 read against `repos/gantry-pendant` on 2026-09-10. Parity means
the same wire and the same thread behaviour, not the same chrome. PWA-only
bits (Chrome Install, service worker, Web Push) have native analogs that
are already here or already "later". Bugs that are not parity live in
[todo_fixes.md](todo_fixes.md); dev-loop items in
[iteration_improvements_todo.md](iteration_improvements_todo.md).

Pendant references are relative to `repos/gantry-pendant`:
`lib/mailbox/frame.ts` (wire), `app/components/chat/PhoneShell.tsx`
(socket + thread state), `app/components/chat/Thread.tsx` (bubbles),
`docs/todo.md` (what pendant itself has not built).

## Already at parity (do not redo)

| Feature | Pendant | Cab |
| --- | --- | --- |
| Send `inbound` / `pin` / `ack`; never `typing` / `draft` / `cmds` / `allow` | `lib/mailbox/route.ts` | `Wire.kt` `inbound` / `pinFrame` / `ackSince`; `onEngage` is a no-op — keep it that way |
| Reconnect `ack` + `since`, client id dedup | `PhoneShell.tsx:363-366, 454-459` | `MailboxClient.kt:78-79, 88-100` |
| Backoff with cap | 1 s → 30 s | 2 s → 60 s, stop on 401/403/404 |
| `face` avatar rev, `cmds` catalog, `error` → hint, `push` "ping" label | `PhoneShell.tsx:397-405`, `Thread.tsx:45-47` | `Mouth.kt:60-72`, `CabScreen.kt:331-333` |
| Text + one photo (JPEG ≤ 1.5 MB, 1600 px edge), GPS toggle, silent pin, geo hint | `Compose.tsx`, `lib/phone/photo.ts` | `CabCompose.kt`, `Photo.kt`, `MailboxService.kt:152-184` |
| Emoji picker + `:shortcode:`, slash commands from `cmds` | `EmojiPicker.tsx`, `app/lib/slash.ts` | `CabCompose.kt`, `Emoji.kt`, `Slash.kt` |
| Google sign-in, `/api/auth/me` crane switcher, sign out | `LoginDoor.tsx`, `SettingsMenu.tsx` | `Google.kt`, `CabSettings.kt` |
| Themes Boom / Inlay / Lamp; font sm → xl | `app/lib/theme.ts`, `app/lib/font.ts` | `Look.kt`, `CabPalette.kt` |
| Avatar view + tap-to-replace | `KitAvatar.tsx`, `/api/avatar` | `KitAvatar.kt`, `AvatarApi.kt` |
| Unread badge, vibration on ping | `lib/phone/badge.ts`, `lib/phone/haptic.ts` | `CabNotifier.ensureChannel` (`setShowBadge`, `enableVibration`, `DEFAULT_ALL`); 40 ms buzz on a visible `push` |
| Lock-screen when the app is dead | Web Push (VAPID) | FCM — "later" in both READMEs |

## P0 — the thread is missing what the pendant paints

- [x] **Streaming draft bubble (`kind: "draft"`) is dropped.**
  `Mouth.kt:73` returns on `draft`. Pendant keeps one bubble with id
  `__draft__`, replaces its text on every frame, removes it when `text`
  is blank, when a `reply` lands, and when the socket closes
  (`PhoneShell.tsx:47, 374, 417-438, 461-464`; italic dim in
  `Thread.tsx:53`). There are no chunk / delta kinds: every `draft` is the
  full current text (`lib/mailbox/frame.ts:26`). Drafts carry no `id`, so
  `MailboxClient` dedup and `lastSeenId` are untouched — good.
  Fix, all in `Mouth.kt` (pure Kotlin, cheap to test):
  - `const val DRAFT_ID = "__draft__"`.
  - `ingest(draft)`: blank text → drop the `DRAFT_ID` line; else replace
    it (or append) with `ChatLine(DRAFT_ID, fromYou = false, text,
    kind = "draft")`. Keep it after the `takeLast(80)` cap so it is always
    last.
  - `ingest(reply)`: drop `DRAFT_ID` before `add`.
  - `setUp(false)`: drop `DRAFT_ID`.
  - `CabScreen.kt:336`: `kind == "draft"` → italic, `onSurfaceVariant`.
  - `shouldSpeak` is already `reply || push` (`Wire.kt:100`), so no
    notification and no `KitHistory` entry for a draft. Confirm
    `carRows` in `drive/CabCarAppService.kt` skips `draft` or shows it as
    one "typing…" row — a car list that repaints on every token is noise.
  - Tests in `MouthTest.kt`: replace-not-append, blank clears, `reply`
    clears, `setUp(false)` clears, draft stays last past the 80 cap.
  - Add a `stream` sample to `dev/Samples.kt` (pendant
    `lib/dev/samples.ts:178-189`: one `you` line, one `draft` line,
    `typing: true`) so `make shot` can take the screenshot the pendant
    README shows.

- [x] **Typing indicator (`kind: "typing"`) is dropped.**
  `Mouth.kt:73`. Pendant sets `typing = true` with a 6 s TTL
  (`lib/mailbox/typing.ts:4`, refreshed by the crane every ~4 s), clears
  on `reply` / `push` / `error` (`clearsTyping`, `typing.ts:15-17`) and on
  socket close, and paints `· typing…` after `live` in the header
  (`PhoneShell.tsx:845`). Never inferred from `ack`.
  Fix:
  - `Mouth.typingUntil: StateFlow<Long>` (epoch ms, 0 = idle) with an
    injectable clock `now: () -> Long` for tests. `ingest(typing)` →
    `now() + TYPING_TTL_MS`; `ingest(reply|push|error)` → 0;
    `setUp(false)` → 0.
  - `CabScreen.kt:157`: `if (up) "Live" + (if typing " · typing…")`. A
    `LaunchedEffect(typingUntil)` that `delay`s to the deadline flips it
    off without a background timer.
  - Optional: `carRows` shows a trailing "typing…" row while active.
  - Tests: TTL respected, `reply` clears, `push` clears, `ack` does not
    start it, `typing` after `reply` starts again.

- [x] **Local echo is never `pending`; DO `ack` is ignored; text is not
  byte-capped on the way out.**
  Pendant adds your bubble with `pending: true`, paints "sending"
  (`Thread.tsx:58-60`), and clears it when the DO answers
  `{ kind: "ack", id }` (`PhoneShell.tsx:446-452`, DO side `worker/mailbox.ts:255-257`).
  Cab: `CabApp.outbound` adds the line as delivered (`CabApp.kt:24-37`),
  `Mouth.kt:73` drops `ack`, and `MailboxService.sendBlocking` only sets
  a hint when `send` fails (`MailboxService.kt:165-167`) — the same
  silent loss that is P0 in [todo_fixes.md](todo_fixes.md). Also
  `TEXT_CHARS_MAX` (`Wire.kt:28`) caps parsed text by **chars**, and
  outbound text is not capped at all; the DO caps at 8 000 UTF-8
  **bytes** (`lib/mailbox/caps.ts`) and answers `error "too large"`, so a
  long multibyte message shows as sent and never arrives.
  Fix (one change, both docs):
  - `ChatLine.pending: Boolean = false`. `outbound(...)` sets it true.
    `Mouth.ack(id)` flips it false; `ingest(ack)` calls it.
  - Outbox in `MailboxService`: `ArrayDeque<WireFrame>`; `send` that
    returns false enqueues; `onState(true)` flushes **after**
    `MailboxClient` has sent `ackSince`. `pinFrame` is fire-and-forget —
    do not queue pins (the DO does not queue them either, `queue.ts:24-25`).
  - Cap outbound text by UTF-8 bytes before `encodeFrame`
    (`text.toByteArray(Charsets.UTF_8).size`), matching `caps.ts`.
  - `CabScreen.kt`: `mine && line.pending` → small "sending" label.
  - Pendant leaves a never-acked bubble as "sending" forever; failed +
    retry is on *their* todo, not parity. Stop at pending + outbox.
  - Tests: `MouthTest` ack flips pending; `MailboxClientTest` (MockWebServer)
    asserts `ack since` arrives before the flushed frame; `WireTest`
    byte cap.

- [x] **Kit's replies are painted as plain text.**
  Pendant renders every bubble through `MarkdownBody.tsx` (GFM + breaks:
  links open in a new tab, inline and fenced code, lists, tables, task
  lists, blockquotes). Cab is `Text(line.text)` at `CabScreen.kt:336`, so
  a code fence or a table from Kit is a wall of asterisks and pipes.
  Fix (import, do not write a parser):
  - `com.mikepenz:multiplatform-markdown-renderer:0.43.0` +
    `com.mikepenz:multiplatform-markdown-renderer-m3:0.43.0` (JetBrains
    `markdown` parser underneath, GFM flavour by default, Material 3
    colours from `MaterialTheme`). `import com.mikepenz.markdown.m3.Markdown`.
  - Use it for every bubble like the pendant does; pass `chat` font size
    through `markdownTypography` so `Look` font ids still apply.
  - The draft bubble can use `rememberStreamingMarkdownState()` from the
    same library — it re-parses only the tail as text grows.
  - Notification text (`CabNotifier.kitMessage`) and `carRows` stay
    plain. If the asterisks bother you there, walk the parser's AST for
    text nodes — it is already on the classpath — rather than regexing.
  - No test on the renderer itself; `MouthTest` is unchanged. Lint will
    flag the new dependency version in Dependabot's next pass.

## P1 — thinner context on the wire than the pendant sends

- [x] **No `context.battery` or `context.net`.**
  `PhoneContext` is `at` / `tz` / `geo` only (`Wire.kt:12-16`). Pendant
  sends `battery: { pct, charging }` and `net: "wifi" | "cellular" | "unknown"`
  on every send (`frame.ts:16-22`, `PhoneShell.tsx:587-594`); the DO
  validates both (`frame.ts:96-104`). The harness has them for the
  clock footer.
  Fix: add both to `PhoneContext` + `encodeFrame`; read them in
  `MailboxService.phoneContext` from `BatteryManager`
  (`BATTERY_PROPERTY_CAPACITY`, sticky `ACTION_BATTERY_CHANGED` for
  `charging`) and `ConnectivityManager.getNetworkCapabilities`
  (`TRANSPORT_WIFI` → `wifi`, `TRANSPORT_CELLULAR` → `cellular`, else
  `unknown`). `WireTest` covers the JSON shape.

- [x] **Geo drops `alt_m`, `heading`, `speed_mps`.**
  `Geo` is lat / lon / accuracy (`Wire.kt:6-10`). Pendant's schema takes
  altitude, heading `[0, 360)` and speed `≥ 0` (`frame.ts:7-14, 64-77`).
  In a car heading and speed are the interesting part of the pin.
  Fix: extend `Geo` + `encodeFrame`; in `lastKnownGeo` / `freshGeo` copy
  `Location.altitude` / `bearing` / `speed` only when the matching
  `has*()` is true, and only within the DO's ranges so the frame is not
  rejected as `bad frame`.

- [x] **`https://` photos from the crane vanish.**
  The DO lets the crane send `images: [{ url: "https://…" }]`
  (`frame.ts:108-113`); pendant paints it with `<img src>`. Cab's
  `acceptInboundImage` (`Wire.kt:107-115`) accepts the URL, but
  `ChatPhoto` (`CabScreen.kt:350-363`) only `decodeDataUrl`s, so the
  bubble is empty.
  Fix (import): Coil 3 — `io.coil-kt.coil3:coil-compose` +
  `io.coil-kt.coil3:coil-network-okhttp` (reuses the OkHttp already in
  the app). `AsyncImage(model = url)` loads both `data:image/…` and
  `https://`. Tighten `acceptInboundImage` to `https://` only for
  non-data URLs. Photo-only reply notifications should say "Photo", not
  "ping" (`MailboxService.kt:120`; pendant `notifyBody`,
  `lib/phone/notify.ts:72-78`).

## P2 — small things the pendant does that cab does not

- [x] **No timestamp on `ChatLine`.** Pendant carries `at` on every bubble
  (`Thread.tsx:11`) even though painting it is still on their todo. Add
  `at: Long = System.currentTimeMillis()` now so day chips are a UI-only
  change later, and so `KitHistory` can stop timestamping separately
  (`CabNotifier.kt:59`).

- [x] **Waiting-for-allowlist shows a sentence, not the id.** Pendant shows
  email + Google `sub` with a Copy button when `cranes` is empty
  (`PhoneShell.tsx:708-712, 757-797`) — that is what the yard admin needs.
  Cab parses `sub` (`AuthApi.kt:16`) and never shows it
  (`mailboxSignedInHint`, `MailboxConnect.kt:73-78`). Add a `sub` row +
  `ClipboardManager` copy in `CabSettings` for the empty-cranes case.

- [x] **Notification fires with the app in the foreground.** Pendant only
  toasts / badges when the tab is hidden (`shouldNotify`,
  `lib/phone/notify.ts:64-70`; `shouldBadge`, `badge.ts:7-9`). Cab posted
  `kitMessage` for every `reply` / `push`, so on the phone you got the
  bubble and a heads-up for the same line. In the car the notification
  *is* the mouth.
  Fix: `shouldPost(resumed, carAttached, kind)` in `NotifyGate.kt`.
  `MainActivity` onResume/onPause toggles `CabApp.phoneResumed` (not
  `ProcessLifecycleOwner` — the Auto session would look like foreground
  and swallow HUNs). `CarConnection` projection/native sets
  `carAttached`. Post when a head unit is attached **or** the phone
  thread is not resumed. Visible `push` with no car: 40 ms
  `VibrationEffect` (`shouldBuzz`, pendant `haptic.ts`).

- [x] **Dev samples missing `stream` and `photo`.** `SAMPLE_IDS` is
  `unsigned, empty, thread, ping, down` (`Samples.kt:5`); pendant also has
  `cmds, stream, emoji, photo, crane` (`lib/dev/samples.ts:10-20`).
  `stream` is required by the draft item above; `photo` lets `make shot`
  cover the bubble image. `crane` is the loopback stand-in — skip.

- [x] **Screen stays off while waiting.** Pendant takes a wake lock after
  send and releases it on reply / close (`app/lib/wake.ts`,
  `PhoneShell.tsx:550-558`). Android: `FLAG_KEEP_SCREEN_ON` on the window
  while any line is `pending` or `typingUntil > now`. Optional — the head
  unit does this on its own.

- [ ] **No "Drop a pin" entry point outside the app.** Pendant registers a
  PWA shortcut `/?pin=1` (`PhoneShell.tsx:535-548`). Cab already pushes a
  conversation shortcut; a static `shortcuts.xml` entry that starts
  `MailboxService.sendPin` is the same one-tap.

## Not parity — pendant does not have these either

Do not build these "to catch up"; they are open on pendant's own list
(`docs/todo.md` → Mouth UI / Later) or explicitly out of scope there:

- Transcript that survives reload (the 50-frame / 1 h queue is not history)
- Failed + retry on an unacked send; copy on long-press
- Painted timestamps / day chips
- Crane presence (`asleep` / `queued`) separate from socket `live`
- Caption + photo in one send (both clients send the photo immediately)
- Stop-a-turn button, quote / reply-to, inline Yes / No, one non-image file
- Mute pings, photo lightbox, voice into compose, share target
- Reactions, read receipts, edit / delete, presence frames, delta
  streaming kinds — "Not this version" in pendant

## Cab has that pendant does not (for the record)

`MessagingStyle` with six turns of history, `RemoteInput` reply,
mark-as-read, conversation shortcut, Android Auto `ConversationItem`
(Car API 7) with host Reply, a foreground socket that outlives the
screen, native Google token flow without a browser. Nothing here to
remove. Google Assistant / maps / music are untouched.

## Order

1. Draft + typing in `Mouth` (pure, tested, one header change). Add the
   `stream` sample in the same commit so the screenshot exists.
2. Pending / `ack` / outbox / byte cap — closes the todo_fixes P0 too.
3. Markdown renderer.
4. Battery, net, altitude / heading / speed on the frame.
5. Coil for `https://` photos; "Photo" notification body.
6. `sub` + copy, foreground notification gate, `photo` sample, wake,
   pin shortcut.

## Verify with

```bash
make test-scripts
./gradlew testDebugUnitTest lintDebug createDebugUnitTestCoverageReport
make coverage-gate
make shot            # after the `stream` sample lands; compare with pendant assets/docs/stream.png
```

Wire walk: pendant `npm run dev`, open `/crane` beside the emulator. From
the crane tab send `{ "kind": "typing", "user_id": "<sub>" }`, then two
`{ "kind": "draft", "user_id": "<sub>", "text": "…" }` frames with
growing text, then a `reply`. Cab should show `· typing…`, one italic
bubble that changes in place, then a normal bubble and no draft. Kill the
Worker mid-draft: the draft bubble and `typing…` must both go.
