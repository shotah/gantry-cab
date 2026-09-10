#!/usr/bin/env bash
# Signing-cert SHA-1 of an APK (Google Android OAuth).
# Not the SHA-256 checksum GitHub prints next to a Release asset.
#
# Release APKs are v2/v3-signed. keytool -printcert -jarfile only sees
# JAR/v1 and prints "Not a signed jar file". Use apksigner.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"

die() {
  echo "Error: $*" >&2
  exit 1
}

usage() {
  echo "usage: apk-sha1.sh <apk> [out.txt]" >&2
  echo "       apk-sha1.sh --parse          # stdin: apksigner --print-certs / keytool -printcert" >&2
  echo "       apk-sha1.sh --notes <sha1> ephemeral|stable" >&2
  exit 2
}

normalize_sha1() {
  local hex="${1//:/}"
  hex="${hex//[$' \t\r\n']/}"
  hex="$(printf '%s' "$hex" | tr '[:lower:]' '[:upper:]')"
  [[ "$hex" =~ ^[0-9A-F]{40}$ ]] || die "not a SHA-1 fingerprint: ${1@Q}"
  local i out=""
  for ((i = 0; i < 40; i += 2)); do
    out+="${hex:i:2}:"
  done
  printf '%s\n' "${out%:}"
}

extract_from_printcert() {
  local line
  line="$(awk '
    $1 == "SHA1:" { print $2; exit }
    $1 == "SHA-1:" { print $2; exit }
    /SHA-1 digest:/ {
      sub(/.*SHA-1 digest:[[:space:]]*/, "")
      print
      exit
    }
  ')"
  [[ -n "$line" ]] || die "no SHA-1 in cert dump"
  normalize_sha1 "$line"
}

find_apksigner() {
  local sdk cand
  if command -v apksigner >/dev/null 2>&1; then
    command -v apksigner
    return 0
  fi
  sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -z "$sdk" && -f "$root/local.properties" ]]; then
    sdk="$(grep '^sdk\.dir=' "$root/local.properties" | head -n1 | cut -d= -f2-)"
  fi
  if [[ -z "$sdk" && -d "${HOME}/Android/Sdk" ]]; then
    sdk="${HOME}/Android/Sdk"
  fi
  [[ -n "$sdk" && -d "$sdk/build-tools" ]] || return 1
  cand="$(find "$sdk/build-tools" -maxdepth 2 -name apksigner -type f | sort -V | tail -n1)"
  [[ -n "$cand" ]] || return 1
  printf '%s\n' "$cand"
}

dump_certs() {
  local apk="$1"
  local signer
  signer="$(find_apksigner)" || die "need Android SDK build-tools apksigner (this APK is v2/v3-signed; keytool only reads JAR/v1 and will say 'Not a signed jar file')"
  "$signer" verify --print-certs "$apk"
}

notes() {
  local sha1="$1"
  local kind="$2"
  local stability
  case "$kind" in
    stable)
      stability='This APK is signed with `CAB_KEYSTORE_*`. The fingerprint stays the same across releases — paste it once.'
      ;;
    ephemeral)
      stability='This APK is signed with the CI debug key. That fingerprint **changes every GitHub Release**. Copy this release’s value, or set `CAB_KEYSTORE_*` so Google only needs one SHA-1.'
      ;;
    *)
      die "notes kind must be ephemeral or stable, got ${kind@Q}"
      ;;
  esac
  cat <<EOF
## Signing certificate SHA-1

Paste into Google Cloud → Credentials → Android OAuth client, package \`com.gantree.cab\`:

\`\`\`
${sha1}
\`\`\`

This is the **signing certificate** fingerprint. The SHA-256 GitHub prints next to the APK is a checksum of the file — ignore it for OAuth.

${stability}

Local \`make signing-report\` is this laptop’s debug key. It only matches a GitHub APK if both builds used the same keystore.
EOF
}

cmd="${1:-}"
case "$cmd" in
  ""|-h|--help)
    usage
    ;;
  --parse)
    extract_from_printcert
    ;;
  --notes)
    [[ "${2:-}" =~ ^[0-9A-Fa-f:]{40,59}$ ]] || die "usage: apk-sha1.sh --notes <sha1> ephemeral|stable"
    notes "$(normalize_sha1 "$2")" "${3:-}"
    ;;
  *)
    apk="$1"
    out="${2:-}"
    [[ -f "$apk" ]] || die "missing $apk"
    sha1="$(dump_certs "$apk" | extract_from_printcert)"
    printf '%s\n' "$sha1"
    if [[ -n "$out" ]]; then
      mkdir -p "$(dirname "$out")"
      printf '%s\n' "$sha1" >"$out"
    fi
    ;;
esac
