#!/usr/bin/env python3
"""Exercise the isolated preview only; never target a physical television."""
import subprocess, xml.etree.ElementTree as ET, pathlib, re
ADB='/opt/homebrew/bin/adb'; SERIAL='emulator-5580'; PKG='local.simple.tvoverlay.preview'
ROOT=pathlib.Path(__file__).resolve().parent

def adb(*args): return subprocess.check_output([ADB,'-s',SERIAL,*args],text=True)
def key(*keys): adb('shell','input','keyevent',*[str(k) for k in keys])
def state():
    adb('shell','rm','-f','/sdcard/dock-test.xml')
    adb('shell','uiautomator','dump','/sdcard/dock-test.xml')
    root=ET.fromstring(adb('shell','cat','/sdcard/dock-test.xml'))
    texts={n.get('text') for n in root.iter('node') if n.get('text')}
    focus=next((n for n in root.iter('node') if n.get('focused')=='true'),None)
    focused={n.get('text') or n.get('content-desc') for n in focus.iter('node') if n.get('text') or n.get('content-desc')} if focus is not None else set()
    return root,texts,focused

def expect(title,expanded=None):
    root,texts,focused=state();assert any(value==title or value.startswith(title+' · ') for value in focused),(title,focused)
    assert ('开机应用' in texts)==(expanded=='settings'),texts
    assert ('客厅灯' in texts)==(expanded=='home'),texts
    assert any(n.get('resource-id')==PKG+':id/lightframe_dock' for n in root.iter('node'))
    print('PASS',title,expanded or 'collapsed',flush=True)

def shot(name):
    data=subprocess.check_output([ADB,'-s',SERIAL,'exec-out','screencap','-p'])
    (ROOT/'images'/name).write_bytes(data)

adb('shell','am','force-stop',PKG)
adb('shell','am','start','-n',PKG+'/local.simple.tvoverlay.DockPreviewActivity')
adb('logcat','-c')
expect('YouTube');shot('dock-apps.png')
initial_root,_,_=state()
anchor=next(n.get('bounds') for n in initial_root.iter('node') if n.get('content-desc')=='Netflix')
key(22,22,22,22);expect('设置','settings');shot('dock-settings.png')
root,_,_=state()
assert next(n.get('bounds') for n in root.iter('node') if n.get('content-desc')=='Netflix')==anchor, 'Dock moved when settings opened'
bounds={n.get('content-desc'):re.findall(r'\d+',n.get('bounds','')) for n in root.iter('node') if n.get('content-desc') in {'YouTube · TizenTube','Netflix','哔哩哔哩'}}
assert len({(b[1],b[3]) for b in bounds.values()})==1,bounds
assert len(bounds)==3,bounds
assert not any(n.get('text') in {'YouTube','Netflix','哔哩哔哩','TizenTube'} for n in root.iter('node'))
print('PASS aligned compact tiles; names only in accessibility descriptions',flush=True)
key(19);expect('开机应用','settings')
key(21);expect('FlClash','settings')
key(20);expect('ADB 调试','settings')
key(20);expect('设置','settings')
key(22);expect('智能家居','home');shot('dock-home.png')
root,_,_=state()
assert next(n.get('bounds') for n in root.iter('node') if n.get('content-desc')=='Netflix')==anchor, 'Dock moved when home opened'
print('PASS dock anchor unchanged across category expansion',flush=True)
key(19);expect('客厅灯','home')
key(21);expect('全部设备','home')
key(20);expect('智能家居','home')
key(22);expect('YouTube')
key(21);expect('智能家居','home')
key(21,21);expect('IPTV')
assert 'ACTION ' not in adb('logcat','-d','-s','DockPreview:I','*:S'), 'Focusing executed an action'
key(4)
assert PKG not in next((s for s in adb('shell','dumpsys','window','windows').splitlines() if 'mCurrentFocus=' in s),'')
print('PASS focus performs no action; BACK closes preview',flush=True)
adb('shell','am','start','-n',PKG+'/local.simple.tvoverlay.DockPreviewActivity','--ez','bright','true')
expect('YouTube');shot('dock-bright.png')
