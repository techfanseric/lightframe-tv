"""Installer tests use an in-memory ADB model; never touch a device."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("lightframe_setup", Path(__file__).resolve().parents[1] / "scripts/setup.py")
setup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(setup)


class FakeADB:
    def __init__(self):
        self.calls = []
        self.states = {setup.HOME: 0, setup.OVERLAY: 0, setup.OLD[0]: 0, setup.OLD[1]: 1}
        self.home = setup.OLD[0] + "/.MainActivity"
        self.fail_disable = False

    def run(self, *args):
        self.calls.append(args)
        if args == ("get-state",):
            return "device"
        return "Success"

    def shell(self, *args):
        self.calls.append(args)
        if args[:2] == ("dumpsys", "package"):
            value = self.states.get(args[2])
            return "Unable to find package" if value is None else "User 0: installed=true enabled=%s\n" % value
        if args[:3] == ("cmd", "package", "resolve-activity"):
            return self.home
        if args[:3] == ("cmd", "package", "set-home-activity"):
            self.home = args[-1]
            return "Success"
        if args[:1] == ("pm",):
            value = {"default-state": 0, "enable": 1, "disable": 2, "disable-user": 3, "disable-until-used": 4}[args[1]]
            if self.fail_disable and args[-1] == setup.OLD[1] and value == 3:
                self.fail_disable = False
                raise setup.SetupError("simulated vendor failure")
            self.states[args[-1]] = value
            return "new state"
        if args[:3] == ("cmd", "appops", "get"):
            return args[-1] + ": allow"
        if args[:1] == ("getprop",):
            return {"ro.build.version.sdk": "28", "ro.serialno": "TEST-ONLY", "ro.build.fingerprint": "test/fingerprint", "ro.product.cpu.abilist": "armeabi-v7a,armeabi", "ro.product.model": "Mock TV"}[args[1]]
        if args == ("am", "get-current-user"):
            return "0"
        return ""


class InstallerTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.adb = FakeADB()
        self.device = {"abis": ["armeabi-v7a"], "identity": "test-hash", "stable_identity": True}

    def tearDown(self):
        self.tmp.cleanup()

    def bundle(self, files):
        lines = []
        for name in files:
            value = ("APK:" + name).encode()
            (self.root / name).write_bytes(value)
            lines.append(hashlib.sha256(value).hexdigest() + "  " + name)
        (self.root / "SHA256SUMS").write_text("\n".join(lines))

    def test_abi_selection(self):
        self.assertEqual(setup.arm_variant(["armeabi-v7a", "armeabi"]), "arm")
        self.assertEqual(setup.arm_variant(["arm64-v8a", "armeabi-v7a"]), "arm64")
        with self.assertRaises(setup.SetupError):
            setup.arm_variant(["x86_64"])
        arm = dict(setup.companion_files(["all"], ["armeabi-v7a"]))
        self.assertEqual(arm["local.simple.streamguardobserver"], "PlaybackGuardian-2.11.apk")
        self.assertEqual(arm["com.follow.clash"], "FlClash-0.9.2-armeabi-v7a.apk")
        self.assertEqual(arm["io.gh.reisxd.tizentube.cobalt"], "TizenTube-2.0.3-arm.apk")

    def test_existing_companion_never_overwritten(self):
        self.adb.states["io.gh.reisxd.tizentube.cobalt"] = 0
        self.bundle([f for _, f in setup.REQUIRED])
        setup.install(self.adb, self.device, self.root, ["tizentube"])
        installed = [c for c in self.adb.calls if c[0] == "install"]
        self.assertEqual(len(installed), 2)
        self.assertFalse(any("TizenTube" in str(c) for c in installed))
        self.assertFalse(any(c[:3] == ("cmd", "package", "set-home-activity") for c in self.adb.calls))

    def test_hash_failure_before_first_install(self):
        self.bundle([f for _, f in setup.REQUIRED])
        (self.root / setup.REQUIRED[1][1]).write_bytes(b"corrupted")
        with self.assertRaisesRegex(setup.SetupError, "Checksum mismatch"):
            setup.install(self.adb, self.device, self.root, [])
        self.assertFalse(any(c[0] == "install" for c in self.adb.calls))

    def test_missing_optional_checksum_before_first_install(self):
        self.bundle([f for _, f in setup.REQUIRED])
        with self.assertRaises(setup.SetupError):
            setup.install(self.adb, self.device, self.root, ["mytv"])
        self.assertFalse(any(c[0] == "install" for c in self.adb.calls))

    def test_activation_and_rollback_preserve_accounts_and_old_states(self):
        original = self.adb.home
        state = self.root / "recovery.json"
        setup.activate(self.adb, self.device, state)
        self.assertEqual(self.adb.home, setup.COMPONENT)
        self.assertEqual([self.adb.states[p] for p in setup.OLD], [3, 3])
        setup.rollback(self.adb, self.device, state)
        self.assertEqual(self.adb.home, original)
        self.assertEqual([self.adb.states[p] for p in setup.OLD], [0, 1])
        self.assertEqual([self.adb.states[p] for p in (setup.HOME, setup.OVERLAY)], [3, 3])
        commands = "\n".join(str(c) for c in self.adb.calls)
        for forbidden in ("uninstall", "clear", "google", "netflix", "settings", "reboot"):
            self.assertNotIn(forbidden, commands)

    def test_partial_activation_automatically_recovers(self):
        original = self.adb.home
        self.adb.fail_disable = True
        with self.assertRaises(setup.SetupError):
            setup.activate(self.adb, self.device, self.root / "state.json")
        self.assertEqual(self.adb.home, original)
        self.assertEqual([self.adb.states[p] for p in setup.OLD], [0, 1])

    def test_state_wrong_device_rejected_before_writes(self):
        state = self.root / "state.json"
        setup.activate(self.adb, self.device, state)
        self.adb.calls.clear()
        other = dict(self.device, identity="other")
        with self.assertRaises(setup.SetupError):
            setup.rollback(self.adb, other, state)
        self.assertEqual(self.adb.calls, [])

    def test_manual_home_change_not_overwritten_on_rollback(self):
        state = self.root / "state.json"
        setup.activate(self.adb, self.device, state)
        self.adb.home = "new.choice/.Home"
        with self.assertRaisesRegex(setup.SetupError, "Default HOME changed"):
            setup.rollback(self.adb, self.device, state)
        self.assertEqual(self.adb.home, "new.choice/.Home")

    def test_doctor_read_only_and_serial_required(self):
        with patch.object(setup, "ADB", return_value=self.adb):
            self.assertEqual(setup.main(["--serial", "EXPLICIT-TEST"]), 0)
        self.assertFalse(any(c[0] in ("install", "pm", "am") and c != ("am", "get-current-user") for c in self.adb.calls))
        with self.assertRaises(SystemExit):
            setup.main([])

    def test_adb_exit_and_android_text_errors(self):
        for code, out in [(1, "device offline"), (0, "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]"),
                          (0, "Error: permission denied"), (0, "Exception occurred while executing"),
                          (0, "Permission Denial: requires signature")]:
            result = subprocess.CompletedProcess([], code, stdout=out, stderr="")
            with self.subTest(output=out), patch.object(setup.subprocess, "run", return_value=result):
                with self.assertRaises(setup.SetupError):
                    setup.ADB("TEST").run("install", "test.apk")

    def test_activation_requires_overlay_and_usage_permissions(self):
        original = self.adb.shell
        def denied(*args):
            if args[:3] == ("cmd", "appops", "get"):
                return args[-1] + ": deny"
            return original(*args)
        self.adb.shell = denied
        with self.assertRaisesRegex(setup.SetupError, "Permission verification failed"):
            setup.activate(self.adb, self.device, self.root / "state.json")
        self.assertFalse(any(c[:3] == ("cmd", "package", "set-home-activity") for c in self.adb.calls))
        self.assertFalse((self.root / "state.json").exists())

    def test_inactive_install_does_not_mutate_packages_or_components(self):
        original_states = dict(self.adb.states)
        original_home = self.adb.home
        self.bundle([f for _, f in setup.REQUIRED])
        setup.install(self.adb, self.device, self.root, [])
        self.assertFalse(any(c[0] == "pm" for c in self.adb.calls))
        self.assertFalse(any(c[:2] == ("am", "force-stop") for c in self.adb.calls))
        self.assertEqual(self.adb.states, original_states)
        self.assertEqual(self.adb.home, original_home)

    def test_active_upgrade_does_not_mutate_component_state(self):
        self.adb.home = setup.COMPONENT
        self.bundle([f for _, f in setup.REQUIRED])
        setup.install(self.adb, self.device, self.root, [])
        self.assertFalse(any(c[0] == "pm" for c in self.adb.calls))
        self.assertEqual(self.adb.home, setup.COMPONENT)

    def test_package_state_ignores_preferred_activity_user_headers(self):
        dump = """Preferred Activities User 0:
  User 0:
    preferred HOME component
Packages:
  Package [local.simple.tvhome.dock]:
    User 0: ceDataInode=123 installed=true hidden=false stopped=false enabled=0
"""
        with patch.object(self.adb, "shell", return_value=dump):
            self.assertEqual(setup.package_state(self.adb, setup.HOME), 0)
        with patch.object(self.adb, "shell", return_value=dump.replace("installed=true", "installed=false")):
            self.assertIsNone(setup.package_state(self.adb, setup.HOME))

    def test_remote_shell_arguments_quoted(self):
        result = subprocess.CompletedProcess([], 0, stdout="ok", stderr="")
        with patch.object(setup.subprocess, "run", return_value=result) as call:
            setup.ADB("TEST").shell("echo", "component/$literal")
            self.assertIn("'component/$literal'", call.call_args[0][0][-1])


if __name__ == "__main__":
    unittest.main()
