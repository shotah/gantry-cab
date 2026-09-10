# Iteration improvements

Why the loop feels slow, with numbers from this machine (24 cores, 89 GB,
JDK 21, Gradle 9.6, AGP 9.4), and what to change. Ordered by payoff per
hour of work. Fixes to app behaviour are in [todo_fixes.md](todo_fixes.md).

## Where the time actually goes

Measured 2026-09-10 with `./gradlew --profile` and `--rerun`:

| Run | Wall | Notes |
| --- | --- | --- |
| `testDebugUnitTest`, cold daemon | **57.8 s** | Configuring Projects **18.2 s**, `compileDebugKotlin` **20.7 s** (full, after IC cache corruption), `testDebugUnitTest` 4.0 s |
| same, warm daemon, no change | 1.4 s | |
| same, warm, `--configuration-cache` reuse | 0.5 s | |
| `compileDebugKotlin --rerun` (warm, full) | 3.3 s | 3.7 k lines of Kotlin |
| `testDebugUnitTest --rerun` (warm) | 1.8 s | 94 tests, < 1 s of test time |
| `lintAnalyzeDebug --rerun` (warm) | 6.3 s | |
| `lintDebug --rerun-tasks` (compile + analyze + report) | 10.0 s | |

The tests are not the problem. The cost is (1) Gradle configuration on a
cold daemon, (2) Kotlin recompiles that go full instead of incremental,
(3) Android lint, and (4) paying (1) three times per commit because the
pre-commit hook runs three separate Gradle invocations.

## 1. `gradle.properties` — ten minutes, biggest win

Current file: `-Xmx2048m`, nothing else. On an 89 GB box.

