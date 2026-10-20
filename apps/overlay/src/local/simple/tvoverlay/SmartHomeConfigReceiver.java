package local.simple.tvoverlay;

import android.content.*;
import java.io.*;
import org.json.JSONObject;

/** Explicit ADB-only provisioning. Never accept a token or a path from an unprivileged app. */
public final class SmartHomeConfigReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if("local.simple.tvoverlay.GET_HOME_LAYOUT".equals(intent.getAction())){
            HomeAssistantClient client=new HomeAssistantClient(context);
            try{setResultCode(1);setResultData(client.quickRows().toString());}finally{client.close();}
            return;
        }
        if("local.simple.tvoverlay.PROBE_REMOTE_VOICE".equals(intent.getAction())){String result=RemoteVoiceProbe.start(context,intent.getIntExtra("source",6));setResultCode(result.startsWith("Probe started")?1:0);setResultData(result);return;}
        if("local.simple.tvoverlay.REMOTE_VOICE_RESULT".equals(intent.getAction())){setResultCode(1);setResultData(context.getSharedPreferences("voice_probe",Context.MODE_PRIVATE).getString("result","not started"));return;}
        if(!"local.simple.tvoverlay.CONFIGURE_HOME_ASSISTANT".equals(intent.getAction()))return;
        PendingResult pending=goAsync();
        new Thread(()->{
            File file=new File(context.getExternalFilesDir(null),"home-assistant-import.json");HomeAssistantClient client=new HomeAssistantClient(context);
            try{
                if(!file.isFile()||file.length()>65536)throw new IOException("Missing or oversized configuration");
                byte[] data=new byte[(int)file.length()];try(DataInputStream in=new DataInputStream(new FileInputStream(file))){in.readFully(data);}
                client.importConfiguration(new JSONObject(new String(data,java.nio.charset.StandardCharsets.UTF_8)));
                pending.setResultCode(1);pending.setResultData("Home Assistant configured");
            }catch(Exception error){pending.setResultCode(0);pending.setResultData("Home Assistant configuration failed");}
            finally{file.delete();client.close();pending.finish();}
        },"HomeAssistantImport").start();
    }
}
