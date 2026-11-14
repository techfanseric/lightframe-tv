#!/usr/bin/env python3
"""Provision v0.2 parallel packages through ADB-only, file-based receivers."""
import argparse,json,os,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--clash-secret',type=Path);p.add_argument('--home-assistant',type=Path);p.add_argument('--proxy-mode',choices=('auto','direct','off'));p.add_argument('--bose-host');p.add_argument('--bose-port',type=int,default=8090);p.add_argument('--bose-account',default='TV');p.add_argument('--bose-hdmi-account',default='HDMI_1');a=p.parse_args()
if not(a.clash_secret or a.home_assistant or a.proxy_mode or a.bose_host):p.error('Choose --clash-secret, --home-assistant, --proxy-mode or --bose-host')
adb=[os.environ.get('ADB','adb'),'-s',a.serial]
def run(*v):return subprocess.check_output(adb+list(v),text=True,stderr=subprocess.STDOUT)
base={}
if a.clash_secret:
 secret=a.clash_secret.read_text().strip()
 if not secret or len(secret)>256:raise SystemExit('Invalid controller credential')
 base['core_secret']=secret
if a.proxy_mode:
 base['proxy_mode']=a.proxy_mode
 base['manual_proxy_off']=a.proxy_mode=='direct'
bose=None
if a.bose_host:
 host=a.bose_host.strip()
 if not host or len(host)>64 or any(c in host for c in ' /\\:"\'@') or '/' in host:raise SystemExit('Invalid speaker address')
 if not 1<=a.bose_port<=65535:raise SystemExit('Invalid speaker port')
 account=a.bose_account.strip()
 if not account or len(account)>32 or any(c in account for c in ' /\\:"\'<>&'):raise SystemExit('Invalid speaker source account')
 hdmi=a.bose_hdmi_account.strip()
 if not hdmi or len(hdmi)>32 or any(c in hdmi for c in ' /\\:"\'<>&'):raise SystemExit('Invalid speaker HDMI account')
 bose={'bose_arc_host':host,'bose_arc_port':a.bose_port,'bose_arc_account':account,'bose_hdmi_account':hdmi}
ha=None
if a.home_assistant:
 ha=json.loads(a.home_assistant.read_text());assert all(k in ha for k in ('url','token','controls')),'Missing HA configuration fields'
for package in ['local.simple.tvhome.dock','local.simple.tvoverlay.dock']:
 data=dict(base)
 if package.endswith('tvoverlay.dock') and bose:data.update(bose)
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