```properties
org.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true
org.gradle.parallel=true
kotlin.daemon.jvmargs=-Xmx4g
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [x] `org.gradle.configuration-cache=true` — configuration drops from 18 s
  (cold) to ~0 on every later run, including after a daemon restart, because
  the cache is on disk. `build.gradle.kts` reads `VERSION`, `.env` and
  `local.properties` via `rootProject.file(...)`, which Gradle tracks as
  inputs, so edits to those still invalidate correctly. Run once with
  `--configuration-cache-problems=warn` to confirm no plugin complains.
- [x] `org.gradle.caching=true` — local build cache. Lint analysis, Kotlin
  compile and resource tasks become cache hits when you switch branches or
  revert a change. Also lets CI restore outputs (see §6).
- [ ] Bigger heaps. Left at `-Xmx2048m` in-repo so GitHub runners (~7 GB)
  do not OOM. Raise in `~/.gradle/gradle.properties` on a large local box
  (`org.gradle.jvmargs=-Xmx6g`, `kotlin.daemon.jvmargs=-Xmx4g`).

## 2. Pre-commit hook — stop paying for lint and coverage on every commit

`scripts/pre-commit` runs `make test-scripts` and `make test-app`. Lint and
coverage live in `scripts/pre-push` (`make check-app`, one Gradle invocation).
`release.test.sh` sets `SKIP_FETCH=1` so it does not `git fetch --tags`.

- [x] Pre-commit = `make test-scripts` + `./gradlew testDebugUnitTest`.
  Move `lintDebug` + coverage bar to a **pre-push** hook or CI only.
- [x] When lint and coverage do run, run them in **one** invocation:
  `./gradlew lintDebug testDebugUnitTest createDebugUnitTestCoverageReport`
  (one configuration, shared task graph). Update `make check` and `ci.yml`
  the same way.
- [x] `release.test.sh`: honour a `SKIP_FETCH=1` env in `release.sh` so the
  test does not touch the network.
- [x] Update `test/scripts/hooks.test.sh` (it greps the hook for `make lint`
  and `make coverage`) when the hook changes.

## 3. Make targets for the inner loop

`make` has build/test/lint/coverage but nothing for "edit, see it".

- [x] `make watch` → `./gradlew -t testDebugUnitTest --tests 'com.gantree.cab.mailbox.*'`
  (Gradle continuous build; reruns on save; ~2 s per cycle on a warm
  daemon).
- [x] `make test-fast TESTS=com.gantree.cab.MouthTest` → `--tests` passthrough.
- [x] `make run` → `./gradlew installDebug && adb shell am start -n com.gantree.cab/.MainActivity`.
- [x] `make run-sample S=thread` → same plus `--es sample thread` (already
  supported by `MainActivity` in debug).
- [x] `make dhu` → launch the Desktop Head Unit against the running emulator
  (`$SDK_DIR/extras/google/auto/desktop-head-unit`).
- [x] `make logs` → `adb logcat --pid=$(adb shell pidof com.gantree.cab)`.

## 4. Split the pure-JVM code into a `:mailbox` module

Everything the 70 % bar covers is already Android-free: `Wire`, `MailboxUrl`,
`MailboxConnect`, `Emoji`, `Slash`, `Photo`, `Jpeg`, `Look`, `GeoHint`,
`Avatar` (URL + rev), `GoogleHint`, and `Mouth`/`ChatLine`. `AuthApi`,
`AvatarApi`, `MailboxClient` are OkHttp-only and can move too.

- [ ] `include(":mailbox")` with `org.jetbrains.kotlin.jvm` (match AGP's
  built-in Kotlin, currently 2.4.20). `./gradlew :mailbox:test` then skips
  AGP entirely: no `android.jar`, no resource merge, no `BuildConfig`, no
  SDK needed — the loop for the code you touch most becomes ~2 s cold.
- [ ] `org.json`: the module should `compileOnly("org.json:json")` and
  `testImplementation` it; on device the platform class wins. Or replace
  with `kotlinx.serialization` and drop the dependency.
- [ ] Point `scripts/jacoco-pct.sh` `COVERAGE_CLASSES` / `COVERAGE_XML` at
  `mailbox/build/reports/jacoco/test/jacocoTestReport.xml` (apply the
  `jacoco` plugin there). Add `MailboxClient` to the gated class list — it
  has tests and is not counted today.
- [ ] Studio: the `:app` module stays thin (Activity, services, Compose,
  ViewModel, prefs).

## 5. Trim what gets compiled and dexed

- [ ] `androidx.compose.material:material-icons-extended` is pulled in for
  five icons (`AddLocationAlt`, `AttachFile`, `Code`, `Photo`,
  `SentimentSatisfied`; the other four used are in `material-icons-core`,
  which `material3` already brings). It is the single largest thing in the
  36.8 MB debug APK and in every dex merge. Export those five as vector
  drawables (Studio → Vector Asset → Material icon) and drop the dependency.
- [ ] Remove `androidx.fragment:fragment-ktx` (unused). **Blocked:** lint
  `InvalidFragmentVersionForActivityResult` requires Fragment ≥ 1.3.0 for
  `registerForActivityResult` even with no Fragment usage. Leave it.
- [ ] Release: `isMinifyEnabled = true`, `isShrinkResources = true`. Faster
  `adb install` of the 51 MB APK, and see the security doc. Debug stays
  unminified.

## 6. CI — one invocation, cancel stale runs, cache

`ci.yml` runs `lintDebug` and `testDebugUnitTest createDebugUnitTestCoverageReport`
as two `--no-daemon` invocations (two cold JVMs, two configurations) and
never cancels superseded runs.

- [x] Single step: `./gradlew lintDebug testDebugUnitTest createDebugUnitTestCoverageReport --build-cache --configuration-cache`.
- [x] `concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }`.
- [x] `gradle/actions/setup-gradle@v4` with
  `cache-read-only: ${{ github.ref != 'refs/heads/main' }}` so PRs read the
  main branch's dependency + build cache and only main writes it.
- [x] `paths-ignore: ['docs/**', 'assets/**', '*.md']` for the check job.
- [x] Add `.github/dependabot.yml` for `gradle` and `github-actions`
  (weekly). The lint report already lists seven stale libraries.

## 7. Replace the hand-painted screenshots

`app/src/test/java/com/gantree/cab/ui/DocsShot.kt` re-implements the UI in
Java2D so `assets/docs/*.png` can be produced without an emulator. It is 500
lines that must be edited alongside every Compose change, and
`DocsShotTest` asserts pixel colours at fixed coordinates.

- [ ] Use Compose Preview Screenshot Testing (`com.android.compose.screenshot`)
  or Roborazzi against the `@Preview`s already in `ShotScenes.kt`
  (`PhoneShot`, `AutoShot`). The "LayoutLib smashes type" issue that pushed
  the project to Java2D is what `Type.kt` (bundled Noto Sans) already solves.
- [ ] Turn `make shot` into a Gradle task (`updateScreenshots`) instead of an
  env-gated test (`CAB_WRITE_SHOTS=1`) that writes into `assets/docs`.
- [ ] Keep `DocsShotTest` only for "renders without throwing" until the
  replacement lands; delete the coordinate assertions.

## 8. Test the Android side without a device

`MailboxService`, `CabViewModel`, `CabPrefs`, `CabNotifier` (only
`conversationId`), `ReplyService`, `CabCarAppService` have no tests, and
most of the P0/P1 bugs in `todo_fixes.md` live there.

- [ ] Add Robolectric (`testImplementation("org.robolectric:robolectric")`,
  `testOptions.unitTests.isIncludeAndroidResources = true`) for the
  service/ViewModel/prefs, or
- [ ] extract the decisions into pure functions in `:mailbox` (`retryDelay`,
  `shouldReconnect(sameTarget)`, outbox flush order, notification history
  model) and test those. Cheaper, matches the existing style.
- [ ] Tag slow/IO tests (MockWebServer, Java2D) with a JUnit `Category` so
  `make watch` can exclude them.

## 9. Style automation

There is no formatter or Kotlin linter; only Android lint. The code is
2-space indented while `kotlin.code.style=official` implies 4.

- [ ] Add `.editorconfig` (`indent_size = 2`, `max_line_length = 120`,
  `ktlint_code_style = intellij_idea`) and the `org.jlleitschuh.gradle.ktlint`
  plugin (or detekt with formatting). Run it in the pre-commit hook — it is
  sub-second on a warm daemon — and drop `kotlin.code.style=official` or
  align to it.

## 10. Workflow hygiene

- [ ] Don't run Studio "Sync/Make" and the CLI/pre-commit in the same
  `app/build` at the same time; that is what corrupted the Kotlin
  incremental cache today and produced the 20 s full recompile. If it
  happens again: `rm -rf app/build/kotlin` is enough (no `clean`).
- [ ] Keep the daemon alive: never `--no-daemon` locally (the Makefile is
  fine). `org.gradle.daemon.idletimeout=10800000` (3 h) in
  `~/.gradle/gradle.properties` if Studio keeps killing it.
- [ ] Use Studio **Live Edit** for Compose tweaks and the `@Preview`s in
  `ShotScenes.kt` before doing a full install.
- [ ] `make ensure-sdk` writes `local.properties` with a mailbox origin; keep
  secrets in `.env` only (already the documented precedence).

## Order of operations

1. ~~§1 `gradle.properties` (10 min) and §2 hook split (20 min).~~
2. ~~§3 `make watch` / `make run` (15 min).~~
3. §5 drop icons-extended. fragment-ktx stays (lint Activity Result). Minify still open.
4. ~~§6 CI single invocation + concurrency + Dependabot (30 min).~~
5. §4 `:mailbox` module (half a day; unlocks §8).
6. §7 screenshots (day; after the UI settles).
