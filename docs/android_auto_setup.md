# Android Auto: after you install Cab

What to do on the phone, in the Android Auto app, and in Cab so that
Kit is read aloud in the car and your spoken reply goes back to the
crane. Do §1–§3 once at your desk, §4 in the driveway, then drive.

Install first: [sideload_to_android.md](sideload_to_android.md).

## 0. What "working" looks like

- **There is no Cab tile in the car.** Do not scroll Auto's app list
  for it. Google only admits a sideloaded app into Android Auto as
  *message notifications*; the Unknown sources switch below "doesn't
  apply to apps built using the Android for Cars App Library"
  ([Test Android apps for cars](https://developer.android.com/training/cars/testing#unknown-sources)).
  Cab's in-dash screen only appears on the Desktop Head Unit or a
  Play internal-testing install.
- What you get is what Messages and WhatsApp get: Kit's reply pops
  up as a **card over maps or music**, the car chimes, Auto reads it
  ("New message from kit: …") and offers **Reply**. You speak; Auto's
  voice typing turns it into text; Cab sends it to the crane as
  `inbound` tagged `surface: android_auto`. The crane's answer comes
  back the same way.
- The same card sits in the phone's notification shade. Swiping it
  away there is fine; Cab forgets those turns so the car does not
  re-read them later.
- Kit only reaches the car after you **open Cab** (Connect, or send
  a line) so the mailbox socket is up, then **lock the phone**. Auto
  cannot start Cab for you. There is no always-on "Listening for
  Kit" card — the shade should only show Kit's message, not a second
  status bubble. Reboot? Open Cab again before you drive.

## 1. Phone (Android settings)

1. **Notifications for Cab: on.** Settings → Apps → Cab →
   Notifications → allow. Open the **Kit** category and make sure it
   is *Urgent* / *pop on screen* (Android names vary). Auto only
   heads-up — and only reads — high-importance cards. Cab creates the
   category that way; this is a check that nobody lowered it.
2. **Battery: unrestricted.** Settings → Apps → Cab → Battery →
   *Unrestricted*. Samsung: Device care → Battery → Background usage
   limits → take Cab out of *Sleeping apps*, add it to *Never sleeping
   apps*. Xiaomi / OnePlus / Oppo: Autostart on, battery saver *No
   restrictions*. The OEM killer is the usual reason the socket is
   dead by the time you get to the car.
3. **Text-to-speech engine: Google.** Settings → search *Text-to-speech*
   → Preferred engine → *Speech Services by Google*. Tap Play to hear
   it. A stale or third-party engine is the classic "card shows, no
   voice" fault, and it hits Messages too.
4. **Android Auto has notification access.** Settings → Notifications
   → *Device & app notifications* (or *Notification access*) →
   Android Auto → allowed. Set during Auto's first run; revoked by
   some privacy dashboards.
5. Leave Cab alone once it is Live. Do not *Force stop* it. Swiping it
   out of Recents is fine on Pixel; on some OEMs that also kills the
   socket, so if in doubt just lock the phone.

## 2. Android Auto (the app on the phone)

Open it from Settings → Connected devices → Connection preferences →
**Android Auto** (newer Androids hide the launcher icon).

1. **Developer mode.** Scroll to *Version* and tap it **10 times**
   until "Allow development settings?" appears → OK.
2. **⋮ → Developer settings → Unknown sources → on.** Without this,
   Auto drops every notification from a non-Play app with no error
   anywhere. If the toggle is missing, Google moved or removed it in
   your Auto version; Cab will work on the phone but not in the car
   until it is back.
3. **Notifications → Show message notifications: on.** *Show first
   line of conversations* is your call (shows Kit's words on the
   dash instead of just "New message"). *Group notifications*: leave
   default.
4. On the head unit, make sure Auto's own **Do not disturb** (the bell
   in the notification center) is off. It mutes exactly these cards.

## 3. Cab

1. Settings (gear): **Mailbox** = the pendant Worker `https://…`,
   **Talking to** = the crane's room (`kit`), then *Continue with
   Google* or paste the phone secret.
2. **Connect** (or send a line), then wait for the bar to say **Live**.
3. Lock the phone. The shade should show Kit's **message** card when
   something arrives — not a second "Listening for Kit" bubble.
4. Settings → **Test car voice**. Expect a *kit* card in the shade
   that starts "Car check from Cab". If Android settings opened
   instead, Cab found notifications off or the Kit category lowered —
   fix it there and tap again. This step proves notification
   permission and the Kit channel without a car.

## 4. Driveway test (two minutes, engine on)

1. Plug the phone into the USB port Subaru marks for data (front
   console; the rear-seat ports are charge-only). Wait for Auto to
   come up on the head unit — maps or the launcher, either is fine.
2. Phone: Cab → Settings → **Test car voice**.
3. You should get, in this order: a chime from the car speakers, a
   **kit** card on the head unit, Auto reading *"Car check from Cab.
   Android Auto is attached. If you hear this, Kit will be read
   aloud."*, then "Do you want to reply?". Say **yes** and say
   anything — that text lands in the Cab thread as your bubble and
   goes to the crane. If Kit's crane is up, its answer comes back as
   the next card.
4. If the voice says *"The phone does not see Android Auto"* but the
   car still read it, you are fine: the sentence reports Cab's
   `CarConnection` probe, the reading comes from Auto's notification
   listener, and they are independent. Replies just get tagged
   `android` instead of `android_auto`.
5. Now type something to Kit on the phone and lock it. The reply
   should arrive as a card and be read aloud. That is the whole loop.

## 5. On the road

- Every **new** Kit reply and push is read. A push with no text is
  read as "ping"; a photo as "Photo". Opening Cab or reconnecting
  paints the last 80 turns (`replay: true`) without a chime — those
  are history, not a new mouth.
- **Reply**: tap the card → Reply → speak, or answer Auto's "Do you
  want to reply?" Hands off the phone.
- **Mark as read** on the card (or tapping it) clears it and Cab's
  history for that card. Next reply starts clean.
- If the socket drops (tunnel, dead zone), replies you speak are
  queued (up to 50) and sent when Live returns; the phone bubble says
  *sending* until then.
- Open Cab (or send a line) before you plug in. Reboot / APK update /
  Force stop: open Cab again, then drive. The socket does not come
  back by itself.
- Do not drive with Cab open on the phone screen. The thread is the
  mouth then, and if Cab cannot see the head unit it skips the card.
  Lock the phone.

## 6. When it does not

| What you see | Why | Do |
| --- | --- | --- |
| Test car voice opens Android settings | Notifications for Cab are off, or the Kit category is below *Urgent* | Allow / raise it there, tap Test again |
| Test card shows on the phone, nothing in the car | Unknown sources off in Auto's developer settings; or Auto's notification access revoked; or Auto's DND on | §2.2, §1.4, §2.4 |
| Card on the head unit, no voice | Text-to-speech engine, or car "Voice" volume at zero | §1.3; turn the knob while it is talking |
| Card read, reply never reaches Kit | Cab not Live; reply is queued | Phone bubble says *sending*; comes through when Live. If it never does, Connect again |
| Worked yesterday, silent today | Reboot, Force stop, or OEM battery killer — Cab does not relisten on boot | §1.2; open Cab, send a line, then drive |
| Kit answers on the phone but the car never speaks | Cab was open on the phone screen | Lock the phone |
| Auto re-reads old turns | Older APK (ignores `replay`) on a mailbox that hydrates | Update Cab; this APK paints last 80 on connect but does not chime. Swiping a live card still clears history |
| Looking for a Cab tile in Auto | Expected | There is none for a sideload. See §0 |

## 7. If you really want the tile

Play Console → internal testing track → upload the APK → add your
Google account as a tester → install from Play. Then Auto lists Cab
and opening it shows the conversation screen (Car API 7
`ConversationItem`: Play, Mark as read, Reply). Same APK, no code
change. The messaging category is Play-beta only, so internal /
closed testing is as far as it goes. Not a goal for this repo.

## 8. Updating Cab

Each GitHub Release APK may be signed with a fresh debug key unless
`CAB_KEYSTORE_*` is set, so Android refuses the update over the old
one: **uninstall Cab first**, install the new APK, sign in again. A
local `make apk` is signed with this machine's debug key — same rule.

```bash
adb uninstall com.gantree.cab
adb install app/build/outputs/apk/release/app-release.apk
```

Then §3 again; §1–§2 stick.
