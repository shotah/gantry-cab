# Device actions — alarms and timers from the crane

Handoff for **ai-gantry** (the harness) and **gantry-pendant** (the
mailbox Worker). Cab (this checkout) and Helm are the executors.
Nothing here is in tree yet. Ship order: pendant → ai-gantry → Cab →
Helm. An old Cab APK drops the new frame with no bubble, so there is
no lockstep.

Problem: "wake me at 7" needs an alarm on the phone in the human's
pocket. The Mini has no phone API. The phone has no model. The
socket between them already exists (`role=crane` out of the Mini,
`role=phone` out of the pocket). Alarms ride that socket as a
request/response frame that is not a turn. "Tell me when to baste"
is not an alarm; it is Kit speaking at a time, and that lives on the
mailbox's own clock (DO `alarm()`), not the phone's.

```text
Kit tool call ─► ai-gantry tool handler ─► act (crane socket)
                                              │
                                pendant Worker routes to the phone socket
                                that produced the current turn
                                              │
                                Cab / Helm ─► AlarmManager / AlarmKit
                                              │
                                act answer, same id ◄─┘
                                              │
                                tool returns ─► Kit speaks the result
```

## Decisions (do not relitigate)

- **The tool lives on the crane.** ai-gantry owns `alarm.*` /
  `timer.*` in its catalog like any other tool. Kit decides. No regex
  over `reply` text. Not a slash command (`cmds` are the human's
  verbs).
- **New `kind: "act"`, never `push` with extra keys.** `Mouth.ingest`
  paints any `reply` / `push` with text as a bubble. A frame with a
  new `kind` and no `text` falls through and is dropped by today's
  Cab. That is the mailbox tolerance rule in
  [pendant_handoff.md](pendant_handoff.md) Watch, already satisfied.
- **Not a turn.** No `seq` / `at`, not hydrated, not fanned to
  siblings, not in the 80-frame replay. Treat like `typing`.
- **Capabilities are announced at connect, not discovered by
  failing.** A phone socket says `device` and `caps` on the upgrade.
  The Worker stamps the `inbound` it forwards to the crane with both;
  the harness turns that into a `[device …]` stamp next to `[surface]`
  / `[input]`. The model knows whether there is a Cab / Helm before
  it reaches for the tool. The PWA announces nothing.
- **Route by capability, then by turn.** `device` override → the
  `turn` socket if it has the cap → any socket on the same `sub` with
  the cap. Typing "wake me at 7" in the browser at a desk still lands
  on the phone in the pocket. No "last phone that typed" heuristic.
- **Offline is an answer, not a queue.** Target socket down → Worker
  answers `offline` at once. Do **not** queue like `q:crane` for
  `react`. A late alarm is worse than "your phone isn't reachable."
- **Noise is the phone's; words are the mailbox's.** A timer or
  alarm rings with the screen off and needs Cab / Helm. Anything Kit
  should *say* at a time — a reminder, the next step in a sequence,
  a turn it has to think about — is a **schedule on the Durable
  Object** (`schedule.*`, DO `alarm()`), not a phone timer and not a
  crane-process cron. It survives a crane restart and works for
  PWA-only users. No phone → alarms are `no_device`; schedules still
  work. See Sequences and crons.
- **The phone never trusts the model.** Fixed schema, closed sets,
  bounded strings. The phone validates and refuses with `bad_input`.
- **Only Kit's alarms.** Neither OS lets a sideload app list, enable,
  disable, or delete alarms in the system Clock. Cab keeps its own
  store and schedules with `AlarmManager.setAlarmClock`; Helm uses
  AlarmKit (iOS 26). `alarm.list` returns that store. Nothing else.
- **Permission is a result, not a prompt.** Cab checks
  `canScheduleExactAlarms()` and returns `needs_permission` without
  prompting. Kit tells the human. The grant is a tap in Cab →
  Settings → Access, same row style as mic / location.

## Wire

Both directions are one JSON object on the existing WebSocket.
Additive keys are fine; a new required key is a Cab change.

