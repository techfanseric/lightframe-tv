#!/usr/bin/env python3
"""Compatibility wrapper for the v0.2 parallel edition."""
import os,sys
from pathlib import Path
if len(sys.argv)!=3:raise SystemExit("Usage: configure-home-assistant.py SERIAL PRIVATE_CONFIG_FILE")
os.execv(sys.executable,[sys.executable,str(Path(__file__).with_name("configure.py")),"--serial",sys.argv[1],"--home-assistant",sys.argv[2]])
