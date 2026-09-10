#!/usr/bin/env bash
# Fail if JaCoCo line coverage is below COVERAGE_MIN (default 70).
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
REPORT="${1:-app/build/reports/coverage/test/debug/report.xml}"
MIN="${COVERAGE_MIN:-70}"

if [[ ! "$MIN" =~ ^[0-9]+$ ]]; then
  echo "COVERAGE_MIN must be an integer, got ${MIN@Q}" >&2
  exit 1
fi

PCT="$("$root/scripts/jacoco-pct.sh" "$REPORT")"
if [[ "$PCT" -lt "$MIN" ]]; then
  echo "coverage ${PCT}% is below the ${MIN}% bar" >&2
  exit 1
fi
echo "coverage ${PCT}% (bar ${MIN}%)"
