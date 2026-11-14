package local.simple.tvoverlay;

import android.content.Context;
import android.net.*;
import android.os.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Launch only after a real HTTPS response.
 *
 * In AUTO the response must additionally travel over an established VPN with a
 * live FlClash core. In DIRECT the upstream network (a router, for example)
 * already proxies the traffic, so only the target site is probed. In OFF
 * nothing is probed at all.
 */
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
    /**
     * Starts the wait for the given application.
     *
     * The budget is taken from the moment the request was made, so a slow
     * step before this call cannot extend the total wait.
     */
    void start(int target){start(target,SystemClock.elapsedRealtime());}
    void start(int target,long requestedAt){
        cancel();app=target;waiting=true;started=requestedAt>0?requestedAt:SystemClock.elapsedRealtime();lastStart=started;
        android.util.Log.i("HomeNetwork","waiting app="+AppCatalog.IDS[app]+" mode="+ProxyMode.mode(context));
        if(ProxyMode.OFF.equals(ProxyMode.mode(context))){
            waiting=false;android.util.Log.i("HomeNetwork","proxy disabled; launching directly");listener.ready(app);return;
        }
        if(LauncherSupport.ensureProxyStarted(context))listener.startingProxy();tick.run();
    }
    void cancel(){generation++;waiting=false;status="";main.removeCallbacks(tick);}
    private boolean vpnConnected(){
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm!=null)for(Network n:cm.getAllNetworks()){
            NetworkCapabilities caps=cm.getNetworkCapabilities(n);
            // A tunnel that is being torn down has no INTERNET capability for
            // a moment while it is still the transport in use. The transport
            // alone is enough to know the traffic is going through it.
            if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return true;
        }
        return false;
    }
    private void status(String value){if(!value.equals(status)){status=value;listener.waiting(value);}}
    private final Runnable tick=new Runnable(){public void run(){
        if(closed||!waiting)return;
        long now=SystemClock.elapsedRealtime();
        if(NetworkWaitPolicy.expired(now,started)){waiting=false;listener.failed(app,"网络未就绪");android.util.Log.w("HomeNetwork","timeout app="+AppCatalog.IDS[app]);return;}
        final boolean direct=!ProxyMode.requiresVpn(context);
        if(!direct&&!vpnConnected()){
            status("正在连接 FlClash…");
            // The proxy is asked for again while there is budget left. A slow
            // tunnel used to stop being pulled up after 45 s and the wait then
            // ran out passively.
            if(NetworkWaitPolicy.startProxyAgain(now,started,lastStart)){lastStart=now;if(LauncherSupport.ensureProxyStarted(context))listener.startingProxy();}
            main.postDelayed(this,1500);return;
        }
        status(direct?"正在验证网络…":"代理已连接，正在验证网络…");
        if(probing){main.postDelayed(this,1500);return;}
        probing=true;final int request=generation;
        worker.execute(()->{
            boolean ok=false;final int target=app;
            try{
                String secret=LauncherSupport.preferences(context).getString("core_secret","");
                boolean coreReady=direct||(!secret.isEmpty()&&request("http://127.0.0.1:9090/version",secret)==200);
                if(coreReady){
                    int response=request(NetworkProbe.url(target),null);
                    ok=NetworkProbe.reachable(target,response);
                }
            }catch(Exception ignored){}
            final boolean result=ok;
            main.post(()->{
                probing=false;if(closed||!waiting||request!=generation)return;
                if(result&&(direct||vpnConnected())){
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
