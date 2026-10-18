package local.simple.tvhome;

import android.content.Context;
import org.json.*;
import java.net.*;
import java.io.*;

/** DUMP-protected, bounded controller diagnostics. Never exports credentials or proxy URLs. */
final class ProxyDiagnostic {
 static JSONObject request(Context c,String path,String method,JSONObject body)throws Exception{
  HttpURLConnection h=(HttpURLConnection)new URL("http://127.0.0.1:9090"+path).openConnection();
  try{
   h.setConnectTimeout(750);h.setReadTimeout(750);h.setRequestMethod(method);
   h.setRequestProperty("Authorization","Bearer "+LauncherSupport.preferences(c).getString("core_secret",""));
   if(body!=null){h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json");try(OutputStream o=h.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}}
   int code=h.getResponseCode();if(code==204)return new JSONObject().put("status",204);
   if(code!=200)throw new IOException("controller HTTP "+code);
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=h.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2097152)throw new IOException("response too large");}}
   return new JSONObject(out.toString("UTF-8"));
  }finally{h.disconnect();}
 }
 static JSONObject probe(String host){
  JSONObject r=new JSONObject();long start=android.os.SystemClock.elapsedRealtime();HttpURLConnection h=null;
  try{r.put("host",host);JSONArray ips=new JSONArray();for(InetAddress ip:InetAddress.getAllByName(host))ips.put(ip.getHostAddress());r.put("addresses",ips);
   h=(HttpURLConnection)new URL("https://"+host+"/").openConnection();h.setConnectTimeout(6000);h.setReadTimeout(6000);h.setInstanceFollowRedirects(false);h.setUseCaches(false);h.setRequestProperty("Connection","close");r.put("httpCode",h.getResponseCode());
  }catch(Exception e){try{r.put("error",e.getClass().getSimpleName()+": "+e.getMessage());}catch(JSONException ignored){}}
  finally{if(h!=null)h.disconnect();try{r.put("elapsedMs",android.os.SystemClock.elapsedRealtime()-start);}catch(JSONException ignored){}}
  return r;
 }
 static void run(Context c,String group,String node){
  JSONObject result=new JSONObject();
  try{
   JSONObject all=request(c,"/proxies","GET",null).getJSONObject("proxies"),groups=new JSONObject();
   if(group!=null&&node!=null){JSONObject selected=all.optJSONObject(group);if(selected==null||selected.optJSONArray("all")==null)throw new IOException("unknown selector");
    JSONArray choices=selected.getJSONArray("all");boolean valid=false;for(int i=0;i<choices.length();i++)if(node.equals(choices.optString(i)))valid=true;
    if(!valid)throw new IOException("unknown node");
    request(c,"/proxies/"+URLEncoder.encode(group,"UTF-8").replace("+","%20"),"PUT",new JSONObject().put("name",node));
    result.put("selected",new JSONObject().put("group",group).put("node",node));all=request(c,"/proxies","GET",null).getJSONObject("proxies");
   }
   java.util.Iterator<String> keys=all.keys();while(keys.hasNext()){String k=keys.next();JSONObject p=all.optJSONObject(k);if(p!=null&&p.has("all"))groups.put(k,new JSONObject().put("type",p.optString("type")).put("now",p.optString("now")).put("all",p.optJSONArray("all")));}
   result.put("groups",groups);
   JSONObject cfg=request(c,"/configs","GET",null);result.put("mode",cfg.optString("mode")).put("ipv6",cfg.optBoolean("ipv6")).put("mixedPort",cfg.optInt("mixed-port")).put("socksPort",cfg.optInt("socks-port"));
   JSONArray rules=request(c,"/rules","GET",null).optJSONArray("rules"),keep=new JSONArray();
   if(rules!=null)for(int i=0;i<rules.length();i++){JSONObject r=rules.optJSONObject(i);if(r==null)continue;String payload=r.optString("payload").toLowerCase();if(payload.contains("netflix")||payload.contains("nflx")||"Match".equals(r.optString("type")))keep.put(r);}
   result.put("netflixRules",keep);
   JSONArray connections=request(c,"/connections","GET",null).optJSONArray("connections"),filtered=new JSONArray();
   if(connections!=null)for(int i=0;i<connections.length();i++){JSONObject x=connections.optJSONObject(i);if(x==null)continue;JSONObject m=x.optJSONObject("metadata");if(m==null)continue;
    String host=m.optString("host").toLowerCase();if(host.contains("netflix")||host.contains("nflx")||host.equals("fast.com"))filtered.put(new JSONObject().put("host",host).put("ip",m.optString("destinationIP")).put("port",m.optString("destinationPort")).put("network",m.optString("network")).put("dnsMode",m.optString("dnsMode")).put("chains",x.optJSONArray("chains")).put("rule",x.optString("rule")).put("rulePayload",x.optString("rulePayload")));}
   result.put("connections",filtered);
   result.put("profile",cfg.optJSONObject("profile"));
   JSONArray configKeys=new JSONArray();java.util.Iterator<String> cfgKeys=cfg.keys();while(cfgKeys.hasNext())configKeys.put(cfgKeys.next());result.put("configKeys",configKeys);
   result.put("dnsConfig",cfg.optJSONObject("dns"));
   try{result.put("coreDns",request(c,"/dns/query?name=secure.netflix.com&type=A","GET",null));}catch(Exception e){result.put("coreDnsError",e.getClass().getSimpleName());}
   java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(4);
   java.util.List<java.util.concurrent.Callable<JSONObject>> tasks=new java.util.ArrayList<>();
   for(String host:new String[]{"secure.netflix.com","appboot.netflix.com","uiboot.netflix.com","fast.com"})tasks.add(()->probe(host));
   JSONArray probes=new JSONArray();
   try{java.util.List<java.util.concurrent.Future<JSONObject>> futures=new java.util.ArrayList<>();for(java.util.concurrent.Callable<JSONObject> t:tasks)futures.add(pool.submit(t));
    Thread.sleep(900);
    JSONArray during=request(c,"/connections","GET",null).optJSONArray("connections"),pending=new JSONArray();
    if(during!=null)for(int i=0;i<during.length();i++){JSONObject x=during.getJSONObject(i),m=x.optJSONObject("metadata");if(m==null)continue;String host=m.optString("host"),ip=m.optString("destinationIP");
     if(host.contains("netflix")||host.contains("nflx")||host.equals("fast.com")||ip.startsWith("28."))pending.put(new JSONObject().put("host",host).put("ip",ip).put("chains",x.optJSONArray("chains")).put("rule",x.optString("rule")).put("rulePayload",x.optString("rulePayload")));}
    result.put("pendingConnections",pending);
    long deadline=android.os.SystemClock.elapsedRealtime()+13000;
    for(java.util.concurrent.Future<JSONObject> f:futures){
     if(f.isCancelled())probes.put(new JSONObject().put("error","probe timed out"));else try{probes.put(f.get(Math.max(1,deadline-android.os.SystemClock.elapsedRealtime()),java.util.concurrent.TimeUnit.MILLISECONDS));}catch(java.util.concurrent.TimeoutException e){f.cancel(true);probes.put(new JSONObject().put("error","probe deadline exceeded"));}
   }}finally{pool.shutdownNow();}
   result.put("probes",probes);
  }catch(Exception e){try{result.put("error",e.getClass().getSimpleName()+": "+e.getMessage());}catch(JSONException ignored){}}
  try{File f=new File(c.getExternalFilesDir(null),"proxy-diagnostic.json");try(FileOutputStream o=new FileOutputStream(f)){o.write(result.toString(2).getBytes("UTF-8"));}android.util.Log.i("NetflixRepair","proxy diagnostic saved");}catch(Exception e){android.util.Log.w("NetflixRepair","diagnostic save failed");}
 }
}
