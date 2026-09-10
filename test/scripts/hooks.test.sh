#!/usr/bin/env bash
# install-hooks writes this checkout's hook, and must not write a parent tree.
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

top="$(git -C "$root" rev-parse --show-toplevel 2>/dev/null || true)"
if [[ "$top" == "$root" ]]; then
  echo "ok hooks (own git)"
  exit 0
fi

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
