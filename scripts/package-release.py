#!/usr/bin/env python3
"""Package signed release assets with shared checksums and explicit source provenance."""
import argparse
import hashlib
import json
import pathlib
import re
import subprocess
import zipfile

from setup import REQUIRED, companion_files


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--assets", type=pathlib.Path, required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"v\d+\.\d+\.\d+", args.version):
        parser.error("Use a version such as v0.2.2")
    root = pathlib.Path(__file__).resolve().parents[1]
    assets = args.assets.resolve()
    core = [name for _, name in REQUIRED]
    companions = sorted({name for abi in ("armeabi-v7a", "arm64-v8a")
                         for _, name in companion_files(["all"], [abi])})
    apks = core + companions
    records = json.loads((assets / "PROVENANCE.json").read_text())
    hashes = {name: digest(assets / name) for name in apks}
    for record in records:
        if record["file"] not in hashes or hashes[record["file"]] != record["sha256"]:
            raise SystemExit("Upstream provenance mismatch: " + record["file"])
    if not (assets / "third-party-sources.zip").is_file():
        raise SystemExit("Missing third-party source/license bundle")
    licenses = sorted((assets / "licenses").glob("*.txt"))
    if len(licenses) < 3:
        raise SystemExit("Missing companion licenses")
    (assets / "SHA256SUMS").write_text("".join(
        hashes[name] + "  " + name + "\n" for name in apks))
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    source_status = subprocess.check_output(["git", "status", "--porcelain"], cwd=root, text=True)
    if source_status:
        raise SystemExit("Commit reviewed source before packaging the release")
    own = []
    for label, name, manifest in [
        ("home", core[0], "apps/home/AndroidManifest.xml"),
        ("overlay", core[1], "apps/overlay/AndroidManifest.xml"),
        ("guardian", next(n for n in companions if n.startswith("PlaybackGuardian-")),
         "companion/playback-guardian/AndroidManifest.xml"),
    ]:
        xml = (root / manifest).read_text()
        package = re.search(r'package="([^"]+)"', xml).group(1)
        own.append({"component": label, "file": name, "sha256": hashes[name],
                    "package": package + (".dock" if label != "guardian" else ""),
                    "versionName": re.search(r'versionName="([^"]+)"', xml).group(1),
                    "versionCode": int(re.search(r'versionCode="(\d+)"', xml).group(1))})
    build_info = {"release": args.version, "sourceCommit": commit, "components": own,
                  "source": "https://github.com/techfanseric/lightframe-tv/tree/" + commit}
    (assets / "BUILD-INFO.json").write_text(json.dumps(build_info, indent=2) + "\n")
    shared = ["SHA256SUMS", "PROVENANCE.json", "BUILD-INFO.json"]

    def bundle(name, files):
        with zipfile.ZipFile(assets / name, "w", zipfile.ZIP_DEFLATED) as archive:
            for path, member in files:
                archive.write(path, member)

    setup_name = "Lightframe-" + args.version + "-setup.zip"
    companions_name = "Lightframe-" + args.version + "-companions.zip"
    setup_files = [(assets / n, n) for n in core + shared]
    setup_files += [(root / n, n) for n in ["README.md", "README.en.md", "CHANGELOG.md",
                    "scripts/setup.py", "scripts/configure.py", "scripts/capture-tv.py"]]
    # Only public documentation from the reviewed Git checkout is included.
    tracked_docs = subprocess.check_output(["git", "ls-files", "-z", "--", "docs"], cwd=root).split(b"\0")
    setup_files += [(root / name.decode(), name.decode()) for name in tracked_docs if name]
    bundle(setup_name, setup_files)
    bundle(companions_name, [(assets / n, n) for n in companions + shared]
           + [(p, "licenses/" + p.name) for p in licenses])
    names = apks + shared + [setup_name, companions_name, "third-party-sources.zip"]
    (assets / "RELEASE-SHA256SUMS").write_text("".join(
        digest(assets / name) + "  " + name + "\n" for name in sorted(names)))
    print("Packaged", args.version, "from", commit, "with", len(apks), "verified APKs")


if __name__ == "__main__":
    main()
