#!/usr/bin/env python3
"""Import local-only HA configuration; never put credentials in ADB arguments or the repository."""
import argparse
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("serial")
parser.add_argument("configuration", type=Path)
args = parser.parse_args()
configuration = args.configuration.resolve()
data = json.loads(configuration.read_text())
if not all(key in data for key in ("url", "token", "controls")):
    parser.error("Configuration requires url, token and controls")
if configuration.stat().st_size > 65536:
    parser.error("Configuration is too large")
adb = [os.environ.get("ADB", "adb"), "-P", os.environ.get("ADB_SERVER_PORT", "5037"), "-s", args.serial]
directory = "/sdcard/Android/data/local.simple.tvoverlay/files"
remote = directory + "/home-assistant-import.json"
try:
    subprocess.run(adb + ["shell", "mkdir", "-p", directory], check=True, stdout=subprocess.DEVNULL)
    subprocess.run(adb + ["push", str(configuration), remote], check=True, stdout=subprocess.DEVNULL)
    result = subprocess.check_output(adb + ["shell", "am", "broadcast", "-n", "local.simple.tvoverlay/.SmartHomeConfigReceiver", "-a", "local.simple.tvoverlay.CONFIGURE_HOME_ASSISTANT"], text=True)
    if "result=1" not in result:
        raise SystemExit("Home Assistant import failed; confirm the current overlay APK is installed")
finally:
    subprocess.run(adb + ["shell", "rm", "-f", remote], stdout=subprocess.DEVNULL, check=False)
print("Home Assistant configured. Press HOME when convenient; no device action was sent.")
