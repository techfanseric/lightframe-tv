package local.simple.tvoverlay;

import java.io.IOException;
import java.util.*;
import org.json.*;

/** Read-only HA registry access with the same non-admin account as the state API. */
final class HomeRegistry {
    final JSONObject data;
    HomeRegistry(JSONObject data){this.data=data;}
    static HomeRegistry fetch(String url,String token) throws Exception {
        try(HomeSocket socket=new HomeSocket(url)){
            if(!"auth_required".equals(new JSONObject(socket.receive()).optString("type")))throw new IOException("Expected authentication");
            socket.send(new JSONObject().put("type","auth").put("access_token",token).toString());
            if(!"auth_ok".equals(new JSONObject(socket.receive()).optString("type")))throw new IOException("Registry authorization failed");
            JSONObject data=new JSONObject();String[] names={"areas","devices","entities"},types={"config/area_registry/list","config/device_registry/list","config/entity_registry/list"};
            String[][] fields={{"area_id","name"},{"id","name","name_by_user","area_id"},{"entity_id","device_id","area_id","name","original_name","disabled_by"}};
            for(int i=0;i<names.length;i++){
                socket.send(new JSONObject().put("id",i+1).put("type",types[i]).toString());JSONObject response=new JSONObject(socket.receive());
                if(response.optInt("id")!=i+1||!response.optBoolean("success"))throw new IOException("Registry read failed");
                JSONArray raw=response.getJSONArray("result"),clean=new JSONArray();if(raw.length()>20000)throw new IOException("Registry too large");
                for(int j=0;j<raw.length();j++){JSONObject source=raw.getJSONObject(j),item=new JSONObject();for(String key:fields[i])if(!source.isNull(key))item.put(key,source.get(key));clean.put(item);}data.put(names[i],clean);
            }
            return new HomeRegistry(data);
        }
    }
    HomeCatalog catalog(Map<String,HomeAssistantClient.State> states,Map<String,String> aliases) throws JSONException {
        Map<String,String> rooms=new HashMap<>();rooms.put("","未分配房间");
        JSONArray areas=data.getJSONArray("areas");for(int i=0;i<areas.length();i++){JSONObject a=areas.getJSONObject(i);rooms.put(a.getString("area_id"),a.getString("name"));}
        Map<String,HomeCatalog.Device> devices=new LinkedHashMap<>();JSONArray registered=data.getJSONArray("devices");
        for(int i=0;i<registered.length();i++){
            JSONObject d=registered.getJSONObject(i);String id=d.getString("id"),area=d.optString("area_id","");
            devices.put(id,new HomeCatalog.Device(id,d.optString("name_by_user",d.optString("name","未命名设备")),area,rooms.getOrDefault(area,"未分配房间")));
        }
        List<HomeControl> controls=new ArrayList<>();Set<String> seen=new HashSet<>();JSONArray entities=data.getJSONArray("entities");
        for(int i=0;i<entities.length();i++){
            JSONObject e=entities.getJSONObject(i);String id=e.getString("entity_id");HomeCatalog.Device device=devices.get(e.optString("device_id"));
            String area=e.optString("area_id",device==null?"":device.roomId),fallback=e.optString("name",e.optString("original_name",id));
            HomeAssistantClient.State state=states.get(id);String name=aliases.getOrDefault(id,state==null?fallback:state.attributes.optString("friendly_name",fallback));
            controls.add(new HomeControl(id,name,device==null?"entity:"+id:device.id,device==null?name:device.name,area,rooms.getOrDefault(area,"未分配房间"),e.isNull("disabled_by")));seen.add(id);
        }
        for(Map.Entry<String,HomeAssistantClient.State> entry:states.entrySet())if(!seen.contains(entry.getKey())){
            String id=entry.getKey(),name=aliases.getOrDefault(id,entry.getValue().attributes.optString("friendly_name",id));controls.add(new HomeControl(id,name));
        }
        return new HomeCatalog(new ArrayList<>(devices.values()),controls);
    }
}
