package local.simple.tvoverlay;

import android.app.AlertDialog;
import android.content.Context;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.widget.Toast;
import java.util.*;

/** Native remote-friendly controls in overlay windows, leaving the current input underneath. */
final class SmartHomeUi {
    private final Context context;private final HomeAssistantClient client;private final Runnable changed;
    private AlertDialog current;private boolean closed;private int request;
    SmartHomeUi(Context context,HomeAssistantClient client,Runnable changed){this.context=context;this.client=client;this.changed=changed;}
    static int icon(HomeControl c){switch(c.domain()){
        case "climate":return R.drawable.phosphor_thermometer_fill;case "cover":return R.drawable.phosphor_columns_fill;
        case "fan":return R.drawable.phosphor_fan_fill;case "vacuum":return R.drawable.phosphor_robot_fill;case "light":return R.drawable.phosphor_lightbulb_fill;
        default:return c.id.contains("airfresh")?R.drawable.phosphor_fan_fill:R.drawable.phosphor_house_line_fill;
    }}
    static String status(HomeControl c,HomeAssistantClient.Snapshot snapshot){
        if(!c.enabled)return "HA 中已禁用";
        if(!snapshot.connected)return snapshot.message;
        HomeAssistantClient.State state=snapshot.states.get(c.id);if(state==null)return "暂未加载";if(!state.available())return "暂不可用";
        String v=state.value;
        if("climate".equals(c.domain())){
            String label="off".equals(v)?"已关闭":"cool".equals(v)?"制冷":"heat".equals(v)?"制热":"auto".equals(v)?"自动":"fan_only".equals(v)?"送风":"dry".equals(v)?"除湿":v;
            double temperature=state.attributes.optDouble("temperature",Double.NaN);
            try{temperature=HomeTemperature.celsius(temperature,snapshot.temperatureUnit(state));}catch(IllegalArgumentException ignored){temperature=Double.NaN;}
            return !"off".equals(v)&&Double.isFinite(temperature)?label+" · "+number(Math.rint(temperature*10)/10)+"°C":label;
        }
        if("cover".equals(c.domain()))return "open".equals(v)?"已打开":"closed".equals(v)?"已关闭":"opening".equals(v)?"打开中":"closing".equals(v)?"关闭中":v;
        if("on".equals(v))return "已开启";if("off".equals(v))return "已关闭";
        if("docked".equals(v))return "已回充";if("cleaning".equals(v))return "清扫中";if("paused".equals(v))return "已暂停";if("returning".equals(v))return "回充中";
        return HomeCatalog.clean(v,"")+state.attributes.optString("unit_of_measurement","");
    }
    private static String number(double n){return n==Math.rint(n)?Long.toString(Math.round(n)):Double.toString(n);}
    void showHub(){reload(false);}
    private void reload(boolean picking){
        if(closed)return;final int generation=++request;
        showRooms(picking);
        client.refreshCatalog(snapshot->{if(closed||generation!=request)return;showRooms(picking);changed.run();});
    }
    private void showRooms(boolean picking){
        List<HomeCatalog.Room> rooms=client.catalog().rooms;String[] names=new String[rooms.size()];
        for(int i=0;i<names.length;i++){HomeCatalog.Room r=rooms.get(i);names[i]=r.name+"    "+r.devices.size()+" 项";}
        String title=picking?"选择常用设备 · 按房间":"智能家居 · 按房间";
        if(!client.catalogStatus().isEmpty())title+=" · "+client.catalogStatus();
        AlertDialog.Builder builder=new AlertDialog.Builder(context).setTitle(title).setNegativeButton("关闭",null)
            .setPositiveButton("刷新",(d,w)->reload(picking)).setNeutralButton(picking?"查看设备":"选择常用设备",(d,w)->{request++;showRooms(!picking);});
        if(rooms.isEmpty())builder.setMessage(client.configured()?"正在同步设备和房间，请稍后刷新。":"尚未连接 Home Assistant，请先配置连接。");
        else builder.setItems(names,(d,i)->showRoom(rooms.get(i),picking));
        show(builder.create());
    }
    private void showRoom(HomeCatalog.Room room,boolean picking){
        request++;String[] names=new String[room.devices.size()];
        for(int i=0;i<names.length;i++){HomeCatalog.Device device=room.devices.get(i);long available=device.controls.stream().filter(c->c.enabled&&client.snapshot().connected&&client.snapshot().states.containsKey(c.id)&&client.snapshot().states.get(c.id).available()).count();names[i]=device.name+"    "+(device.controls.isEmpty()?"暂无实体":available==0?"暂不可用":available+" 项可用");}
        Runnable back=()->{request++;showRooms(picking);};
        show(new AlertDialog.Builder(context).setTitle(room.name+(picking?" · 选择常用设备":"")).setItems(names,(d,i)->{if(picking)chooseFavorites(room,room.devices.get(i));else showDevice(room,room.devices.get(i));}).setNegativeButton("返回",(d,w)->back.run()).create(),back);
    }
    private String itemName(HomeCatalog.Device device,HomeControl control){String name=control.name;if(name.startsWith(device.name)&&name.length()>device.name.length())name=name.substring(device.name.length()).trim();return name;}
    private void showDevice(HomeCatalog.Room room,HomeCatalog.Device device){
        request++;String[] names=new String[device.controls.size()];
        for(int i=0;i<names.length;i++){HomeControl c=device.controls.get(i);names[i]=itemName(device,c)+"    "+status(c,client.snapshot())+(c.actionable()?"":" · 仅查看");}
        Runnable back=()->showRoom(room,false);
        AlertDialog.Builder builder=new AlertDialog.Builder(context).setTitle(device.name).setNegativeButton("返回",(d,w)->back.run());
        if(names.length==0)builder.setMessage("HA 已登记此设备，暂时没有可显示的实体。");
        else builder.setItems(names,(d,i)->showControl(device.controls.get(i),()->showDevice(room,device))).setNeutralButton("选择快捷项",(d,w)->chooseFavorites(room,device));
        show(builder.create(),back);
    }
    private void chooseFavorites(HomeCatalog.Room room,HomeCatalog.Device device){
        request++;List<HomeControl> controls=device.controls;Set<String> ids=new LinkedHashSet<>(client.favoriteIds());
        boolean[] checked=new boolean[controls.size()];String[] names=new String[controls.size()];for(int i=0;i<names.length;i++){names[i]=itemName(device,controls.get(i));checked[i]=ids.contains(controls.get(i).id);}
        int capacity=MenuNavigation.quickCapacity(Math.round(context.getResources().getDisplayMetrics().heightPixels/context.getResources().getDisplayMetrics().density));
        Runnable back=()->showRoom(room,true);
        AlertDialog dialog=new AlertDialog.Builder(context).setTitle(device.name+" · 快捷项（最多 "+capacity+" 项）").setMultiChoiceItems(names,checked,(d,i,value)->{
            checked[i]=value;Set<String> candidate=new LinkedHashSet<>(ids);for(int j=0;j<checked.length;j++){if(checked[j])candidate.add(controls.get(j).id);else candidate.remove(controls.get(j).id);}
            if(HomeCatalog.favorites(new ArrayList<>(candidate),client.controls()).size()>capacity){checked[i]=false;((AlertDialog)d).getListView().setItemChecked(i,false);Toast.makeText(context,"快捷行最多显示 "+capacity+" 项，请先取消其他常用项",Toast.LENGTH_SHORT).show();}
        }).setPositiveButton("保存",(d,w)->{
            for(int i=0;i<checked.length;i++){if(checked[i])ids.add(controls.get(i).id);else ids.remove(controls.get(i).id);}client.setFavoriteIds(ids);changed.run();back.run();
        }).setNegativeButton("返回",(d,w)->back.run()).create();
        show(dialog,back);
    }
    void showControl(HomeControl control){showControl(control,null);}
    private void showControl(HomeControl control,Runnable back){
        dismiss();HomeAssistantClient.Snapshot snapshot=client.snapshot();HomeAssistantClient.State state=snapshot.states.get(control.id);
        if(!control.actionable()){
            show(new AlertDialog.Builder(context).setTitle(control.name).setMessage(status(control,snapshot)+"\n\n"+(control.enabled?"此项在电视上仅显示状态。":"请在 Home Assistant 中启用后刷新。"))
                .setNegativeButton(back==null?"关闭":"返回",(d,w)->{if(back!=null)back.run();}).create(),back);return;
        }
        if(!snapshot.connected||state==null||!state.available()){
            show(new AlertDialog.Builder(context).setTitle(control.name).setMessage(status(control,snapshot)).setPositiveButton("重试",(d,w)->{dismiss();final int generation=request;client.refresh(s->{changed.run();if(closed||generation!=request)return;showControl(control,back);});}).setNegativeButton(back==null?"关闭":"返回",(d,w)->{if(back!=null)back.run();}).create(),back);return;
        }
        List<String> labels=new ArrayList<>(),actions=new ArrayList<>();List<Double> values=new ArrayList<>();
        switch(control.domain()){
            case "climate":
                add(labels,actions,values,"开启","on",0);add(labels,actions,values,"关闭","off",0);
                try{
                    String unit=snapshot.temperatureUnit(state);double target=Math.rint(HomeTemperature.celsius(state.attributes.optDouble("temperature",Double.NaN),unit));
                    if(!"off".equals(state.value)&&Double.isFinite(target))for(int delta:new int[]{1,-1}){
                        try{HomeTemperature.nativeTarget(target+delta,unit,state.attributes.optDouble("min_temp",Double.NaN),state.attributes.optDouble("max_temp",Double.NaN),state.attributes.optDouble("target_temp_step",Double.NaN));add(labels,actions,values,(delta>0?"温度调高至 ":"温度调低至 ")+number(target+delta)+"°C","temperature",target+delta);}catch(IllegalArgumentException ignored){}
                    }
                }catch(IllegalArgumentException ignored){}break;
            case "cover":add(labels,actions,values,"打开","open",0);add(labels,actions,values,"停止","stop",0);add(labels,actions,values,"关闭","close",0);break;
            case "vacuum":add(labels,actions,values,"开始清扫","start",0);add(labels,actions,values,"暂停清扫","pause",0);add(labels,actions,values,"返回充电","dock",0);break;
            default:add(labels,actions,values,"开启","on",0);add(labels,actions,values,"关闭","off",0);
        }
        if(HomeControl.onlyPowerActions(actions)){
            perform(control,"toggle",0,back);return;
        }
        show(new AlertDialog.Builder(context).setTitle(control.name+" · "+status(control,snapshot)).setItems(labels.toArray(new String[0]),(d,i)->{
            perform(control,actions.get(i),values.get(i),back);
        }).setNegativeButton(back==null?"关闭":"返回",(d,w)->{if(back!=null)back.run();}).create(),back);
    }
    private void perform(HomeControl control,String action,double value,Runnable back){dismiss();final int generation=request;client.execute(control,action,value,(ok,message)->{if(closed)return;Toast.makeText(context,message,Toast.LENGTH_SHORT).show();changed.run();if(generation==request&&back!=null)back.run();});}
    private static void add(List<String> labels,List<String> actions,List<Double> values,String label,String action,double value){labels.add(label);actions.add(action);values.add(value);}
    private void show(AlertDialog dialog){show(dialog,null);}
    private void show(AlertDialog dialog,Runnable back){
        if(closed)return;AlertDialog previous=current;current=null;if(previous!=null)previous.dismiss();current=dialog;
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        if(back!=null)dialog.setOnKeyListener((d,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP)back.run();return true;});
        dialog.setOnDismissListener(d->{if(current==dialog){current=null;request++;}});
        dialog.show();dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);dialog.getWindow().setDimAmount(0);
    }
    void dismiss(){request++;if(current!=null){current.dismiss();current=null;}}
    void close(){closed=true;dismiss();}
}
