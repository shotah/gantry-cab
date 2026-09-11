#!/usr/bin/env bash
# Print integer JaCoCo line coverage from a report XML.
#
# Default: mailbox helpers + Mouth (JVM pendant-parity — same idea as gantree
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
  COVERAGE_CLASSES="com/gantree/cab/mailbox/WireKt com/gantree/cab/mailbox/MailboxUrlKt com/gantree/cab/mailbox/EmojiKt com/gantree/cab/mailbox/SlashKt com/gantree/cab/mailbox/PhotoKt com/gantree/cab/mailbox/JpegKt com/gantree/cab/mailbox/SendErrorKt com/gantree/cab/mailbox/LookKt com/gantree/cab/mailbox/GeoHintKt com/gantree/cab/mailbox/AvatarKt com/gantree/cab/mailbox/ThemeApiKt com/gantree/cab/mailbox/ThreadKt com/gantree/cab/Mouth"
fi

parse() {
  local parsed
  if ! parsed="$("$@")"; then
    return 1
  fi
  eval "$parsed"
}

# JaCoCo XML may be pretty-printed or a single line. Concatenate, then pick
# class-level LINE counters (not method counters) or the report total.
if [[ -z "${COVERAGE_CLASSES// /}" ]]; then
  parse awk '
    { xml = xml $0 }
    END {
      tail = xml
      while ((i = index(tail, "</package>")) > 0) {
        tail = substr(tail, i + 10)
      }
      if (!match(tail, /<counter type="LINE" missed="[0-9]+" covered="[0-9]+"/)) exit 2
      s = substr(tail, RSTART, RLENGTH)
      missed = 0
      covered = 0
      n = split(s, a, "\"")
      for (i = 1; i < n; i++) {
        if (a[i] ~ /missed=$/) missed = a[i + 1]
        if (a[i] ~ /covered=$/) covered = a[i + 1]
      }
      print "MISSED=" missed + 0
      print "COVERED=" covered + 0
    }
  ' "$REPORT" || {
    echo "could not parse total coverage from $REPORT" >&2
    exit 1
  }
else
  parse awk -v classes="$COVERAGE_CLASSES" '
    BEGIN { ncls = split(classes, cls, " ") }
    { xml = xml $0 }
    END {
      for (c = 1; c <= ncls; c++) {
        needle = "<class name=\"" cls[c] "\""
        start = index(xml, needle)
        if (start == 0) continue
        rest = substr(xml, start)
        closeat = index(rest, "</class>")
        if (closeat == 0) continue
        blob = substr(rest, 1, closeat + 7)
        while (match(blob, /<method /)) {
          rest2 = substr(blob, RSTART)
          mend = index(rest2, "</method>")
          if (mend == 0) break
          blob = substr(blob, 1, RSTART - 1) substr(rest2, mend + 9)
        }
        if (!match(blob, /<counter type="LINE" missed="[0-9]+" covered="[0-9]+"/)) continue
        s = substr(blob, RSTART, RLENGTH)
        missed = 0
        covered = 0
        n = split(s, a, "\"")
        for (i = 1; i < n; i++) {
          if (a[i] ~ /missed=$/) missed = a[i + 1]
          if (a[i] ~ /covered=$/) covered = a[i + 1]
        }
        MISSED += missed + 0
        COVERED += covered + 0
        found = 1
      }
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
