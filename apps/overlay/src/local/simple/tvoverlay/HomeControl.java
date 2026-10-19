package local.simple.tvoverlay;

/** Deliberately small command vocabulary; no arbitrary services or templates from configuration. */
final class HomeControl {
    final String id,name,deviceId,deviceName,roomId,roomName;final boolean enabled;
    HomeControl(String id,String name){
        this(id,name,"entity:"+id,name,"","未分配房间",true);
    }
    HomeControl(String id,String name,String deviceId,String deviceName,String roomId,String roomName,boolean enabled){
        if(id==null||!id.matches("[a-z_]+\\.[a-z0-9_]+"))throw new IllegalArgumentException("Invalid entity");
        this.id=id;this.name=HomeCatalog.clean(name,id);this.deviceId=deviceId;this.deviceName=HomeCatalog.clean(deviceName,this.name);
        this.roomId=roomId;this.roomName=HomeCatalog.clean(roomName,"未分配房间");this.enabled=enabled;
    }
    String domain(){return id.substring(0,id.indexOf('.'));}
    boolean actionable(){return enabled&&ArraysHolder.DOMAINS.contains(domain());}
    private static final class ArraysHolder {static final java.util.Set<String> DOMAINS=new java.util.HashSet<>(java.util.Arrays.asList("climate","cover","fan","switch","light","vacuum"));}
    static boolean onlyPowerActions(java.util.List<String> actions){return actions.size()==2&&actions.contains("on")&&actions.contains("off");}
    String powerAction(String state){
        if(state==null||"unknown".equals(state)||"unavailable".equals(state))throw new IllegalArgumentException("Power state unavailable");
        String d=domain();
        if("climate".equals(d))return "off".equals(state)?"on":"off";
        if("fan".equals(d)||"switch".equals(d)||"light".equals(d))return "toggle";
        throw new IllegalArgumentException("Unsupported power control");
    }
    static final class Command {
        final String domain,service,data;
        Command(String domain,String service,String data){this.domain=domain;this.service=service;this.data=data;}
    }
    Command command(String action,double value){
        if(!actionable())throw new IllegalArgumentException("Read-only entity");
        String d=domain(),service,extra="";
        if("climate".equals(d)){
            if("off".equals(action))service="turn_off";
            else if("on".equals(action))service="turn_on";
            else if("temperature".equals(action)&&Double.isFinite(value)&&value>=5&&value<=40){service="set_temperature";extra=",\"temperature\":"+value;}
            else throw new IllegalArgumentException("Unsupported climate action");
        }else if("cover".equals(d)){
            if("open".equals(action))service="open_cover";else if("close".equals(action))service="close_cover";else if("stop".equals(action))service="stop_cover";else throw new IllegalArgumentException("Unsupported cover action");
        }else if("vacuum".equals(d)){
            if("start".equals(action))service="start";else if("pause".equals(action))service="pause";else if("dock".equals(action))service="return_to_base";else throw new IllegalArgumentException("Unsupported vacuum action");
        }else{
            if("on".equals(action))service="turn_on";else if("off".equals(action))service="turn_off";else if("toggle".equals(action))service="toggle";else throw new IllegalArgumentException("Unsupported power action");
        }
        return new Command(d,service,"{\"entity_id\":\""+id+"\""+extra+"}");
    }
}
