#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
badge="$root/scripts/coverage-badge.sh"
pct="$root/scripts/jacoco-pct.sh"
gate="$root/scripts/coverage-gate.sh"
fix90="$root/test/scripts/fixtures/jacoco-line-90.xml"
fix70="$root/test/scripts/fixtures/jacoco-line-70.xml"
fix69="$root/test/scripts/fixtures/jacoco-line-69.xml"
out="$(mktemp)"
trap 'rm -f "$out"' EXIT

# Default classes: WireKt 18/20 + MailboxUrlKt 9/10 = 27/30 → 90%
assert_eq() {
  local got="$1" want="$2" msg="$3"
  if [[ "$got" != "$want" ]]; then
    echo "FAIL: got ${got@Q} want ${want@Q} ${msg}" >&2
    exit 1
  fi
}

assert_eq "$("$pct" "$fix90")" "90" "scoped wire 90%"
assert_eq "$(COVERAGE_CLASSES= "$pct" "$fix90")" "14" "report total 33/233"
assert_eq "$("$pct" "$fix70")" "70" "exactly 70%"
assert_eq "$("$pct" "$fix69")" "69" "69%"

"$badge" "$fix90" "$out"
grep -q 'aria-label="coverage: 90%"' "$out" || {
  echo "FAIL: badge label" >&2
  cat "$out" >&2
  exit 1
}
grep -q 'fill="#4c1"' "$out" || {
  echo "FAIL: expected green band for 90%" >&2
  exit 1
}

"$gate" "$fix90" >/dev/null
"$gate" "$fix70" >/dev/null
if "$gate" "$fix69" >/dev/null 2>&1; then
  echo "FAIL: 69% should miss the 70% bar" >&2
  exit 1
fi
if COVERAGE_MIN=91 "$gate" "$fix90" >/dev/null 2>&1; then
  echo "FAIL: 90% should miss a 91% bar" >&2
  exit 1
fi

if "$badge" /tmp/gantry-cab-missing-jacoco.xml "$out" >/dev/null 2>&1; then
  echo "FAIL: missing report should error" >&2
  exit 1
fi

echo "ok coverage-badge"
