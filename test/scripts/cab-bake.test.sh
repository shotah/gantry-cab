#!/usr/bin/env bash
# Real mailbox / OAuth values stay out of git. Bake them at assemble time.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"

grep -qx '.env' "$root/.gitignore" || {
  echo "FAIL: .gitignore must ignore .env" >&2
  exit 1
}

test -f "$root/.env.example" || {
  echo "FAIL: missing .env.example" >&2
  exit 1
}
grep -q 'CAB_MAILBOX_ORIGIN=' "$root/.env.example" || {
  echo "FAIL: .env.example should set CAB_MAILBOX_ORIGIN" >&2
  exit 1
}
grep -q 'CAB_GOOGLE_WEB_CLIENT_ID=' "$root/.env.example" || {
  echo "FAIL: .env.example should set CAB_GOOGLE_WEB_CLIENT_ID" >&2
  exit 1
}

grep -q 'CAB_MAILBOX_ORIGIN' "$root/.github/workflows/release.yml" || {
  echo "FAIL: release.yml should pass CAB_MAILBOX_ORIGIN" >&2
  exit 1
}
grep -q 'CAB_GOOGLE_WEB_CLIENT_ID' "$root/.github/workflows/release.yml" || {
  echo "FAIL: release.yml should pass CAB_GOOGLE_WEB_CLIENT_ID" >&2
  exit 1
}
grep -q 'CAB_MAILBOX_ORIGIN' "$root/app/build.gradle.kts" || {
  echo "FAIL: build.gradle.kts should read CAB_MAILBOX_ORIGIN" >&2
  exit 1
}

if grep -rI --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git \
  --exclude=local.properties --exclude=.env --exclude=cab-bake.test.sh \
  -n 'bldhosting' "$root"
then
  echo "FAIL: personal mailbox host must not be in the public tree" >&2
  exit 1
fi

grep -q ':app:signingReport' "$root/Makefile" || {
  echo "FAIL: Makefile should run :app:signingReport" >&2
  exit 1
}
grep -q 'apk-cert' "$root/Makefile" || {
  echo "FAIL: Makefile should have apk-cert" >&2
  exit 1
}
grep -q 'apk-sha1.sh' "$root/.github/workflows/release.yml" || {
  echo "FAIL: release.yml should publish the signing-cert SHA-1" >&2
  exit 1
}
grep -q 'apksigner' "$root/scripts/apk-sha1.sh" || {
  echo "FAIL: apk-sha1.sh should use apksigner (v2/v3 APKs)" >&2
  exit 1
}
grep -q 'sha1.txt' "$root/.github/workflows/release.yml" || {
  echo "FAIL: release.yml should attach a sha1.txt asset" >&2
  exit 1
}

grep -q 'in 0..99' "$root/app/build.gradle.kts" || {
  echo "FAIL: versionCode must reject minor/patch >= 100" >&2
  exit 1
}
if grep -q 'ANDROID_GOOGLE_WEB_CLIENT_ID' "$root/.env.example"; then
  echo "FAIL: .env.example should not set ANDROID_GOOGLE_WEB_CLIENT_ID" >&2
  exit 1
fi

grep -q 'Optional stable keystore' "$root/.github/workflows/release.yml" || {
  echo "FAIL: release.yml should keep GitHub Releases installable without a Play keystore" >&2
  exit 1
}
grep -q 'persist-credentials: false' "$root/.github/workflows/release.yml" || {
  echo "FAIL: release.yml checkout should not persist credentials" >&2
  exit 1
}
grep -q 'contents: read' "$root/.github/workflows/ci.yml" || {
  echo "FAIL: ci.yml default token should be contents: read" >&2
  exit 1
}

car="$root/app/src/main/java/com/gantree/cab/drive/CabCarAppService.kt"
grep -q 'BuildConfig.DEV' "$car" || {
  echo "FAIL: HostValidator must be gated on BuildConfig.DEV" >&2
  exit 1
}
grep -q 'hosts_allowlist_sample' "$car" || {
  echo "FAIL: release HostValidator must use hosts_allowlist_sample" >&2
  exit 1
}

debug_ns="$root/app/src/debug/res/xml/network_security_config.xml"
if grep -q 'base-config cleartextTrafficPermitted="true"' "$debug_ns"; then
  echo "FAIL: debug cleartext must not be allowed for all hosts" >&2
  exit 1
fi
grep -q '10.0.2.2' "$debug_ns" || {
  echo "FAIL: debug cleartext should stay on the emulator loopback" >&2
  exit 1
}

if grep -RIn --include='*.kt' 'android.util.Log' "$root/app/src/main"; then
  echo "FAIL: do not log from app/src/main" >&2
  exit 1
fi

if grep -rI --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git \
  --exclude=local.properties --exclude=.env --exclude=cab-bake.test.sh \
  -nE '[0-9]+-[a-z0-9]+\.apps\.googleusercontent\.com' "$root"
then
  echo "FAIL: Google client id must not be in the public tree" >&2
  exit 1
fi

echo "ok cab-bake"
