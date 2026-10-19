package local.simple.tvoverlay;

/** Run with a plain JDK. Validates navigation boundaries and the permitted HA command vocabulary. */
public final class SmartHomeLogicTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Expected rejection");}
    public static void main(String[] args){
        for(int count=1;count<=8;count++)for(int column=0;column<count;column++){
            check(MenuNavigation.wrap(MenuNavigation.wrap(column,1,count),-1,count)==column);
            check(MenuNavigation.wrap(column,count,count)==column);
            for(int other=1;other<=8;other++){int target=MenuNavigation.adjacentColumn(column,count,other);check(target>=0&&target<other);if(count==other)check(target==column);}
        }
        check(MenuNavigation.wrap(0,-1,7)==6);check(MenuNavigation.wrap(6,1,7)==0);
        check(MenuNavigation.quickRows(0,3)==0);check(MenuNavigation.quickRows(8,3)==2);check(MenuNavigation.quickRows(9,3)==3);check(MenuNavigation.quickRows(32,2)==2);
        HomeControl air=new HomeControl("climate.bedroom","主卧空调");check(air.command("off",0).service.equals("turn_off"));check(air.command("on",0).service.equals("turn_on"));
        HomeControl.Command set=air.command("temperature",26.5);check(set.domain.equals("climate"));check(set.service.equals("set_temperature"));check(set.data.equals("{\"entity_id\":\"climate.bedroom\",\"temperature\":26.5}"));
        rejects(()->air.command("temperature",Double.NaN));rejects(()->air.command("temperature",45));rejects(()->air.command("toggle",0));
        HomeControl cover=new HomeControl("cover.west","西窗帘");check(cover.command("stop",0).service.equals("stop_cover"));check(cover.command("close",0).service.equals("close_cover"));rejects(()->cover.command("off",0));
        HomeControl vacuum=new HomeControl("vacuum.robot","扫地机器人");check(vacuum.command("dock",0).service.equals("return_to_base"));rejects(()->vacuum.command("toggle",0));
        check(HomeControl.onlyPowerActions(java.util.Arrays.asList("on","off")));
        check(HomeControl.onlyPowerActions(java.util.Arrays.asList("off","on")));
        check(!HomeControl.onlyPowerActions(java.util.Arrays.asList("on","off","temperature")));
        check(!HomeControl.onlyPowerActions(java.util.Arrays.asList("open","stop","close")));
        check(!HomeControl.onlyPowerActions(java.util.Arrays.asList("start","pause","dock")));
        check(air.command(air.powerAction("off"),0).service.equals("turn_on"));
        for(String state:new String[]{"cool","heat","auto","fan_only","dry"})check(air.command(air.powerAction(state),0).service.equals("turn_off"));
        rejects(()->air.powerAction("unavailable"));rejects(()->air.powerAction("unknown"));rejects(()->cover.powerAction("open"));rejects(()->vacuum.powerAction("docked"));
        for(String domain:new String[]{"fan","switch","light"}){HomeControl power=new HomeControl(domain+".power","开关");HomeControl.Command toggle=power.command(power.powerAction("off"),0);check(toggle.domain.equals(domain)&&toggle.service.equals("toggle")&&toggle.data.equals("{\"entity_id\":\""+domain+".power\"}"));check(power.powerAction("on").equals("toggle"));rejects(()->power.command("dock",0));}
        HomeControl lock=new HomeControl("lock.door","门");check(!lock.actionable());rejects(()->lock.command("off",0));
        rejects(()->new HomeControl("switch.a\"}","bad"));rejects(()->new HomeControl("switch.a/../../services","bad"));
        check(Math.abs(HomeTemperature.celsius(79,"°F")-26.111111)<0.00001);
        check(HomeTemperature.nativeTarget(27,"°F",61,88,0.5)==80.5);
        check(HomeTemperature.nativeTarget(26,"°C",16,31,0.5)==26);
        rejects(()->HomeTemperature.nativeTarget(40,"°F",61,88,0.5));rejects(()->HomeTemperature.nativeTarget(26,"",16,31,1));
        HomeCatalog.Device bedroom=new HomeCatalog.Device("air","空调","bed","卧室"),other=new HomeCatalog.Device("remote-air","空调","other-bed","卧室"),empty=new HomeCatalog.Device("hub","网关","","未分配房间");
        HomeControl a=new HomeControl("climate.air","空调","air","空调","bed","卧室",true),sensor=new HomeControl("sensor.air","温度","air","空调","bed","卧室",true),remote=new HomeControl("climate.remote","空调","remote-air","空调","other-bed","卧室",true),disabled=new HomeControl("switch.disabled","禁用项","air","空调","bed","卧室",false);
        HomeCatalog catalog=new HomeCatalog(java.util.Arrays.asList(bedroom,other,empty),java.util.Arrays.asList(sensor,remote,a,disabled));
        check(catalog.registeredDeviceCount==3&&catalog.devices.size()==3&&catalog.controls.size()==4&&catalog.rooms.size()==3);
        check(catalog.rooms.get(2).id.isEmpty());check(!catalog.rooms.get(0).id.equals(catalog.rooms.get(1).id));
        HomeCatalog.Device grouped=catalog.devices.stream().filter(d->d.id.equals("air")).findFirst().get();check(grouped.controls.size()==3&&grouped.controls.get(0).id.equals(a.id));
        check(!sensor.actionable()&&!disabled.actionable());rejects(()->disabled.command("toggle",0));
        java.util.List<HomeControl> selected=HomeCatalog.favorites(java.util.Arrays.asList(remote.id,a.id,remote.id,"sensor.deleted"),catalog.controls);check(selected.size()==2&&selected.get(0)==remote&&selected.get(1)==a);
        check(HomeCatalog.favorites(java.util.Collections.emptyList(),catalog.controls).isEmpty());
        check(HomeCatalog.clean("\b家 客厅\n","").equals("家 客厅"));
        check(MenuNavigation.quickCapacity(1080)==12&&MenuNavigation.quickCapacity(350)==8&&MenuNavigation.quickCapacity(200)==0);
        System.out.println("Smart home navigation and command checks passed");
    }
}
