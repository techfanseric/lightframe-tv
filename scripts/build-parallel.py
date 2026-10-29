#!/usr/bin/env python3
"""Build a separately signed Lightframe pair; never overwrite/uninstall original packages."""
from pathlib import Path
import os, shutil, subprocess
root=Path(__file__).resolve().parents[1]
local=root/'.local';local.mkdir(exist_ok=True)
build=local/'parallel-build'
if build.exists():shutil.rmtree(build)
(build/'scripts').mkdir(parents=True)
shutil.copytree(root/'apps',build/'apps',ignore=shutil.ignore_patterns('build'))
for name in ['build.sh','build-app.sh']:shutil.copy2(root/'scripts'/name,build/'scripts'/name)
for file in (build/'apps').rglob('*'):
    if file.suffix in ('.java','.xml'):
        text=file.read_text().replace('local.simple.tvhome','local.simple.tvhome.dock').replace('local.simple.tvoverlay','local.simple.tvoverlay.dock')
        file.write_text(text)
for app,pkg in [('home','local.simple.tvhome.dock'),('overlay','local.simple.tvoverlay.dock')]:
    source=build/'apps'/app/'src'/Path(pkg.replace('.','/'))
    source.mkdir(parents=True,exist_ok=True)
    ha='''if(data.has("home_assistant")){HomeAssistantClient client=new HomeAssistantClient(context);try{org.json.JSONObject config=data.getJSONObject("home_assistant");client.importConfiguration(config);org.json.JSONArray favorites=config.optJSONArray("favorites");if(favorites!=null){java.util.List<String> ids=new java.util.ArrayList<>();for(int n=0;n<favorites.length();n++)ids.add(favorites.getString(n));client.setFavoriteIds(ids);}}finally{client.close();}}''' if app=='overlay' else ''
    code='''package PKG;
import android.content.*;import java.io.*;import org.json.*;
/** ADB-only migration of bounded, explicitly allowed preferences. */
public final class ParallelConfigReceiver extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent){if(!"PKG.IMPORT_SETTINGS".equals(intent.getAction()))return;
  File file=new File(context.getExternalFilesDir(null),"parallel-settings.json");
  try{if(!file.isFile()||file.length()>131072)throw new IOException();byte[] raw=new byte[(int)file.length()];try(DataInputStream in=new DataInputStream(new FileInputStream(file))){in.readFully(raw);}JSONObject data=new JSONObject(new String(raw,java.nio.charset.StandardCharsets.UTF_8));
   SharedPreferences.Editor edit=LauncherSupport.preferences(context).edit();
   for(String name:new String[]{"core_secret","native_default_app","native_status_mode","last_native_app","last_video_app"})if(data.has(name)){String value=data.getString(name);if(value.length()>256)throw new IOException();edit.putString(name,value);}
   if(data.has("native_boot_enabled"))edit.putBoolean("native_boot_enabled",data.getBoolean("native_boot_enabled"));
   if(data.has("native_launch_boot"))edit.putInt("native_launch_boot",data.getInt("native_launch_boot"));
   if(!edit.commit())throw new IOException();HA
   setResultCode(1);setResultData("Parallel settings imported");
  }catch(Exception e){setResultCode(0);setResultData("Parallel settings import failed");}finally{file.delete();}
 }
}'''.replace('PKG',pkg).replace('HA',ha)
    (source/'ParallelConfigReceiver.java').write_text(code)
    manifest=build/'apps'/app/'AndroidManifest.xml'
    text=manifest.read_text().replace('</application>','<receiver android:name=".ParallelConfigReceiver" android:exported="true" android:permission="android.permission.DUMP"/></application>')
    manifest.write_text(text)
    strings=build/'apps'/app/'res/values/strings.xml'
    text=strings.read_text().replace('轻幕','轻幕新版');strings.write_text(text)
key=Path(os.environ.get('LIGHTFRAME_KEYSTORE',str(local/'parallel-signing.jks'))).expanduser().resolve()
if os.environ.get('LIGHTFRAME_KEYSTORE') and not key.exists():raise SystemExit('Configured signing key is missing')
if not key.exists():
    subprocess.run(['keytool','-genkeypair','-keystore',str(key),'-storepass','android','-keypass','android','-alias','parallel','-dname','CN=Lightframe Parallel Local','-keyalg','RSA','-keysize','2048','-validity','10000'],check=True)
    key.chmod(0o600)
env=dict(os.environ,LIGHTFRAME_KEYSTORE=str(key),LIGHTFRAME_KEY_ALIAS=os.environ.get('LIGHTFRAME_KEY_ALIAS','parallel'),LIGHTFRAME_UNSIGNED='0')
subprocess.run(['sh',str(build/'scripts/build.sh')],env=env,check=True)
(root/'dist').mkdir(exist_ok=True)
for app in ['Home','Overlay']:
    target=root/'dist'/('Lightframe'+app+'-Parallel.apk');shutil.copy2(build/'dist'/('Lightframe'+app+'.apk'),target);print(target)

import hashlib
artifacts=[root/"dist"/("Lightframe"+name+"-Parallel.apk") for name in ["Home","Overlay"]]
(root/"dist/SHA256SUMS").write_text("".join(hashlib.sha256(p.read_bytes()).hexdigest()+"  "+p.name+"\n" for p in artifacts))
