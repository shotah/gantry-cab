#!/usr/bin/env bash
# Print integer JaCoCo line coverage from a report XML.
#
# Default: WireKt + MailboxUrlKt (JVM mailbox wire — same idea as gantree
# gating lib/yard). Empty COVERAGE_CLASSES uses the report-total LINE counter.
set -euo pipefail

REPORT="${1:-}"
if [[ -z "$REPORT" || ! -f "$REPORT" ]]; then
  echo "missing ${REPORT:-report.xml}" >&2
  exit 1
fi

# Default scoped classes. Override: COVERAGE_CLASSES=  (report total)
# or COVERAGE_CLASSES="com/gantree/cab/mailbox/AuthApi".
if [[ ! -v COVERAGE_CLASSES ]]; then
  COVERAGE_CLASSES="com/gantree/cab/mailbox/WireKt com/gantree/cab/mailbox/MailboxUrlKt"
fi

parse() {
  local parsed
  if ! parsed="$("$@")"; then
    return 1
  fi
  eval "$parsed"
}

if [[ -z "${COVERAGE_CLASSES// /}" ]]; then
  parse awk '
    /type="LINE"/ {
      missed = 0
      covered = 0
      n = split($0, a, "\"")
      for (i = 1; i < n; i++) {
        if (a[i] ~ /missed=$/) missed = a[i + 1]
        if (a[i] ~ /covered=$/) covered = a[i + 1]
      }
    }
    END {
      if (covered == "" && missed == "") exit 2
      print "MISSED=" missed + 0
      print "COVERED=" covered + 0
    }
  ' "$REPORT" || {
    echo "could not parse total coverage from $REPORT" >&2
    exit 1
  }
else
  parse awk -v classes="$COVERAGE_CLASSES" '
    BEGIN {
      ncls = split(classes, cls, " ")
    }
    /<class / {
      keep = 0
      in_method = 0
      current = ""
      if (match($0, /<class name="[^"]+"/)) {
        s = substr($0, RSTART, RLENGTH)
        split(s, a, "\"")
        current = a[2]
      }
      for (i = 1; i <= ncls; i++) {
        if (current == cls[i]) keep = 1
      }
    }
    /<method / { in_method = 1 }
    /<\/method>/ { in_method = 0 }
    keep && !in_method && /type="LINE"/ {
      missed = 0
      covered = 0
      n = split($0, a, "\"")
      for (i = 1; i < n; i++) {
        if (a[i] ~ /missed=$/) missed = a[i + 1]
        if (a[i] ~ /covered=$/) covered = a[i + 1]
      }
      MISSED += missed + 0
      COVERED += covered + 0
      found = 1
    }
    END {
      if (!found) exit 2
      print "MISSED=" MISSED + 0
      print "COVERED=" COVERED + 0
    }
  ' "$REPORT" || {
    echo "could not parse class coverage (${COVERAGE_CLASSES}) from $REPORT" >&2
    exit 1
  }
fi

DENOM=$((MISSED + COVERED))
if [[ "$DENOM" -eq 0 ]]; then
  echo 0
  exit 0
fi
awk -v c="$COVERED" -v d="$DENOM" 'BEGIN{printf "%.0f\n", (c * 100) / d}'
