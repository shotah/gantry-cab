# Sibling phones (live inbound)

Canonical design lives in the mailbox checkout:

[`gantry-pendant/docs/sibling_phones.md`](../../gantry-pendant/docs/sibling_phones.md)

This page is what that means for Cab. It is **not** a new mailbox, a
group chat, or a Cab-side poll. The Durable Object already stores Ada's
turns under her Google `sub`. The hole is live delivery to her other
open sockets.

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
Doze insurance, not how browser turns arrive. Do not rip it out in
the same change.

## What Cab does not do

- Do not flash Offline or clear the thread to "sync".
- Do not broadcast Cab inbound to every phone in the room.
- Do not treat spike (`MAILBOX_SECRET`) as the same human as Google.
  Sibling live-delivery is per `sub`. Sign both mouths in with Google.

## Walk when the Worker ships

Google on Cab and the PWA, both sockets up. Type in the browser — Cab
inserts it as "you" without "Connecting…". Type in Cab — the browser
does the same without a tab hide. Bob in the same room still does not
see Ada's inbound.