Crane → phone (request):

```json
{
  "kind": "act",
  "id": "a1f3…",
  "name": "alarm.put",
  "turn": "<inbound id this answers>",
  "device": "<optional phone id override>",
  "input": { "id": "gym", "at": "07:30", "days": ["mon", "fri"], "label": "Gym", "enabled": true }
}
```

Phone → crane (answer, same `id`):

```json
{ "kind": "act", "id": "a1f3…", "ok": true, "output": { "id": "gym", "next": "2026-09-25T07:30:00-07:00" } }
```

```json
{ "kind": "act", "id": "a1f3…", "ok": false, "error": "needs_permission" }
```

| Key | Who sets | Notes |
| --- | --- | --- |
| `kind` | both | Always `act`. |
| `id` | crane | Request id. Answer echoes it. UUID or ULID. |
| `name` | crane | Closed set below. Unknown → `unsupported`. |
| `turn` | crane | The `inbound` `id` this tool call is answering. Worker resolves it to a socket and a `sub`. |
| `device` | crane | Optional. Stable phone id from connect. Overrides `turn`. |
| `input` | crane | Per `name`. ≤ 2 KB. |
| `ok` | phone / Worker | `true` with `output`, `false` with `error`. |
| `output` | phone / Worker | Per `name`. ≤ 4 KB. |
| `error` | phone / Worker | Closed set below. |

Errors (closed): `needs_permission`, `unsupported`, `bad_input`,
`offline`, `no_device`, `ambiguous`, `timeout`, `not_found`, `failed`.

- Worker emits `offline` (a named `device` is not connected),
  `no_device` (nothing on this `sub` has the cap — browser only, or
  no phone app at all), `ambiguous` (several phones have the cap and
  none was named; `output` carries `{ devices: [...] }` so Kit can
  ask), and `timeout` (no answer).
- Phone emits the rest.

### Names

Alarm inputs. `at` is `HH:MM` 24 h in the phone's zone. `days` is a
subset of `mon`…`sun`; missing / empty = one-shot at the next `at`.
`id` is caller-chosen, `[a-z0-9-]{1,32}`; reuse = update. `label` ≤ 40
chars.

| Name | Input | Output |
| --- | --- | --- |
| `alarm.list` | `{}` | `{ alarms: [{ id, at, days, label, enabled, armed, next }] }` — `armed` is false when the OS dropped it (permission revoked) |
| `alarm.put` | `{ id, at, days?, label?, enabled?, skip_next? }` | `{ id, next }` — `next` is null when `enabled: false`; `skip_next: true` drops the next fire only and leaves the rule on |
| `alarm.delete` | `{ id }` | `{ id }` — `not_found` if absent |
| `timer.list` | `{}` | `{ timers: [{ id, label, ends_at, remaining_s }] }` |
| `timer.start` | `{ id, seconds, label? }` | `{ id, ends_at }` — `seconds` 1…86400 |
| `timer.cancel` | `{ id }` | `{ id }` |
| `device.list` | `{}` | `{ devices: [{ device, kind, caps, label, live }] }` — answered by the **Worker**, no phone involved |
| `schedule.put` | `{ id, at, text, group?, wake? }` | `{ id, at }` — **Worker** (DO alarm). `at` ISO-8601 with offset, ≤ 30 days out |
| `schedule.list` | `{ group? }` | `{ schedules: [{ id, at, text, group, wake }] }` — Worker |
| `schedule.delete` | `{ id }` or `{ group }` | `{ removed: n }` — Worker |

Enable / disable is `alarm.put` with `enabled`. Disable keeps the row
and cancels the `PendingIntent`; enable schedules it again. A
weekday alarm ("7 am Mon–Fri for work") is one row: `at: "07:00"`,
`days: [mon…fri]`. Cab re-arms to the next listed day after each
ring. "Skip tomorrow" is `skip_next: true` — the row stays enabled,
`next` jumps one occurrence, and the flag clears itself after that
fire is skipped. `alarm.list` shows the skip as `next` being the day
after.

