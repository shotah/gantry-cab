#!/usr/bin/env bash
# Cert SHA-1 parser + release notes (no APK / SDK).
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
sha1="$root/scripts/apk-sha1.sh"
# Built from octets so a colon MAC is not rewritten into a URL.
want="$(printf '%s:' DE AD BE EF CA FE BA BE 01 23 45 67 89 AB CD EF 12 34 56 78)"
want="${want%:}"
digest="$(printf '%s' "$want" | tr -d ':' | tr '[:upper:]' '[:lower:]')"

got="$("$sha1" --parse <<EOF
Owner: CN=Android Debug, O=Android, C=US
Issuer: CN=Android Debug, O=Android, C=US
Certificate fingerprints:
	 SHA1: ${want}
	 SHA256: FF:EE:DD:CC:BB:AA:99:88:77:66:55:44:33:22:11:00:FF:EE:DD:CC:BB:AA:99:88:77:66:55:44:33:22:11:00
EOF
)"
[[ "$got" == "$want" ]] || {
  echo "FAIL: keytool SHA1 parse got ${got@Q}" >&2
  exit 1
}

got="$("$sha1" --parse <<EOF
Signer #1 certificate DN: CN=Android Debug, O=Android, C=US
Signer #1 certificate SHA-256 digest: ffeeddccbbaa99887766554433221100ffeeddccbbaa99887766554433221100
Signer #1 certificate SHA-1 digest: ${digest}
Signer #1 certificate MD5 digest: 00112233445566778899aabbccddeeff
EOF
)"
[[ "$got" == "$want" ]] || {
  echo "FAIL: apksigner SHA-1 parse got ${got@Q}" >&2
  exit 1
}

if "$sha1" --parse >/dev/null 2>&1 <<'EOF'
Certificate fingerprints:
	 SHA256: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99
EOF
then
  echo "FAIL: parser must not treat SHA-256 as SHA-1" >&2
  exit 1
fi

notes="$("$sha1" --notes "$want" ephemeral)"
echo "$notes" | grep -q "$want" || {
  echo "FAIL: notes should include the SHA-1" >&2
  exit 1
}
echo "$notes" | grep -q 'changes every GitHub Release' || {
  echo "FAIL: ephemeral notes should say the fingerprint changes" >&2
  exit 1
}
echo "$notes" | grep -q 'checksum of the file' || {
  echo "FAIL: notes should distinguish file SHA-256 from cert SHA-1" >&2
  exit 1
}

stable="$("$sha1" --notes "$want" stable)"
echo "$stable" | grep -q 'CAB_KEYSTORE_\*' || {
  echo "FAIL: stable notes should mention CAB_KEYSTORE_*" >&2
  exit 1
}
echo "$stable" | grep -q 'paste it once' || {
  echo "FAIL: stable notes should say paste once" >&2
  exit 1
}

grep -q 'verify --print-certs' "$sha1" || {
  echo "FAIL: apk-sha1.sh should use apksigner, not keytool -jarfile" >&2
  exit 1
}
grep -q 'Not a signed jar file' "$sha1" || {
  echo "FAIL: apk-sha1.sh should explain the keytool v1 failure" >&2
  exit 1
}

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat >"$tmp/apksigner" <<EOF
#!/bin/sh
cat <<CERT
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256 digest: ffeeddccbbaa99887766554433221100ffeeddccbbaa99887766554433221100
Signer #1 certificate SHA-1 digest: ${digest}
CERT
EOF
chmod +x "$tmp/apksigner"
touch "$tmp/fake.apk"
got="$(PATH="$tmp:$PATH" "$sha1" "$tmp/fake.apk")"
[[ "$got" == "$want" ]] || {
  echo "FAIL: apksigner extract got ${got@Q}" >&2
  exit 1
}

echo "ok apk-sha1"
