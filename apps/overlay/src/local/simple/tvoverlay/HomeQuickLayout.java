package local.simple.tvoverlay;

import org.json.*;
import java.util.*;

/** Ordered spaces and HA group entities; personal presets live only in private preferences. */
final class HomeQuickLayout {
    static final class Group {
        final String name,entityId;final List<String> details;final boolean visible,openDetails;
        Group(JSONObject o) throws JSONException {
            name=label(o.getString("name"));entityId=o.getString("entity_id");new HomeControl(entityId,name);
            visible=o.optBoolean("visible",true);openDetails=o.optBoolean("open_details",false);List<String> ids=new ArrayList<>();ids.add(entityId);
            JSONArray a=o.optJSONArray("details");if(a!=null)for(int i=0;i<a.length();i++){String id=a.getString(i);new HomeControl(id,id);if(!ids.contains(id))ids.add(id);}
            if(ids.size()>128)throw new IllegalArgumentException("Too many group details");details=Collections.unmodifiableList(ids);
        }
        HomeControl control(HomeCatalog catalog){for(HomeControl c:catalog.controls)if(c.id.equals(entityId))return new HomeControl(c.id,name,c.deviceId,c.deviceName,c.roomId,c.roomName,c.enabled);return new HomeControl(entityId,name);}
    }
    static final class Space {
        final String name;final boolean visible;final List<Group> groups;
        Space(JSONObject o) throws JSONException {name=label(o.getString("name"));visible=o.optBoolean("visible",true);List<Group> items=new ArrayList<>();JSONArray a=o.getJSONArray("groups");if(a.length()>64)throw new IllegalArgumentException("Too many groups");for(int i=0;i<a.length();i++)items.add(new Group(a.getJSONObject(i)));groups=Collections.unmodifiableList(items);}
    }
    final List<Space> spaces;
    HomeQuickLayout(JSONArray data) throws JSONException {if(data.length()>32)throw new IllegalArgumentException("Too many spaces");List<Space> result=new ArrayList<>();for(int i=0;i<data.length();i++)result.add(new Space(data.getJSONObject(i)));spaces=Collections.unmodifiableList(result);}
    private static String label(String s){String value=HomeCatalog.clean(s,"");if(value.isEmpty()||value.length()>80)throw new IllegalArgumentException("Invalid name");return value;}
}
