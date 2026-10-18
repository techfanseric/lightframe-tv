package local.simple.tvhome;
import android.content.*;
public final class ConfigReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  if("local.simple.tvhome.PROXY_DIAGNOSTIC".equals(i.getAction())){
   PendingResult pending=goAsync();Context app=c.getApplicationContext();String group=i.getStringExtra("group"),node=i.getStringExtra("node");
   new Thread(()->{try{ProxyDiagnostic.run(app,group,node);}finally{pending.finish();}},"ProxyDiagnostic").start();return;
  }
  if(!"local.simple.tvhome.CONFIGURE_CORE".equals(i.getAction()))return;
  String secret=i.getStringExtra("secret");if(secret==null||secret.isEmpty()||secret.length()>256)return;
  LauncherSupport.preferences(c).edit().putString("core_secret",secret).apply();
 }
}