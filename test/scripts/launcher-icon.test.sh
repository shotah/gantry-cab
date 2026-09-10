#!/usr/bin/env bash
# Home-screen glyph must keep the pendant path from assets/logo.svg.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
logo="$root/assets/logo.svg"
fg="$root/app/src/main/res/drawable/ic_launcher_foreground.xml"

body=$(grep -oE 'd="M20 18h24v28a12 12 0 0 1-24 0V18z"' "$logo") || {
  echo "FAIL: assets/logo.svg is missing the pendant body path" >&2
  exit 1
}
path=${body#d=\"}
path=${path%\"}

grep -Fq "$path" "$fg" || {
  echo "FAIL: ic_launcher_foreground.xml must copy the pendant path from assets/logo.svg" >&2
  exit 1
}

grep -q 'fillColor="#F3B199"' "$fg" || {
  echo "FAIL: launcher foreground should keep the logo highlight" >&2
  exit 1
}

echo "ok launcher-icon"
