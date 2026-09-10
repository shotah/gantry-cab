#!/usr/bin/env bash
# Paint phone + Auto sample PNGs into assets/docs (Java2D — LayoutLib smashes type).
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
CAB_WRITE_SHOTS=1 ./gradlew :app:testDebugUnitTest --tests com.gantree.cab.ui.DocsShotTest --rerun

copied=0
for name in phone-unsigned phone-empty phone-thread phone-down phone-settings phone-emoji phone-attach auto-empty auto-thread; do
  png="$root/assets/docs/${name}.png"
  if [[ ! -f "$png" ]]; then
    echo "missing $png" >&2
    exit 1
  fi
  echo "assets/docs/${name}.png"
  copied=$((copied + 1))
done
echo "wrote ${copied} shots → assets/docs/"
