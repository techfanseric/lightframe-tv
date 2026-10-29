#!/usr/bin/env python3
"""Lightframe installer: explicit device, verified files, separate activation and rollback.

No third-party account data, Google-service state, VPN policy or accessibility
settings are changed. Requires Python 3.8+ and Android platform-tools on PATH.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys

HOME = "local.simple.tvhome.dock"
OVERLAY = "local.simple.tvoverlay.dock"
COMPONENT = HOME + "/.MainActivity"
OLD = ("local.simple.tvhome", "local.simple.tvoverlay")
REQUIRED = ((HOME, "LightframeHome-Parallel.apk"),
            (OVERLAY, "LightframeOverlay-Parallel.apk"))
COMPANIONS = ("tizentube", "flclash", "mytv", "guardian")
ERROR = re.compile(r"(?im)^\s*(?:error(?:\s+type\s+\d+)?\s*:|failure\b|failed\b|securityexception\b|permission denial\b|exception occurred\b|java\.[\w.]*exception\b)")


class SetupError(Exception):
    pass


class ADB:
    def __init__(self, serial, executable="adb"):
        self.serial, self.executable = serial, executable

    def run(self, *args):
        try:
            result = subprocess.run([self.executable, "-s", self.serial, *args],
                                    capture_output=True, text=True, timeout=180)
        except (OSError, subprocess.TimeoutExpired) as exc:
            raise SetupError("ADB unavailable or timed out: " + str(exc)) from exc
        output = (result.stdout + "\n" + result.stderr).strip()
        if result.returncode or ERROR.search(output):
            raise SetupError("ADB command failed; no app data will be cleared.\n" + output[:1500])
        return result.stdout.strip()

    def shell(self, *args):
        # adb shell joins arguments on the remote shell; quote even system-returned components.
        return self.run("shell", shlex.join(str(arg) for arg in args))


def last_line(value):
    return next((line.strip() for line in reversed(value.splitlines()) if line.strip()), "")


def inspect_device(adb):
    if last_line(adb.run("get-state")) != "device":
        raise SetupError("Device is not ready. Connect/pair this exact TV and accept its ADB prompt.")
    sdk = last_line(adb.shell("getprop", "ro.build.version.sdk"))
    if not sdk.isdigit() or int(sdk) < 28:
        raise SetupError("Lightframe requires Android 9 / API 28 or newer; detected API " + sdk)
    if last_line(adb.shell("am", "get-current-user")) != "0":
        raise SetupError("Only the primary TV user (0) is supported. Switch to it first.")
    serial = last_line(adb.shell("getprop", "ro.serialno"))
    fingerprint = last_line(adb.shell("getprop", "ro.build.fingerprint"))
    abis = last_line(adb.shell("getprop", "ro.product.cpu.abilist"))
    if not abis:
        abis = last_line(adb.shell("getprop", "ro.product.cpu.abi"))
    return {"sdk": int(sdk), "abis": abis.split(","),
            "model": last_line(adb.shell("getprop", "ro.product.model")),
            "identity": hashlib.sha256((serial + "\n" + fingerprint).encode()).hexdigest(),
            "stable_identity": bool(serial and serial.lower() not in ("unknown", "null"))}


def arm_variant(abis):
    if "arm64-v8a" in abis:
        return "arm64"
    if "armeabi-v7a" in abis:
        return "arm"
    raise SetupError("Bundled TizenTube/FlClash APKs require ARM32 or ARM64. Core launcher is architecture-independent.")


def companion_files(names, abis):
    requested = set(COMPANIONS if "all" in names else names)
    result = []
    if "tizentube" in requested:
        result.append(("io.gh.reisxd.tizentube.cobalt", "TizenTube-2.0.3-" + arm_variant(abis) + ".apk"))
    if "flclash" in requested:
        arch = "arm64-v8a" if arm_variant(abis) == "arm64" else "armeabi-v7a"
        result.append(("com.follow.clash", "FlClash-0.9.2-" + arch + ".apk"))
    if "mytv" in requested:
        result.append(("top.yogiczy.mytv.tv", "MyTV-2.2.7.apk"))
    if "guardian" in requested:
        result.append(("local.simple.streamguardobserver", "PlaybackGuardian-2.10.apk"))
    return result


def package_state(adb, package):
    value = adb.shell("dumpsys", "package", package)
    # Other dumpsys sections (e.g. preferred activities) also contain "User 0:".
    # Select the package-user line itself; never let whitespace consume a newline.
    user_lines = re.findall(r"(?m)^[ \t]*User 0:[ \t]*([^\r\n]*\binstalled=(?:true|false)\b[^\r\n]*)$", value)
    if not user_lines:
        return None
    installed_lines = [line for line in user_lines if re.search(r"\binstalled=true\b", line)]
    if not installed_lines:
        return None
    enabled = re.search(r"\benabled=(\d+)\b", installed_lines[-1])
    if not enabled:
        raise SetupError("Cannot safely determine enabled state for " + package)
    return int(enabled.group(1))


def verify_files(bundle, names):
    try:
        lines = (bundle / "SHA256SUMS").read_text(encoding="utf-8").splitlines()
    except OSError as exc:
        raise SetupError("Missing SHA256SUMS in bundle directory") from exc
    hashes = {}
    for line in lines:
        if not line.strip():
            continue
        match = re.fullmatch(r"([a-fA-F0-9]{64})\s+\*?(.+)", line)
        if not match or match.group(2) in hashes:
            raise SetupError("Invalid or duplicate SHA256SUMS entry")
        hashes[match.group(2)] = match.group(1).lower()
    for name in names:
        path = bundle / name
        if name not in hashes or not path.is_file():
            raise SetupError("Missing release file or checksum: " + name)
        digest = hashlib.sha256()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        if digest.hexdigest() != hashes[name]:
            raise SetupError("Checksum mismatch; installation stopped before any APK install: " + name)


def verify_special_access(adb):
    for operation in ("SYSTEM_ALERT_WINDOW", "GET_USAGE_STATS"):
        value = adb.shell("cmd", "appops", "get", "--user", "0", OVERLAY, operation)
        if not re.search(r"\b" + operation + r":\s*allow\b", value):
            raise SetupError("Permission verification failed: " + operation + ". Enable it in TV special app access settings before activation.")


def install(adb, device, bundle, companions):
    selected = list(REQUIRED)
    for package, filename in companion_files(companions, device["abis"]):
        if package_state(adb, package) is not None:
            print("Keeping existing third-party app:", package)
        else:
            selected.append((package, filename))
    verify_files(bundle, [filename for _, filename in selected])
    for package, filename in selected:
        print("Installing", filename)
        adb.run("install", "--user", "0", "-r", str((bundle / filename).resolve()))
    # Android enforces update signatures; never fall back to uninstall or downgrade.
    for operation in ("SYSTEM_ALERT_WINDOW", "GET_USAGE_STATS"):
        adb.shell("cmd", "appops", "set", "--user", "0", OVERLAY, operation, "allow")
    verify_special_access(adb)
    for package in (HOME, OVERLAY):
        adb.shell("dumpsys", "deviceidle", "whitelist", "+" + package)
    print("Installed. Default HOME unchanged. Configure VPN/HA separately, then run activate when ready.")
    if "guardian" in companions or "all" in companions:
        print("Guardian is optional: review its scope before manually enabling its accessibility service.")


def current_home(adb):
    output = adb.shell("cmd", "package", "resolve-activity", "--brief", "--user", "0",
                       "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
    components = re.findall(r"(?m)^([\w.]+/[\w.$]+)\s*$", output)
    if not components or "ResolverActivity" in components[-1]:
        raise SetupError("No unique default HOME. Choose a default launcher on the TV first.")
    return components[-1]


def is_home(component, package):
    return component.split("/", 1)[0] == package


def save_state(path, state):
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_suffix(path.suffix + ".tmp")
    # Restrict local recovery records even though they contain no account credentials.
    fd = os.open(str(temp), os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as stream:
        json.dump(state, stream, ensure_ascii=False, indent=2)
    os.replace(str(temp), str(path))


def load_state(path, device):
    try:
        state = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        raise SetupError("Cannot read recovery state: " + str(path)) from exc
    if state.get("format") != 1 or state.get("device") != device["identity"]:
        raise SetupError("Recovery state belongs to a different device or format")
    home = state.get("original_home", "")
    if not re.fullmatch(r"[\w.]+/[\w.$]+", home) or is_home(home, HOME):
        raise SetupError("Invalid original HOME in recovery state")
    if set(state.get("old_states", {})) != set(OLD):
        raise SetupError("Invalid old-package recovery states")
    if any(value not in (None, 0, 1, 2, 3, 4) for value in state["old_states"].values()):
        raise SetupError("Unknown package enabled state")
    return state


def set_enabled(adb, package, value):
    command = {0: "default-state", 1: "enable", 2: "disable", 3: "disable-user", 4: "disable-until-used"}[value]
    adb.shell("pm", command, "--user", "0", package)
    if package_state(adb, package) != value:
        raise SetupError("Cannot verify package enabled state: " + package)


def rollback(adb, device, path):
    state = load_state(path, device)
    active = current_home(adb)
    if not is_home(active, HOME) and active != state["original_home"]:
        raise SetupError("Default HOME changed since installation; refusing to overwrite your new choice.")
    # Restore old apps before handing HOME back; keep all their data.
    for package, value in state["old_states"].items():
        if value is not None:
            set_enabled(adb, package, value)
    adb.shell("cmd", "package", "set-home-activity", "--user", "0", state["original_home"])
    if current_home(adb) != state["original_home"]:
        raise SetupError("Original HOME verification failed; new pair left enabled")
    for package in (HOME, OVERLAY):
        adb.shell("am", "force-stop", "--user", "0", package)
        set_enabled(adb, package, 3)
    state["stage"] = "rolled_back"
    save_state(path, state)
    print("Original HOME restored; new pair disabled with its data retained. No accounts or third-party services changed.")


def activate(adb, device, path):
    if not device["stable_identity"]:
        raise SetupError("TV does not expose a stable serial; automatic activation/rollback is unavailable. Use system default-app settings manually.")
    for package, _ in REQUIRED:
        if package_state(adb, package) is None:
            raise SetupError("Install both Lightframe packages first")
    verify_special_access(adb)
    if path.exists():
        state = load_state(path, device)
        if current_home(adb) not in (COMPONENT, HOME + "/" + HOME + ".MainActivity", state["original_home"]):
            raise SetupError("HOME changed since recovery record; refusing to replace it")
    else:
        original = current_home(adb)
        if is_home(original, HOME):
            raise SetupError("Already active but no recovery record exists. Restore/select your previous HOME before creating a record.")
        state = {"format": 1, "device": device["identity"], "original_home": original,
                 "old_states": {package: package_state(adb, package) for package in OLD}, "stage": "prepared"}
        save_state(path, state)
    try:
        for package in (HOME, OVERLAY):
            set_enabled(adb, package, 1)
        adb.shell("cmd", "package", "set-home-activity", "--user", "0", COMPONENT)
        if not is_home(current_home(adb), HOME):
            raise SetupError("New default HOME was not applied")
        for package in OLD:
            if state["old_states"][package] is not None:
                adb.shell("am", "force-stop", "--user", "0", package)
                set_enabled(adb, package, 3)
        state["stage"] = "active"
        save_state(path, state)
    except SetupError as exc:
        try:
            rollback(adb, device, path)
        except SetupError as recovery:
            raise SetupError(str(exc) + "\nAutomatic recovery incomplete: " + str(recovery) + "\nKeep recovery file: " + str(path)) from exc
        raise
    print("New HOME selected. Press HOME on the remote when ready. Recovery file:", path)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", nargs="?", default="doctor", choices=("doctor", "install", "activate", "rollback"))
    parser.add_argument("--serial", required=True, help="Exact adb devices serial, e.g. TV_IP:5555; never auto-selects")
    parser.add_argument("--adb", default=os.environ.get("ADB", "adb"))
    parser.add_argument("--bundle", type=Path, default=Path(__file__).resolve().parents[1] / "dist")
    parser.add_argument("--companions", nargs="+", choices=COMPANIONS + ("all",), default=[], help="Install missing optional apps only")
    parser.add_argument("--state", type=Path, help="Recovery JSON; retain this file for rollback")
    args = parser.parse_args(argv)
    if args.companions and args.action != "install":
        parser.error("--companions is only valid with install")
    state = args.state or Path.home() / ".lightframe" / ("recovery-" + hashlib.sha256(args.serial.encode()).hexdigest()[:16] + ".json")
    try:
        adb = ADB(args.serial, args.adb)
        device = inspect_device(adb)
        if args.action == "doctor":
            print("Connected:", device["model"], "API", device["sdk"], "ABI", ", ".join(device["abis"]))
            print("Default HOME:", current_home(adb))
            for package in (HOME, OVERLAY) + OLD:
                print(package, "enabled state:", package_state(adb, package))
            print("Read-only checks complete. Firmware may still restrict overlay/default-HOME behavior.")
        elif args.action == "install":
            install(adb, device, args.bundle, args.companions)
        elif args.action == "activate":
            activate(adb, device, state)
        else:
            rollback(adb, device, state)
    except SetupError as exc:
        print("STOP:", exc, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
