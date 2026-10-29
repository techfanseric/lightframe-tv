package local.simple.tvoverlay;

import android.content.Context;
import android.net.*;
import android.os.*;
import java.net.*;
import java.util.concurrent.*;

/** Launch only after an established VPN, a live FlClash core and a real HTTPS response. */
final class NetworkReadyGate {
    interface Listener { void waiting(String status); void startingProxy(); void ready(int app); void failed(int app,String reason); }
    private final Context context;
    private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private int generation,app;
    private long started,lastStart;
    private boolean waiting,closed,probing;
    private String status="";
    NetworkReadyGate(Context c,Listener l){context=c;listener=l;}
    boolean isWaiting(){return waiting;}
    void start(int target){
        cancel();app=target;waiting=true;started=SystemClock.elapsedRealtime();lastStart=started;
        android.util.Log.i("HomeNetwork","waiting app="+AppCatalog.IDS[app]);
        String issue=app==2?GoogleServicesHealth.issue(context):null;
        if(issue!=null){waiting=false;listener.failed(app,issue);android.util.Log.w("HomeNetwork","blocked netflix: "+issue);return;}
        if(LauncherSupport.ensureProxyStarted(context))listener.startingProxy();tick.run();
    }
    void cancel(){generation++;waiting=false;status="";main.removeCallbacks(tick);}
    private boolean vpnConnected(){
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm!=null)for(Network n:cm.getAllNetworks()){
            NetworkCapabilities caps=cm.getNetworkCapabilities(n);
            if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))return true;
        }
        return false;
    }
    private void status(String value){if(!value.equals(status)){status=value;listener.waiting(value);}}
    private final Runnable tick=new Runnable(){public void run(){
        if(closed||!waiting)return;
        long now=SystemClock.elapsedRealtime();
        if(now-started>=120000){waiting=false;listener.failed(app,"网络未就绪");android.util.Log.w("HomeNetwork","timeout app="+AppCatalog.IDS[app]);return;}
        if(!vpnConnected()){
            status("正在连接 FlClash…");
            if(now-lastStart>=15000 && now-started<45000){lastStart=now;if(LauncherSupport.ensureProxyStarted(context))listener.startingProxy();}
            main.postDelayed(this,1500);return;
        }
        status("代理已连接，正在验证网络…");
        if(probing){main.postDelayed(this,1500);return;}
        probing=true;final int request=generation;
        worker.execute(()->{
            boolean ok=false;String failure=null;final int target=app;
            try{
                String secret=LauncherSupport.preferences(context).getString("core_secret","");
                if(!secret.isEmpty()&&request("http://127.0.0.1:9090/version",secret)==200){
                    int response=request(target==0?"https://www.youtube.com/generate_204":"https://www.netflix.com/robots.txt",null);
                    ok=target==0?response==204:response>=200&&response<400;
                    if(ok&&target==2){failure=GoogleServicesHealth.probe(context);ok=failure==null;}
                }
            }catch(Exception ignored){}
            final boolean result=ok;final String googleFailure=failure;
            main.post(()->{
                probing=false;if(closed||!waiting||request!=generation)return;
                if(googleFailure!=null){waiting=false;listener.failed(app,googleFailure);android.util.Log.w("HomeNetwork","blocked netflix: "+googleFailure);}
                else if(result&&vpnConnected()){
                    waiting=false;android.util.Log.i("HomeNetwork","ready app="+AppCatalog.IDS[app]+" elapsedMs="+(SystemClock.elapsedRealtime()-started));listener.ready(app);
                }else{status("正在等待外网可用…");main.postDelayed(tick,2500);}
            });
        });
    }};
    private int request(String url,String secret)throws Exception{
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        try{
            connection.setConnectTimeout(3000);connection.setReadTimeout(3000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);
            connection.setRequestProperty("Connection","close");if(secret!=null)connection.setRequestProperty("Authorization","Bearer "+secret);
            return connection.getResponseCode();
        }finally{connection.disconnect();}
    }
    void close(){closed=true;cancel();worker.shutdownNow();}
}
