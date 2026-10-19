package local.simple.tvoverlay;

import java.text.Collator;
import java.util.*;

/** Device and room identity come from HA; identical room names never merge different areas. */
final class HomeCatalog {
    static final class Device {
        final String id,name,roomId,roomName;final List<HomeControl> controls;
        Device(String id,String name,String roomId,String roomName){this(id,name,roomId,roomName,Collections.emptyList());}
        Device(String id,String name,String roomId,String roomName,List<HomeControl> controls){this.id=id;this.name=clean(name,"未命名设备");this.roomId=roomId;this.roomName=clean(roomName,"未分配房间");this.controls=Collections.unmodifiableList(new ArrayList<>(controls));}
    }
    static final class Room {
        final String id,name;final List<Device> devices;
        Room(String id,String name,List<Device> devices){this.id=id;this.name=name;this.devices=Collections.unmodifiableList(devices);}
    }
    final List<HomeControl> controls;final List<Room> rooms;final List<Device> devices;final int registeredDeviceCount;
    HomeCatalog(List<Device> registered,List<HomeControl> entities){
        Set<String> registeredIds=new HashSet<>();for(Device device:registered)registeredIds.add(device.id);registeredDeviceCount=registeredIds.size();
        Collator order=Collator.getInstance(Locale.CHINA);
        Map<String,Device> metadata=new LinkedHashMap<>();Map<String,List<HomeControl>> grouped=new HashMap<>();
        for(Device d:registered){String key=key(d.roomId,d.id);metadata.put(key,d);grouped.put(key,new ArrayList<>());}
        for(HomeControl c:entities){String key=key(c.roomId,c.deviceId);if(!metadata.containsKey(key))metadata.put(key,new Device(c.deviceId,c.deviceName,c.roomId,c.roomName));grouped.computeIfAbsent(key,k->new ArrayList<>()).add(c);}
        List<Device> all=new ArrayList<>();List<HomeControl> flat=new ArrayList<>();
        for(Map.Entry<String,Device> entry:metadata.entrySet()){
            Device d=entry.getValue();List<HomeControl> items=grouped.get(entry.getKey());
            items.sort((a,b)->{int primary=Boolean.compare(b.actionable(),a.actionable());return primary!=0?primary:order.compare(a.name,b.name);});
            Device device=new Device(d.id,d.name,d.roomId,d.roomName,items);all.add(device);flat.addAll(items);
        }
        all.sort((a,b)->order.compare(a.name,b.name));devices=Collections.unmodifiableList(all);controls=Collections.unmodifiableList(flat);
        Map<String,List<Device>> byRoom=new HashMap<>();Map<String,String> names=new HashMap<>();
        for(Device d:all){byRoom.computeIfAbsent(d.roomId,k->new ArrayList<>()).add(d);names.put(d.roomId,d.roomName);}
        List<Room> result=new ArrayList<>();for(String id:byRoom.keySet())result.add(new Room(id,names.get(id),byRoom.get(id)));
        result.sort((a,b)->{if(a.id.isEmpty()!=b.id.isEmpty())return a.id.isEmpty()?1:-1;int name=order.compare(a.name,b.name);return name!=0?name:a.id.compareTo(b.id);});rooms=Collections.unmodifiableList(result);
    }
    private static String key(String room,String device){return room+'\u0000'+device;}
    static String clean(String value,String fallback){if(value==null)return fallback;String text=value.replaceAll("[\\p{Cc}]","").trim();return text.isEmpty()?fallback:text;}
    static List<HomeControl> favorites(List<String> ids,List<HomeControl> controls){
        Map<String,HomeControl> index=new HashMap<>();for(HomeControl c:controls)index.put(c.id,c);
        List<HomeControl> result=new ArrayList<>();Set<String> seen=new HashSet<>();for(String id:ids){HomeControl c=index.get(id);if(c!=null&&seen.add(id))result.add(c);}return result;
    }
}
