#!/usr/bin/env python3
"""Configure an existing local Clash controller without printing its credential."""
import argparse,os,subprocess,shlex
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('serial');p.add_argument('credential_file',type=Path);args=p.parse_args()
secret=args.credential_file.read_text().strip()
if not secret or len(secret)>256:raise SystemExit('Invalid controller credential file')
adb=os.environ.get('ADB','adb');port=os.environ.get('ADB_SERVER_PORT','5037')
remote=['am','broadcast','-n','local.simple.tvhome/.ConfigReceiver','-a','local.simple.tvhome.CONFIGURE_CORE','--es','secret',secret]
subprocess.run([adb,'-P',port,'-s',args.serial,'shell',shlex.join(remote)],check=True,stdout=subprocess.DEVNULL)
print('Controller configured. Reopen HOME when ready.')
