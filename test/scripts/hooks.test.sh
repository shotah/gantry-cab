#!/usr/bin/env bash
# install-hooks must not write into the parent gantree tree.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"

if [[ ! -x "$root/scripts/pre-commit" ]]; then
  echo "FAIL: scripts/pre-commit should be executable" >&2
  exit 1
fi
grep -q "make lint" "$root/scripts/pre-commit" || {
  echo "FAIL: pre-commit should run make lint" >&2
  exit 1
}
grep -q "make test-app" "$root/scripts/pre-commit" || {
  echo "FAIL: pre-commit should run make test-app" >&2
  exit 1
}
grep -q "make coverage" "$root/scripts/pre-commit" || {
  echo "FAIL: pre-commit should run make coverage" >&2
  exit 1
}

if make -C "$root" install-hooks >/tmp/gantry-cab-hooks.out 2>/tmp/gantry-cab-hooks.err; then
  echo "FAIL: install-hooks should refuse a parent git root" >&2
  cat /tmp/gantry-cab-hooks.err >&2
  exit 1
fi
if ! grep -q "own git checkout" /tmp/gantry-cab-hooks.err /tmp/gantry-cab-hooks.out; then
  echo "FAIL: expected own-checkout error" >&2
  cat /tmp/gantry-cab-hooks.out /tmp/gantry-cab-hooks.err >&2
  exit 1
fi

echo "ok hooks"
