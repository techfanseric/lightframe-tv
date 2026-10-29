#!/bin/sh
# Compatibility wrapper: the Python installer owns safety checks and recovery.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SERIAL=${1:?Usage: install.sh ADB_SERIAL [--set-home]}
case "${2:-}" in ''|--set-home) ;; *) echo 'Unknown option' >&2; exit 2;; esac
python3 "$ROOT/scripts/setup.py" install --serial "$SERIAL" --bundle "$ROOT/dist"
if [ "${2:-}" = --set-home ]; then
 python3 "$ROOT/scripts/setup.py" activate --serial "$SERIAL" --bundle "$ROOT/dist"
fi
