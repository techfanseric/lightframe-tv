#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
"$ROOT/scripts/build-app.sh" home
"$ROOT/scripts/build-app.sh" overlay
