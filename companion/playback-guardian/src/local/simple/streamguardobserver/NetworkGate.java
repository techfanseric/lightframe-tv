package local.simple.streamguardobserver;
import android.content.Context;
import android.net.Network;
import android.os.*;
import java.net.*;
import java.util.concurrent.*;

/** Public HTTPS over the current configured VPN; no access to the launcher's private controller secret. */
final class NetworkGate {
 interface Listener{void status(String text);void ready();void failed(String text);void startingProxy();}
 private final Context context;private final Listener listener;private final Handler main=new Handler();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private Future<?> task;private volatile HttpURLConnection active;private int generation,successes,starts;private long lastStart;private boolean waiting,closed;private Network verified;
 NetworkGate(Context c,Listener l){context=c;listener=l;}
 void start(){cancel();if(closed)return;waiting=true;successes=0;starts=0;lastStart=0;verified=null;
  if(!Bridge.googleDisabled(context)){fail("兼容模式状态已改变，请先检查电视设置");return;}
  main.postDelayed(deadline,120000);tick.run();
 }
 void cancel(){generation++;waiting=false;main.removeCallbacks(tick);main.removeCallbacks(deadline);if(task!=null){task.cancel(true);task=null;}HttpURLConnection c=active;if(c!=null)c.disconnect();}
 void close(){closed=true;cancel();worker.shutdownNow();}
 private void fail(String reason){cancel();listener.failed(reason);}
 private final Runnable deadline=()->{if(waiting)fail("暂时无法连接 Netflix，请检查网络后重试");};
 private final Runnable tick=new Runnable(){public void run(){
  if(closed||!waiting)return;
  if(!Bridge.correctVpnPolicy(context)){fail("请先恢复 FlClash 常开 VPN 设置");return;}
  final Network network=Bridge.vpn(context);
  if(network==null){successes=0;verified=null;listener.status("正在连接网络…");long now=SystemClock.elapsedRealtime();
   if(starts<3&&(lastStart==0||now-lastStart>=15000)){starts++;lastStart=now;listener.startingProxy();Bridge.startProxy(context);}
   main.postDelayed(this,1500);return;
  }
  listener.status(successes==0?"正在检查 Netflix 连接…":"连接已恢复，正在确认稳定…");final int token=generation;
  task=worker.submit(()->{boolean ok=false;try{ok=request(network,"https://www.netflix.com/robots.txt")&&request(network,"https://www.netflix.com/login");}catch(Exception e){android.util.Log.i("StreamGuardObserver","netflix_probe failure="+e.getClass().getSimpleName());}final boolean result=ok;
   main.post(()->{if(closed||!waiting||token!=generation)return;task=null;
    if(result&&network.equals(Bridge.vpn(context))){successes=network.equals(verified)?successes+1:1;verified=network;if(successes>=2){cancel();listener.ready();return;}}
    else{successes=0;verified=null;listener.status("正在等待 Netflix 网络恢复…");}
    main.postDelayed(tick,result?1500:2500);
   });
  });
 }};
 private boolean request(Network network,String url)throws Exception{
  if(Thread.currentThread().isInterrupted())throw new InterruptedException();
  HttpURLConnection c=(HttpURLConnection)network.openConnection(new URL(url));active=c;
  try{c.setConnectTimeout(4000);c.setReadTimeout(4000);c.setUseCaches(false);c.setInstanceFollowRedirects(false);c.setRequestProperty("Connection","close");int code=c.getResponseCode();
   if(code>=300&&code<400){String location=c.getHeaderField("Location");if(location==null)return false;URL next=new URL(new URL(url),location);return "https".equals(next.getProtocol())&&("netflix.com".equals(next.getHost())||next.getHost().endsWith(".netflix.com"));}
   return code>=200&&code<300;
  }finally{c.disconnect();if(active==c)active=null;}
 }
}
