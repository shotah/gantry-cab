# Sibling phones (live inbound)

Canonical design lives in the mailbox checkout:

[`gantry-pendant/docs/sibling_phones.md`](../../gantry-pendant/docs/sibling_phones.md)

This page is what that means for Cab. It is **not** a new mailbox, a
group chat, or a Cab-side poll. The Durable Object already stores Ada's
turns under her Google `sub`. The Worker fans a live `inbound` to her
other open sockets (`siblingPhoneTag` → `sub:<userId>` except the
sender). Walk both mouths on a deployed origin (same Google). That
walk is still open.

## Why the browser seemed to "just work"

The PWA drops its socket when the tab hides and redials when it is
visible. A connect flush is how the DO hands out `t:<sub>`. Cab's
`MailboxService` holds one socket for hours, so it never re-reads that
store. The bytes were on the DO the whole time.

## What Cab already does (keep it)

| Piece | Why it stays |
| --- | --- |
| `inbound` paints as you (`Mouth.ingest`) | A sibling frame is the same kind as a hydrate frame |
| Dedup by `id`; `pending` waits for `ack` | The sender's own echo, and a sibling copy, share an id |
| `shouldSpeak` skips `inbound` | No Auto HUN / toast for your other mouth |
| `MailboxClient.sweep` | Quiet second dial, no down state. Frozen / Doze sockets, and spike (no `sub`), still need a connect flush |

After the Worker fans inbound to `sub:<userId>`, the sweep is
Doze insurance, not how browser turns arrive. Do not rip it out.

## What Cab does not do

- Do not flash Offline or clear the thread to "sync".
- Do not broadcast Cab inbound to every phone in the room.
- Do not treat spike (`MAILBOX_SECRET`) as the same human as Google.
  Sibling live-delivery is per `sub`. Sign both mouths in with Google.
  Spike Cab never sees PWA personal rows; PWA may still see Cab via
  broadcast `t:_`.

## Walk

Still the open box on both ledgers. Google on Cab and the PWA, both
sockets up, deployed origin:

1. Type in the browser — Cab inserts it as "you" without "Connecting…",
   without clearing the thread, in seq order. Type in Cab — the
   browser does the same without a tab hide.
2. Bob in the same room still does not see Ada's inbound. Kit's
   `reply` to Ada still only hits Ada's sockets.
3. An old APK already knows how to paint extra `inbound`. Worst case
   it restamps an id it sent. No new required field.
4. Cab sweep still runs. After the fan it is mostly "socket looks up
   but is frozen"; it is not how sibling turns arrive.
