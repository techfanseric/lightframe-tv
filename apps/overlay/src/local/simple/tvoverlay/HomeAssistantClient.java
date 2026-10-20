package local.simple.tvoverlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;

/** Local HA connection. Credentials stay encrypted in app-private storage, outside launcher prefs. */
final class HomeAssistantClient {
    private static final String KEY="lightframe.home-assistant",PREFS="home_assistant";
    private final SharedPreferences prefs;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private volatile HomeCatalog catalog;private HomeRegistry registry;private long registryAttempt;
    private volatile String catalogStatus="正在读取设备目录";
    private volatile Snapshot latest=new Snapshot(Collections.emptyMap(),"读取中",false);
    static final class State {
        final String value;final JSONObject attributes;
        State(String value,JSONObject attributes){this.value=value;this.attributes=attributes==null?new JSONObject():attributes;}
        boolean available(){return !"unavailable".equals(value)&&!"unknown".equals(value);}
    }
    static final class Snapshot {
        final Map<String,State> states;final String message,temperatureUnit;final boolean connected;
        Snapshot(Map<String,State> states,String message,boolean connected){this(states,message,connected,"");}
        Snapshot(Map<String,State> states,String message,boolean connected,String unit){this.states=states;this.message=message;this.connected=connected;this.temperatureUnit=unit;}
        String temperatureUnit(State state){return state.attributes.optString("temperature_unit",temperatureUnit);}
    }
    interface Result {void done(Snapshot snapshot);}
    interface ActionResult {void done(boolean success,String message);}
    HomeAssistantClient(Context context){
        prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        try{registry=new HomeRegistry(new JSONObject(prefs.getString("registry","")));catalog=registry.catalog(Collections.emptyMap(),aliases());catalogStatus="使用上次设备目录";}catch(Exception ignored){registry=null;catalog=new HomeCatalog(Collections.emptyList(),legacyControls());}
    }
    boolean configured(){return prefs.contains("token")&&!prefs.getString("url","").isEmpty();}
    Snapshot snapshot(){return latest;}
    HomeCatalog catalog(){return catalog;}
    String catalogStatus(){return catalogStatus;}
    List<HomeControl> controls(){return catalog.controls;}
    private List<HomeControl> legacyControls(){
        List<HomeControl> controls=new ArrayList<>();
        try{JSONArray a=new JSONArray(prefs.getString("controls","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);controls.add(new HomeControl(o.getString("entity_id"),o.optString("name")));}}catch(Exception ignored){}
        return controls;
    }
    private Map<String,String> aliases(){Map<String,String> result=new HashMap<>();for(HomeControl c:legacyControls())result.put(c.id,c.name);return result;}
    List<String> favoriteIds(){List<String> ids=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("favorites","[]"));for(int i=0;i<a.length();i++)ids.add(a.getString(i));}catch(Exception ignored){}return ids;}
    List<HomeControl> favorites(){return HomeCatalog.favorites(favoriteIds(),controls());}
    void setFavoriteIds(Collection<String> selected){JSONArray a=new JSONArray();for(String id:new LinkedHashSet<>(selected))a.put(id);prefs.edit().putString("favorites",a.toString()).apply();}
    boolean hasQuickLayout(){return prefs.contains("quick_rows");}
    JSONArray quickRows(){try{return new JSONArray(prefs.getString("quick_rows","[]"));}catch(JSONException e){return new JSONArray();}}
    HomeQuickLayout quickLayout(){try{return new HomeQuickLayout(quickRows());}catch(Exception e){throw new IllegalStateException("Invalid saved layout",e);}}
    void setQuickRows(JSONArray rows) throws JSONException {new HomeQuickLayout(rows);prefs.edit().putString("quick_rows",rows.toString()).apply();}
    void importConfiguration(JSONObject config) throws Exception {
        String url=config.getString("url").replaceAll("/+$","");URI uri=new URI(url);
        if(!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath()==null||uri.getPath().isEmpty()))throw new IllegalArgumentException("Invalid server URL");
        String token=config.getString("token");if(token.length()<20||token.length()>4096||token.indexOf('\n')>=0||token.indexOf('\r')>=0)throw new IllegalArgumentException("Invalid token");
        JSONArray a=config.getJSONArray("controls"),normalized=new JSONArray();Set<String> seen=new HashSet<>();
        if(a.length()>4096)throw new IllegalArgumentException("Too many controls");
        for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);HomeControl c=new HomeControl(o.getString("entity_id"),o.optString("name"));if(!seen.add(c.id)||c.name.length()>80)throw new IllegalArgumentException("Invalid control");normalized.put(new JSONObject().put("entity_id",c.id).put("name",c.name));}
        JSONArray rows=config.optJSONArray("quick_rows");if(config.has("quick_rows")&&rows==null)throw new IllegalArgumentException("Invalid quick_rows");if(rows!=null)new HomeQuickLayout(rows);
        String encrypted=encrypt(token);
        SharedPreferences.Editor edit=prefs.edit().putString("url",url).putString("token",encrypted).putString("controls",normalized.toString());
        if(rows!=null)edit.putString("quick_rows",rows.toString());
        if(!prefs.contains("favorites"))edit.putString("favorites","[]");
        if(!edit.commit())throw new IOException("Cannot store configuration");
    }
    void refresh(Result result){if(closed)return;worker.execute(()->{Snapshot s=read();latest=s;main.post(()->{if(!closed)result.done(s);});});}
    void refreshCatalog(Result result){if(closed)return;worker.execute(()->{registryAttempt=0;Snapshot s=read();latest=s;main.post(()->{if(!closed)result.done(s);});});}
    void execute(HomeControl control,String action,double value,ActionResult result){
        if(closed)return;
        worker.execute(()->{
            boolean success=false;String message;
            try{
                if(controls().stream().noneMatch(c->c.id.equals(control.id)))throw new IllegalArgumentException("Unknown control");
                State state=latest.states.get(control.id);if(!latest.connected||state==null||!state.available())throw new IllegalStateException("Device unavailable");
                String resolvedAction=action;
                if("toggle".equals(action)){
                    if("climate".equals(control.domain())){
                        JSONObject entity=new JSONObject(request("GET","/api/states/"+control.id,null));
                        state=new State(entity.getString("state"),entity.optJSONObject("attributes"));
                    }
                    resolvedAction=control.powerAction(state.value);
                }
                HomeControl.Command command=control.command(resolvedAction,value);
                String data=command.data;
                if("temperature".equals(action)){
                    double target=HomeTemperature.nativeTarget(value,latest.temperatureUnit(state),state.attributes.optDouble("min_temp",Double.NaN),state.attributes.optDouble("max_temp",Double.NaN),state.attributes.optDouble("target_temp_step",Double.NaN));
                    data=new JSONObject(data).put("temperature",target).toString();
                }
                request("POST","/api/services/"+command.domain+"/"+command.service,data);
                latest=read();success=true;message="已发送，请以设备状态为准";
            }catch(Exception error){message=errorMessage(error);}
            final boolean ok=success;final String text=message;main.post(()->{if(!closed)result.done(ok,text);});
        });
    }
    private Snapshot read(){
        try{
            if(!configured())return new Snapshot(Collections.emptyMap(),"尚未连接 Home Assistant",false);
            JSONObject config=new JSONObject(request("GET","/api/config",null));JSONObject units=config.optJSONObject("unit_system");String temperatureUnit=units==null?"":units.optString("temperature","");
            JSONArray states=new JSONArray(request("GET","/api/states",null));Map<String,State> found=new HashMap<>();
            for(int i=0;i<states.length();i++){JSONObject e=states.getJSONObject(i);found.put(e.getString("entity_id"),new State(e.getString("state"),e.optJSONObject("attributes")));}
            if(System.currentTimeMillis()-registryAttempt>60000){
                registryAttempt=System.currentTimeMillis();
                try{HomeRegistry updated=HomeRegistry.fetch(prefs.getString("url",""),decrypt(prefs.getString("token","")));HomeCatalog discovered=updated.catalog(found,aliases());registry=updated;catalog=discovered;prefs.edit().putString("registry",updated.data.toString()).apply();catalogStatus="";}
                catch(Exception error){catalogStatus=registry==null?"设备目录未同步，请刷新":"目录更新失败，使用上次目录";}
            }
            if(registry!=null)catalog=registry.catalog(found,aliases());
            return new Snapshot(Collections.unmodifiableMap(found),"已连接 Home Assistant",true,temperatureUnit);
        }catch(Exception error){return new Snapshot(Collections.emptyMap(),errorMessage(error),false);}
    }
    private String request(String method,String path,String body) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(prefs.getString("url","")+path).openConnection();
        try{
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(4000);connection.setReadTimeout(6000);connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization","Bearer "+decrypt(prefs.getString("token","")));connection.setRequestProperty("Accept","application/json");
            if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");try(OutputStream out=connection.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
            int status=connection.getResponseCode();if(status<200||status>=300)throw new HttpFailure(status);
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Response too large");out.write(buffer,0,n);}return out.toString("UTF-8");}
        }finally{connection.disconnect();}
    }
    private static final class HttpFailure extends IOException {final int status;HttpFailure(int status){this.status=status;}}
    private static String errorMessage(Exception e){if(e instanceof HttpFailure){int s=((HttpFailure)e).status;return s==401||s==403?"连接授权已失效，请重新配置":"Home Assistant 暂时无法处理（"+s+"）";}return "暂时无法连接 Home Assistant";}
    private static javax.crypto.SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(KEY))return (javax.crypto.SecretKey)store.getKey(KEY,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return generator.generateKey();
    }
    private static String encrypt(String value) throws Exception {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);}
    private static String decrypt(String value) throws Exception {String[] parts=value.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}
    void close(){closed=true;worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
