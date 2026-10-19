package local.simple.tvoverlay;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** Explicit diagnostic only: no audio files, network traffic, recognition or key interception. */
final class RemoteVoiceProbe {
    private static final AtomicBoolean running=new AtomicBoolean();
    static String start(Context context,int requestedSource){
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return "Microphone permission required";
        if(!running.compareAndSet(false,true))return "Probe already running";
        final int source=requestedSource==1?1:6;
        context.getSharedPreferences("voice_probe",Context.MODE_PRIVATE).edit().putString("result","running").commit();
        new Thread(()->{
            AudioRecord recorder=null;JSONObject result=new JSONObject();
            try{
                int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
                if(min<0)throw new IllegalStateException("Unsupported audio format");
                recorder=new AudioRecord(source,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,16000));
                if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Audio input unavailable");
                recorder.startRecording();if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("Recording did not start");
                short[] samples=new short[1600];long start=SystemClock.elapsedRealtime(),count=0;int peak=0,second=0,windowPeak=0;long windowCount=0;double sum=0,windowSum=0;JSONArray windows=new JSONArray();
                while(SystemClock.elapsedRealtime()-start<20000){
                    int n=recorder.read(samples,0,samples.length,AudioRecord.READ_NON_BLOCKING);
                    if(n<0)throw new IllegalStateException("Audio input failed");
                    for(int i=0;i<n;i++){int a=Math.abs((int)samples[i]);peak=Math.max(peak,a);windowPeak=Math.max(windowPeak,a);double square=(double)samples[i]*samples[i];sum+=square;windowSum+=square;samples[i]=0;}
                    count+=n;windowCount+=n;int elapsed=(int)((SystemClock.elapsedRealtime()-start)/1000);
                    if(elapsed>second){windows.put(new JSONObject().put("second",elapsed).put("samples",windowCount).put("peak",windowPeak).put("rms",windowCount==0?0:Math.sqrt(windowSum/windowCount)));second=elapsed;windowCount=0;windowPeak=0;windowSum=0;}
                    if(n==0)SystemClock.sleep(10);
                }
                result.put("source",source).put("samples",count).put("peak",peak).put("rms",count==0?0:Math.sqrt(sum/count)).put("windows",windows);
            }catch(Exception error){try{result.put("error",error.getClass().getSimpleName());}catch(JSONException ignored){}}
            finally{if(recorder!=null){try{recorder.stop();}catch(Exception ignored){}recorder.release();}context.getSharedPreferences("voice_probe",Context.MODE_PRIVATE).edit().putString("result",result.toString()).commit();running.set(false);}
        },"RemoteVoiceProbe").start();
        return "Probe started: 20 seconds, statistics only";
    }
}
