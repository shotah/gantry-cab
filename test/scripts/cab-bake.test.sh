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

echo "ok cab-bake"
