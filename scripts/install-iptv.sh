#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SERIAL=${1:?Usage: install-iptv.sh ADB_SERIAL [--launch-and-configure]}
MODE=${2:-}
case "$MODE" in ''|--launch-and-configure) ;; *) echo 'Unknown option' >&2; exit 2;; esac
ADB=${ADB:-adb}
PORT=${ADB_SERVER_PORT:-5037}
for file in "$ROOT/dist/LightframeHome.apk" "$ROOT/dist/LightframeOverlay.apk" "$ROOT/.local/iptv/MyTV-2.2.7.apk" "$ROOT/.local/iptv/sources.json"; do
 [ -f "$file" ] || { echo "Missing prepared file: $file" >&2; exit 1; }
done
"$ROOT/scripts/install.sh" "$SERIAL"
"$ADB" -P "$PORT" -s "$SERIAL" install -r "$ROOT/.local/iptv/MyTV-2.2.7.apk"
if [ "$MODE" = --launch-and-configure ]; then
 python3 "$ROOT/scripts/configure-iptv.py" "$SERIAL" --launch
else
 printf '%s\n' 'Installed without opening apps. Open IPTV when ready, then run:' "python3 $ROOT/scripts/configure-iptv.py $SERIAL"
fi
