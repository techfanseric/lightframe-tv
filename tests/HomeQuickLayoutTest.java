package local.simple.tvoverlay;

import org.json.*;
import java.util.*;

public final class HomeQuickLayoutTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    private static void rejects(String json) throws Exception {try{new HomeQuickLayout(new JSONArray(json));}catch(IllegalArgumentException|JSONException expected){return;}throw new AssertionError("Invalid layout accepted");}
    public static void main(String[] args) throws Exception {
        HomeQuickLayout layout=new HomeQuickLayout(new JSONArray("[{name:'Living',groups:[{name:'Sofa',entity_id:'light.sofa',details:['light.sofa','light.left','light.right']}]},{name:'Dining',visible:false,groups:[{name:'Table',entity_id:'light.table',visible:false,open_details:true}]}]"));
        check(layout.spaces.size()==2&&layout.spaces.get(0).name.equals("Living")&&!layout.spaces.get(1).visible);
        HomeQuickLayout.Group group=layout.spaces.get(0).groups.get(0);
        check(group.details.equals(Arrays.asList("light.sofa","light.left","light.right")));
        check(!layout.spaces.get(1).groups.get(0).visible);
        check(!group.openDetails&&layout.spaces.get(1).groups.get(0).openDetails);
        HomeControl disabled=new HomeControl("light.sofa","Old name","device","Device","area","Room",false);
        HomeControl control=group.control(new HomeCatalog(Collections.emptyList(),Arrays.asList(disabled)));
        check(control.name.equals("Sofa")&&!control.actionable()&&control.roomId.equals("area"));
        HomeControl missing=group.control(new HomeCatalog(Collections.emptyList(),Collections.emptyList()));
        check(missing.id.equals("light.sofa")&&missing.command("toggle",0).data.equals("{\"entity_id\":\"light.sofa\"}"));
        check(new HomeQuickLayout(new JSONArray()).spaces.isEmpty());
        rejects("[{name:'',groups:[]}]");rejects("[{name:'Room',groups:[{name:'X',entity_id:'light.bad/../'}]}]");
        rejects("[{name:'Room',groups:[{name:'X',entity_id:'light.ok',details:['bad']}]}]");
        rejects("[{name:'Room'}]");
        System.out.println("Space layout validation, group targeting, visibility and disabled-state checks passed");
    }
}