`device.list` walks the caller's `sub`. `kind` is `cab` / `helm`.
`label` is the phone's own name (`Build.MODEL`, iOS device name),
≤ 40 chars. `live` is whether the socket is up right now. Kit uses it
when the turn stamp is empty and someone asks anyway ("is my phone
connected?").

### Identity and capabilities

Each phone socket says who it is on the upgrade, additive query keys
on `GET /ws/<slug>?role=phone`:

| Key | Value | Cab |
| --- | --- | --- |
| `device` | opaque stable id, `[A-Za-z0-9_-]{8,64}` | minted once, `CabPrefs.deviceId` |
| `kind` | `cab` / `helm` | constant |
| `caps` | comma list from `alarm`, `timer` | what this build implements |
| `label` | ≤ 40 chars | `Build.MODEL` |

Worker keeps `device → { socket, sub, kind, caps, label }` for the
life of the socket. The PWA sends none of these and never receives
`act`. Spike (`MAILBOX_SECRET`) has no `sub`, so a spike phone is only
reachable by `turn` or `device`, never by the `sub` walk. Lab-only.

### The turn stamp

When the Worker forwards a phone `inbound` to the crane it adds
additive `device`, `kind`, and `caps` on that frame. The harness
renders one closed-set stamp beside `[surface]` / `[input]`:

```text
[device: cab · alarm timer]
```

No stamp when the turn came from the browser. The model reads this
before it reaches for a tool: stamp present → `alarm.put`; stamp
absent → `device.list` if it wants to be sure, else `schedule.put`.

### Routing

Worker rule for a crane `act`, in order:

1. `device` given → that socket. Not connected → `offline`.
2. `turn` socket has the cap → that socket.
3. Any live socket on the `turn`'s `sub` with the cap. Exactly one →
   that socket. Several → `ambiguous` with the list. None →
   `no_device`.

Step 3 is what makes the browser work: the PWA has no cap, so a turn
typed at a desk walks to the Cab in the pocket. Step 1 is how Kit
answers `ambiguous` ("phone or tablet?") on the next call.

## ai-gantry

- [ ] `alarm.list` / `alarm.put` / `alarm.delete` / `timer.list` /
      `timer.start` / `timer.cancel` in the tool catalog. Schemas
      above. Tool description says: sets alarms on the human's phone
      through Cab / Helm; only alarms Kit created are visible.
- [ ] Handler writes `act` on the crane socket with `turn` = the
      inbound id of the current turn. Waits for the answer frame by
      `id`. Deadline 8 s → returns `timeout` to the model.
- [ ] Map errors to model text: `needs_permission` → tell the human
      to open Cab → Settings → Access and grant alarms; `offline` →
      that phone is not reachable, open Cab and try again;
      `no_device` → no phone app is connected, use `schedule.put`;
      `ambiguous` → ask which device, then call again with `device`;
      `unsupported` → that mouth cannot do this (old APK).
- [ ] Render the turn stamp: additive `device` / `kind` / `caps` on a
      forwarded `inbound` → `[device: <kind> · <caps>]` beside
      `[surface]` / `[input]`. Closed sets; junk dropped, same as
      `harness.go` does today. No stamp when the keys are absent.
- [ ] `device.list` in the catalog. Same handler, same 8 s; the
      Worker answers, no phone round-trip.
- [ ] `schedule.put` / `schedule.list` / `schedule.delete` in the
      catalog. Same `act` handler; the **Worker** answers, no phone.
      Tool description says the difference plainly: a timer rings and
      needs Cab / Helm; a schedule is Kit speaking (or thinking) at a
      time and needs nothing. For a sequence, one `schedule.put` per
      step with the same `group`.
- [ ] Handle `tick`. The DO sends `{ kind: "tick", id, group, text }`
      on the crane socket when a `wake: true` schedule fires. Run a
      turn with `text` as the prompt and the room's last inbound
      `sub` as the human. Reply goes out as a normal `push`. No
      `tick` in the transcript as a human line.
- [ ] `act` is not a message. Do not log it in the transcript, do not
      `[stamp]` it, do not let it clear typing.
- [ ] Never say "done" before the answer frame. The tool result is
      the only source of truth.

## pendant (Worker)

- [ ] Accept `device` / `kind` / `caps` / `label` on the phone
      upgrade. Map `device → { socket, sub, kind, caps, label }`; drop
      on close. All optional; PWA omits them. Validate the shapes
      above; junk → connect as a plain phone with no caps.
- [ ] On `inbound` from a phone, remember `inbound.id → socket` for
      the turn (short TTL, e.g. 10 min, or until the next `inbound`
      from that `sub`). Stamp the forwarded frame with additive
      `device` / `kind` / `caps` when the socket has them.
- [ ] Crane `act`: route per Routing above (`device` → `turn` → `sub`
      walk). Forward the frame verbatim to that one socket. Answer
      the crane yourself with `offline` / `no_device` / `ambiguous`
      when routing fails, `ambiguous` carrying
      `output: { devices: [...] }`.
- [ ] `device.list`: answer from the map for the `turn`'s `sub`
      (spike → empty list). Never forwarded to a phone.
- [ ] `schedule.*`: rows in DO storage, one DO `alarm()` set to the
      earliest `at`. On fire: `wake: false` → emit `push` with `text`
      (Web Push + HUN as any push); `wake: true` → send `tick` to the
      crane, or fall back to the `push` with `text` if the crane
      socket is down. Then re-arm to the next row. Cap 50 rows per
      room, 30 days out. `schedule.delete` by `id` or by `group`.
- [ ] Phone `act` answer: forward to the crane socket by `id`. Drop
      if no pending request (stale / forged).
- [ ] Deadline on the Worker too: no answer in 10 s → `timeout` to
      the crane and forget the id. Crane's own 8 s wins in practice;
      this stops leaks.
- [ ] `act` never touches storage: no `seq`, no `at`, no replay, no
      sibling fan, no Web Push.
- [ ] Only `role=crane` may send a request; only `role=phone` may
      answer. Anything else is dropped. A phone can never `act`
      another phone.
- [ ] `docs/frontends.md`: one section, "Device actions (`act`)",
      with the table above and the PWA line: "ignored."

## Cab (this checkout, after pendant)

Listed here so the other two see the whole edge. Tracked in
[todo.md](todo.md) when pendant ships.

- `MailboxConnect`: `device` / `kind=cab` / `caps` / `label` query
  keys; `CabPrefs.deviceId` minted once. `caps` is what the build
  implements, so an APK that ships `alarm` before `timer` says so.
- `Wire.kt`: `kind == "act"` parses `name` / `input`; `Mouth.ingest`
  returns `false` before the bubble path.
- `mailbox/Act.kt` (Android-free): validate `input`, build the answer.
  Junk → `bad_input`.
- `drive/Alarms.kt`: store in `CabPrefs` or a small file,
  `AlarmManager.setAlarmClock`, a `BroadcastReceiver` that posts a
  full-screen alarm notification with Stop / Snooze, and
  `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` in the manifest.
  Timers post an ongoing notification while running. Re-arm on
  `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_CHANGED`, and
  `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- A Kit alarms list in Cab (toggle, delete) so the human is not
  locked out of what Kit set. Same store.
- Settings → Access: "Alarms & reminders" row when
  `canScheduleExactAlarms()` is false.
- Tests: `ActTest`, `WireTest` (drops `act` as a turn), `MouthTest`.

## Helm

Same frame, same names, `kind=helm`. AlarmKit for schedule / update /
cancel; `needs_permission` when the AlarmKit prompt has not been
accepted. Timers as a Live Activity.

## Sequences and crons

Thanksgiving: turkey out at 3:40, rolls in at 3:45, baste at 4:00,
rest at 4:20. Three different clocks could hold that. Only one
should.

| Where | Rings with screen off | Survives crane restart | PWA-only user | Kit can think at fire time |
| --- | --- | --- | --- | --- |
| Phone timer (`timer.start`) | yes | yes | no | no |
| Crane process cron | no | **no** | yes | yes |
| DO schedule (`schedule.put`) | no | yes | yes | yes (`wake`) |

Rule: **noise is the phone's, words are the mailbox's.**

- Kit writes the sequence as `schedule.put` rows with one `group`
  (`thanksgiving`). Each fires a `push` with the step text. If a step
  needs Kit to look at the thread first ("did she say the turkey was
  in yet?"), `wake: true` sends a `tick` and Kit runs a turn.
- For steps that need a ring in a loud kitchen, Kit **also** calls
  `timer.start` with the step as the label. The phone rings; the
  schedule says why. If the phone is `no_device`, the schedule alone
  still lands as a HUN / Web Push.
- Kit never narrates a timer ending on its own. The ring is the
  notice. Kit speaks only when a schedule tells it to.
- No `timer.fired` / `alarm.fired` frame back to the crane. Kit set
  `ends_at`; if it wants to act at that moment it schedules that.
  One clock per job, no reconciliation.
- "Cancel dinner" is `schedule.delete { group }` plus `timer.cancel`
  per phone timer Kit started. `schedule.list { group }` before
  changing a plan — Kit does not recite from memory.
- The DO is one `alarm()` per room, re-armed to the earliest row.
  A room that has nothing scheduled has no alarm set.

## No phone app

PWA only, a `CHANNEL` that is not pendant, or Cab installed but never
opened today (no FGS, no socket): every alarm / timer name answers
`no_device`. Kit says a ring needs Cab or Helm on the phone and uses
`schedule.put` instead. The schedule is a `push` at a time; it lands
in the thread, the PWA gets it by Web Push, a phone that has Cab gets
the HUN. It does not ring with the screen off and it does not need
a socket at fire time.

A phone with Cab installed that is not connected is still
`no_device`, because the Worker cannot tell "installed, asleep" from
"never installed". FCM would close that gap
([fcm_design_and_todo.md](fcm_design_and_todo.md)); not this beta.
Alarms that were already set keep ringing with the socket down —
only new sets need it.

## Edge cases

Each line is the failure, then the rule. Owner in brackets.

**Time**

- `at` is wall-clock in the **phone's** zone, not the crane's and not
  the browser's. "Wake me at 7" typed in a Tokyo browser lands on a
  phone in LA at 07:00 LA. `output.next` carries the offset so Kit
  can say which. [Cab / Helm]
- One-shot `at` already passed today → tomorrow. Never `bad_input`.
  [Cab / Helm]
- DST and zone change: recurring alarms recompute `next` after each
  fire and on `ACTION_TIMEZONE_CHANGED` / `ACTION_TIME_CHANGED`. The
  stored row is `HH:MM` + `days`, never an epoch. [Cab]
- `timer.start` stores `ends_at` as an epoch; a zone change does not
  move it. [Cab / Helm]

**Phone lifecycle**

- Reboot drops every `AlarmManager` alarm. `RECEIVE_BOOT_COMPLETED`
  receiver re-arms from the store. A timer whose `ends_at` passed
  during the reboot fires at once, labelled missed. [Cab]
- Permission revoked after alarms exist: Android cancels them.
  `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` re-arms on
  grant. Meanwhile `alarm.list` rows say `enabled: true, armed:
  false, next: null`; `alarm.put` answers `needs_permission`. Add
  `armed` to the list output. [Cab]
- Notifications denied: an alarm can still ring (full-screen intent
  plus a foreground alarm service), but a timer end has nowhere to
  show. `needs_permission` with `output: { missing: ["alarms",
  "notifications"] }` so Kit names both rows. [Cab]
- Alarm fires while Cab is in Android Auto: the ring is the phone's
  full-screen notification; Auto shows it as a HUN with Stop. No
  frame is sent. [Cab]
- Deleting or disabling an alarm that is ringing stops the ring.
  [Cab / Helm]
- Human edits Kit's alarm in Cab's own list (Cab needs one, with
  toggles): the store is the truth, `alarm.list` reflects it, Kit's
  memory is stale. Kit calls `alarm.list` before speaking about
  alarms; it never recites from earlier turns. [ai-gantry]

**Routing and retries**

- Answer lost, crane retried with a new `id`: `alarm.put` is
  idempotent by alarm `id` (reuse = update), `timer.start` on an
  existing `id` restarts it, `alarm.delete` / `timer.cancel` on a
  missing `id` is `not_found` and Kit treats that as done. Nothing
  double-fires. [Cab / Helm / ai-gantry]
- Answer arrives after the Worker's 10 s: dropped (no pending id).
  The alarm may still exist. On `timeout` Kit calls `alarm.list`
  before it retries or apologises. [pendant / ai-gantry]
- Phone socket redialled between request and answer: the Worker
  matches the answer by `id` and `device`, not by socket object.
  [pendant]
- Old APK (no `caps`): never a routing candidate, so the `sub` walk
  gives `no_device` rather than a silent drop and `timeout`. A
  `device` override that names a socket without the cap → Worker
  answers `unsupported` at once, no forward. [pendant]
- Two phones, same `sub`, both `alarm` (phone + tablet): `ambiguous`
  on the first call. Kit asks, then sends `device`. The crane keeps
  the choice for the session and reuses it; it is not on the wire.
  [ai-gantry]
- Browser turn, one phone → step 3 finds it. Browser turn, no phone
  → `no_device`. Phone turn, own phone → step 2. Never a fan-out to
  every phone. [pendant]
- Spike (`MAILBOX_SECRET`): no `sub`, so no step 3, `device.list` is
  empty, browser turns are `no_device`. Sign both mouths in with
  Google. [pendant]
- `turn` map is keyed by `inbound.id`, not "latest inbound per
  `sub`". Two turns in flight resolve independently. TTL 10 min.
  [pendant]
- Rate: 10 `act` per minute per room; over → `failed`. A tool loop
  cannot ring a phone into the ground. [pendant]

**Limits**

- 20 alarms and 5 timers per device. Over → `bad_input` with
  `output: { limit: 20 }`. [Cab / Helm]
- `seconds` outside 1…86400, `at` not `HH:MM`, `days` outside the
  set, `id` outside `[a-z0-9-]{1,32}`, `label` > 40 → `bad_input`.
  The phone validates; the crane schema is a courtesy. [Cab / Helm]
- `device.list` caps at 8 devices. [pendant]
- Android 12 `SCHEDULE_EXACT_ALARM` (user grant) vs 13+
  `USE_EXACT_ALARM` (auto-grant for alarm apps; Play policy only,
  sideload is fine). Declare both. `canScheduleExactAlarms()` is the
  single check. [Cab]

**Visibility**

- `act` is not fanned, so a sibling mouth never sees the alarm land;
  Kit's `reply` text is the only record in the thread. Fine. [all]
- Phone socket down and someone asks "what alarms do I have": the
  store is on the phone; `alarm.list` is `no_device`. Kit says it
  cannot see the phone right now. It does not guess. [ai-gantry]
- A fired alarm or finished timer sends **no** frame back to the
  room, and there is no `event` frame planned. Kit set `ends_at`; it
  already knows. If Kit should say something at that moment, it
  schedules that itself — see Sequences and crons. [all]

## Not this version

- Hand-off into Google Clock / iOS Clock (`ACTION_SET_ALARM`). No id
  comes back, so Kit cannot disable it later. Add only as an explicit
  `alarm.handoff` if someone asks for it in that app.
- Calendar, clipboard, dial / SMS / mail sheets, DND / Bluetooth /
  Wi-Fi. Same `act` pipe when wanted; separate names, separate
  tickets.
- `act` from the PWA. The browser has no alarm API worth using.
- Queuing `act` for a dead socket.
