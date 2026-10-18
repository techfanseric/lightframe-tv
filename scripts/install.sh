#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SERIAL=${1:?Usage: install.sh ADB_SERIAL [--set-home]}
ADB=${ADB:-adb}
PORT=${ADB_SERVER_PORT:-5037}
"$ADB" -P "$PORT" -s "$SERIAL" install -r "$ROOT/dist/LightframeHome.apk"
"$ADB" -P "$PORT" -s "$SERIAL" install -r "$ROOT/dist/LightframeOverlay.apk"
"$ADB" -P "$PORT" -s "$SERIAL" shell appops set local.simple.tvoverlay SYSTEM_ALERT_WINDOW allow
"$ADB" -P "$PORT" -s "$SERIAL" shell appops set local.simple.tvoverlay GET_USAGE_STATS allow
if [ "${2:-}" = --set-home ]; then
 "$ADB" -P "$PORT" -s "$SERIAL" shell cmd package set-home-activity local.simple.tvhome/.MainActivity
fi
printf '%s\n' 'Installed. Open HOME with your remote when ready.'
