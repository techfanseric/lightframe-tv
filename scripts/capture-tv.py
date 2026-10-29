#!/usr/bin/env python3
"""Capture a real TV frame; tolerate vendor diagnostics before the PNG header."""
import argparse
import pathlib
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True, help='Explicit adb device serial')
    parser.add_argument('--output', required=True, type=pathlib.Path)
    args = parser.parse_args()
    adb = shutil.which('adb')
    if not adb:
        parser.error('adb not found')
    result = subprocess.run([adb, '-s', args.serial, 'exec-out', 'screencap', '-p'],
                            capture_output=True, timeout=30)
    signature = b'\x89PNG\r\n\x1a\n'
    offset = result.stdout.find(signature)
    if result.returncode or offset < 0 or b'IEND' not in result.stdout[offset:]:
        parser.exit(1, 'Capture failed or incomplete; no image written.\n')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(result.stdout[offset:])
    print(f'Saved real device capture: {args.output} (removed {offset} vendor prefix bytes)')
    print('Protected video may be blank; this does not indicate playback success or failure.')


if __name__ == '__main__':
    main()
