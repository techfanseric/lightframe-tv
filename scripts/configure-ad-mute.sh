#!/bin/sh
set -eu
SERIAL=${1:?Usage: configure-ad-mute.sh ADB_SERIAL TOKEN_FILE}
TOKEN_FILE=${2:?Supply the narrow-capability token file; never a router admin credential}
ADB=${ADB:-adb}
PORT=${ADB_SERVER_PORT:-5037}
[ -f "$TOKEN_FILE" ]
TARGET=/sdcard/Android/data/local.simple.tvoverlay/files
"$ADB" -P "$PORT" -s "$SERIAL" shell mkdir -p "$TARGET"
"$ADB" -P "$PORT" -s "$SERIAL" push "$TOKEN_FILE" "$TARGET/ad-mute-token"
printf '%s\n' 'Provisioned. The next YouTube menu request imports it into private storage and removes the staging file.'
