#!/usr/bin/env python3
"""Provision v0.2 parallel packages through ADB-only, file-based receivers."""
import argparse,json,os,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--clash-secret',type=Path);p.add_argument('--home-assistant',type=Path);a=p.parse_args()
if not(a.clash_secret or a.home_assistant):p.error('Choose --clash-secret or --home-assistant')
adb=[os.environ.get('ADB','adb'),'-s',a.serial]
def run(*v):return subprocess.check_output(adb+list(v),text=True,stderr=subprocess.STDOUT)
base={}
if a.clash_secret:
 secret=a.clash_secret.read_text().strip()
 if not secret or len(secret)>256:raise SystemExit('Invalid controller credential')
 base['core_secret']=secret
ha=None
if a.home_assistant:
 ha=json.loads(a.home_assistant.read_text());assert all(k in ha for k in ('url','token','controls')),'Missing HA configuration fields'
for package in ['local.simple.tvhome.dock','local.simple.tvoverlay.dock']:
 data=dict(base)
 if package.endswith('tvoverlay.dock') and ha:data['home_assistant']=ha
 if not data:continue
 raw=json.dumps(data,ensure_ascii=False).encode()
 if len(raw)>131072:raise SystemExit('Configuration is too large')
 remote='/sdcard/Android/data/'+package+'/files/parallel-settings.json'
 with tempfile.TemporaryDirectory(prefix='lightframe-') as d:
  file=Path(d)/'settings.json';file.write_bytes(raw);file.chmod(0o600)
  try:
   run('shell','mkdir','-p',str(Path(remote).parent));run('push',str(file),remote)
   result=run('shell','am','broadcast','--include-stopped-packages','-n',package+'/.ParallelConfigReceiver','-a',package+'.IMPORT_SETTINGS')
   if 'result=1' not in result:raise SystemExit('Import failed; verify the v0.2 parallel APK pair')
  finally:subprocess.run(adb+['shell','rm','-f',remote],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
print('Configuration imported. No video launch or home-device command was sent.')
