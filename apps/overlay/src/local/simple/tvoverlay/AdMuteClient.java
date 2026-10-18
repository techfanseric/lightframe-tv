package local.simple.tvoverlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** LAN capability restricted to the helper's ad-mute preference. */
final class AdMuteClient implements AutoCloseable {
    static final class State {
        final boolean available, known, enabled, connected, applied;
        State(boolean available, boolean known, boolean enabled, boolean connected, boolean applied) {
            this.available=available;this.known=known;this.enabled=enabled;
            this.connected=connected;this.applied=applied;
        }
    }
    interface Callback { void done(State state); }
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private volatile HttpURLConnection active;
    private volatile State state;

    AdMuteClient(Context context) {
        this.context=context;this.prefs=LauncherSupport.preferences(context);
        state=new State(false,prefs.contains("ad_mute_confirmed"),prefs.getBoolean("ad_mute_confirmed",false),false,false);
    }
    State state(){return state;}
    void read(Callback callback){submit(null,callback);}
    void write(boolean enabled,Callback callback){submit(enabled,callback);}

    private String token() throws Exception {
        File dir=context.getExternalFilesDir(null);
        File pending=dir==null?null:new File(dir,"ad-mute-token");
        if(pending!=null&&pending.isFile()&&pending.length()<=256){
            String imported;
            try(InputStream in=new FileInputStream(pending)){imported=new String(readBounded(in,256),StandardCharsets.UTF_8).trim();}
            if(imported.matches("[A-Za-z0-9_-]{32,128}")){
                if(prefs.edit().putString("ad_mute_token",imported).commit())pending.delete();
            }
        }
        String token=prefs.getString("ad_mute_token","");
        if(token.isEmpty())throw new java.io.IOException("unconfigured");
        return token;
    }
    private static byte[] readBounded(InputStream in,int limit) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[512];int size;
        while((size=in.read(buffer))!=-1){if(out.size()+size>limit)throw new java.io.IOException("response too large");out.write(buffer,0,size);}
        return out.toByteArray();
    }
    private void submit(Boolean desired,Callback callback){
        if(closed)return;
        worker.execute(()->{
            HttpURLConnection connection=null;State result;
            try{
                String capability=token();
                connection=(HttpURLConnection)new URL("http://10.0.0.1:17792/v1/ad-mute").openConnection();active=connection;
                connection.setConnectTimeout(1500);connection.setReadTimeout(3000);
                connection.setUseCaches(false);connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod(desired==null?"GET":"POST");
                connection.setRequestProperty("Authorization","Bearer "+capability);
                if(desired!=null){
                    byte[] body=("{\"mute_ads\":"+desired+"}").getBytes(StandardCharsets.UTF_8);
                    connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");
                    connection.setFixedLengthStreamingMode(body.length);
                    try(java.io.OutputStream out=connection.getOutputStream()){out.write(body);}
                }
                if(connection.getResponseCode()!=200)throw new java.io.IOException("unavailable");
                JSONObject data;
                try(InputStream in=connection.getInputStream()){data=new JSONObject(new String(readBounded(in,4096),StandardCharsets.UTF_8));}
                if(data.optInt("version")!=1||!(data.opt("mute_ads") instanceof Boolean)
                    ||!(data.opt("connected") instanceof Boolean)||!(data.opt("applied") instanceof Boolean))throw new java.io.IOException("invalid response");
                boolean enabled=data.getBoolean("mute_ads");
                if(desired!=null&&enabled!=desired)throw new java.io.IOException("not confirmed");
                prefs.edit().putBoolean("ad_mute_confirmed",enabled).apply();
                result=new State(true,true,enabled,data.getBoolean("connected"),data.getBoolean("applied"));
                android.util.Log.i("AdMuteControl",(desired==null?"GET":"POST")+" confirmed enabled="+enabled+" connected="+result.connected);
            }catch(Exception ignored){
                State previous=state;result=new State(false,previous.known,previous.enabled,false,false);
                android.util.Log.i("AdMuteControl",(desired==null?"GET":"POST")+" unavailable");
            }finally{if(connection!=null)connection.disconnect();active=null;}
            state=result;final State delivered=result;
            main.post(()->{if(!closed)callback.done(delivered);});
        });
    }
    @Override public void close(){closed=true;HttpURLConnection connection=active;if(connection!=null)connection.disconnect();worker.shutdownNow();}
}
