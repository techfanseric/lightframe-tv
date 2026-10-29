package local.simple.tvoverlay;

import android.app.Service;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Native, small overlay windows. No video decoding, blur, or animation loop. */
final class LauncherOverlay {
    private final Service service;
    private final WindowManager wm;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final DashboardClient dashboard=new DashboardClient();
    private final java.util.concurrent.ExecutorService usageWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final java.util.concurrent.atomic.AtomicBoolean usageBusy=new java.util.concurrent.atomic.AtomicBoolean();
    private final RecentApps recents;
    private final HomeAssistantClient homeAssistant;
    private final SmartHomeUi smartHome;
    private final java.util.Map<String,TextView> homeStates=new java.util.HashMap<>();
    private boolean homeRefreshPending;
    private final Runnable homeRefresh=new Runnable(){public void run(){
        if(closed||panel==null||homeRefreshPending)return;
        homeRefreshPending=true;homeAssistant.refresh(snapshot->{homeRefreshPending=false;if(panel!=null){updateHomeStates();handler.postDelayed(this,5000);}});
    }};
    private View chip,clockChip,panel,networkCard;
    private TextView networkLabel,networkHint;
    private View settingsFirst;
    private boolean menuLoading,openSettingsRow;
    private int panelRequest;
    private NetworkReadyGate networkGate;
    private boolean automaticLaunch;
    private StatusStripView statusStrip;
    private int foreground=-1;
    private boolean screenOn=true,closed;
    private AlertDialog dialog;
    private final Runnable dismiss=()->hidePanel();
    private final Runnable homeTransition=()->{showChip();if(panel==null&&!menuLoading)showPanel();else hidePanel();};
    private final Runnable poll=new Runnable(){public void run(){
        if(closed||!screenOn)return;
        refreshForeground();showChip();
        updateDashboard();
        handler.postDelayed(this,5000);
    }};
    private void updateDashboard(){
        if(chip!=null)dashboard.readSnapshot(LauncherSupport.preferences(service).getString("core_secret",""),s->{
            if(statusStrip!=null)statusStrip.update(s);
        });
    }
    private final BroadcastReceiver screen=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        screenOn=Intent.ACTION_SCREEN_ON.equals(i.getAction());handler.removeCallbacks(poll);
        if(screenOn){showChip();poll.run();if(!networkGate.isWaiting())boot();}else{smartHome.dismiss();hidePanel();removeChip();if(networkGate!=null)networkGate.cancel();hideNetworkCard();automaticLaunch=false;}
    }};
    LauncherOverlay(Service s){service=s;recents=new RecentApps(s);homeAssistant=new HomeAssistantClient(s);smartHome=new SmartHomeUi(s,homeAssistant,this::updateHomeStates);android.content.SharedPreferences initial=LauncherSupport.preferences(s);if(!initial.contains("native_status_mode"))initial.edit().putString("native_status_mode","smart").apply();if(!initial.getBoolean("follow_last_default_v1",false))initial.edit().putBoolean("follow_last_default_v1",true).putString("native_default_app","last").apply();String last=LauncherSupport.preferences(s).getString("last_native_app","");if(!last.isEmpty()){foreground=AppCatalog.index(last);if(!initial.contains("last_video_app"))initial.edit().putString("last_video_app",last).apply();}wm=(WindowManager)s.getSystemService(Context.WINDOW_SERVICE);screenOn=((PowerManager)s.getSystemService(Context.POWER_SERVICE)).isInteractive();
        usageWorker.execute(()->{long[] history=recents.readHistoricalUse();handler.post(()->{if(!closed)for(int i=0;i<history.length;i++)recents.record(i,history[i]);});});
        networkGate=new NetworkReadyGate(s,new NetworkReadyGate.Listener(){
            public void waiting(String value){showNetworkCard(value);showChip();}
            public void ready(int app){hideNetworkCard();launchNow(app);}
            public void failed(int app,String reason){completeAutomaticLaunch();showNetworkCard(reason+"，暂未打开 "+AppCatalog.NAMES[app]);}
        });
        IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_SCREEN_OFF);s.registerReceiver(screen,filter);
        handler.post(poll);
        if(homeAssistant.configured())homeAssistant.refresh(snapshot->{if(!closed){updateHomeStates();android.util.Log.i("HomeOverlay","HA catalog connected="+snapshot.connected+" devices="+homeAssistant.catalog().registeredDeviceCount+" rooms="+homeAssistant.catalog().rooms.size()+" entities="+homeAssistant.controls().size()+" favorites="+homeAssistant.favorites().size()+" current="+foreground);}});
    }
    void foreground(String pkg){int app=AppCatalog.forPackage(pkg);
        if(app>=0){foreground=app;recents.record(app,System.currentTimeMillis());LauncherSupport.preferences(service).edit().putString("last_native_app",AppCatalog.IDS[app]).putString("last_video_app",AppCatalog.IDS[app]).apply();showChip();}
        else if(pkg!=null&&!pkg.equals(service.getPackageName())&&!pkg.equals("local.simple.tvhome")&&!pkg.equals("com.android.systemui")){
            foreground=-1;hidePanel();showChip();
        }
    }
    private long lastUsageRead,lastLaunchAt;
    private void refreshForeground(){
        if(closed||!usageBusy.compareAndSet(false,true))return;
        usageWorker.execute(()->{
            Integer found=null;long observedAt=0;
            try {
                android.app.usage.UsageStatsManager usage=(android.app.usage.UsageStatsManager)service.getSystemService(Context.USAGE_STATS_SERVICE);
                long now=System.currentTimeMillis();android.app.usage.UsageEvents events=usage.queryEvents(lastUsageRead==0?now-10*60*1000L:lastUsageRead,now);
                android.app.usage.UsageEvents.Event event=new android.app.usage.UsageEvents.Event();String last=null;
                while(events.hasNextEvent()){events.getNextEvent(event);String pkg=event.getPackageName();
                    if(event.getEventType()==android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND && pkg!=null
                        && !pkg.equals("local.simple.tvhome") && !pkg.equals(service.getPackageName()) && !pkg.equals("com.android.systemui")){last=pkg;observedAt=event.getTimeStamp();}
                }
                lastUsageRead=now-1000;if(last!=null)found=AppCatalog.forPackage(last);
            }catch(RuntimeException ignored){}
            final Integer result=found;final long observationTime=observedAt;usageBusy.set(false);
            handler.post(()->{if(closed||result==null||observationTime<lastLaunchAt)return;boolean changed=foreground!=result;foreground=result;
                if(foreground>=0){recents.record(foreground,observationTime);LauncherSupport.preferences(service).edit().putString("last_video_app",AppCatalog.IDS[foreground]).apply();}
                LauncherSupport.preferences(service).edit().putString("last_native_app",foreground<0?"":AppCatalog.IDS[foreground]).apply();
                showChip();
                if(changed&&panel!=null){hidePanel();showPanel();}
            });
        });
    }
    void home(boolean settings){
        // Xiaomi can dispatch HOME twice while bringing the existing HOME task forward.
        handler.removeCallbacks(homeTransition);
        smartHome.dismiss();
        if(dialog!=null){dialog.dismiss();dialog=null;}
        refreshForeground();
        if(settings){handler.postDelayed(this::settings,180);return;}
        if(!networkGate.isWaiting()&&boot())return;
        if(!networkGate.isWaiting())hideNetworkCard();
        handler.postDelayed(homeTransition,180);
    }
    boolean boot(){
        android.content.SharedPreferences p=LauncherSupport.preferences(service);
        int boot=android.provider.Settings.Global.getInt(service.getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,-1);
        if(boot>=0 && p.getInt("native_launch_boot",-2)!=boot && p.getBoolean("native_boot_enabled",true)){
            if(automaticLaunch)return true;
            automaticLaunch=true;launch(StartupChoice.resolve(p.getString("native_default_app","last"),p.getString("last_video_app",""),recents.latest(),AppCatalog.IDS));return true;
        }
        return false;
    }
    private void completeAutomaticLaunch(){
        if(automaticLaunch){int boot=android.provider.Settings.Global.getInt(service.getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,-1);
            LauncherSupport.preferences(service).edit().putInt("native_launch_boot",boot).apply();automaticLaunch=false;}
    }
    private void cancelPendingLaunch(){
        if(networkGate.isWaiting()){networkGate.cancel();completeAutomaticLaunch();hideNetworkCard();}
    }
    private void launch(int app){
        hidePanel();hideNetworkCard();
        if(app==0||app==2)networkGate.start(app);else{networkGate.cancel();launchNow(app);}
    }
    private void launchNow(int app){hidePanel();completeAutomaticLaunch();
        android.util.Log.i("HomeNetwork","launch app="+AppCatalog.IDS[app]);if(dialog!=null){dialog.dismiss();dialog=null;}
        if(AppCatalog.launch(service,app)){lastLaunchAt=System.currentTimeMillis();foreground=app;recents.record(app,lastLaunchAt);LauncherSupport.preferences(service).edit().putString("last_native_app",AppCatalog.IDS[app]).putString("last_video_app",AppCatalog.IDS[app]).apply();showChip();}
        else{Toast.makeText(service,AppCatalog.NAMES[app]+" 暂时无法打开",Toast.LENGTH_SHORT).show();showPanel();}
    }
    private WindowManager.LayoutParams params(int width,int height,boolean focus){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|(focus?0:WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE),android.graphics.PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.BOTTOM|Gravity.LEFT;p.x=dp(28);p.y=dp(focus?100:22);p.setTitle(focus?"轻幕 · 应用切换":"轻幕 · FlClash");return p;
    }
    private String statusMode(){
        String mode=LauncherSupport.preferences(service).getString("native_status_mode","smart");
        return "on".equals(mode)||"off".equals(mode)?mode:"smart";
    }
    private void showChip(){
        String mode=statusMode();
        if(closed||!screenOn||"off".equals(mode)||("smart".equals(mode)&&foreground==1)){removeChip();return;}
        if(clockChip==null){
            clockChip=new ClockStripView(service);WindowManager.LayoutParams clock=params(-2,dp(18),false);
            clock.gravity=Gravity.TOP|Gravity.LEFT;clock.x=dp(4);clock.y=dp(2);clock.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;clock.setTitle("轻幕 · 日期时间");
            try{wm.addView(clockChip,clock);}catch(RuntimeException ignored){clockChip=null;}
        }
        if(chip!=null)return;
        statusStrip=new StatusStripView(service);chip=statusStrip;
        WindowManager.LayoutParams p=params(dp(440),dp(18),false);
        p.gravity=Gravity.TOP|Gravity.RIGHT;p.x=dp(4);p.y=dp(2);
        p.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        p.width=Math.min(dp(440),service.getResources().getDisplayMetrics().widthPixels-dp(24));
        p.setTitle("轻幕 · 顶部网络状态");
        try{wm.addView(chip,p);}catch(RuntimeException e){chip=null;statusStrip=null;}
        if(chip!=null)updateDashboard();
    }
    private void removeChip(){if(clockChip!=null){try{wm.removeView(clockChip);}catch(RuntimeException ignored){}clockChip=null;}if(chip!=null){try{wm.removeView(chip);}catch(RuntimeException ignored){}chip=null;statusStrip=null;}}
    private void showPanel(){
        if(panel!=null||menuLoading)return;
        menuLoading=true;final int request=++panelRequest;
        usageWorker.execute(()->{String[] runtime=AppRuntimeState.read(service);handler.post(()->{
            if(closed||request!=panelRequest)return;
            menuLoading=false;int current=-1,count=0;
            for(int app=0;app<runtime.length;app++)if("正在使用".equals(runtime[app])){current=app;count++;}
            if(count==1&&foreground<0){foreground=current;lastLaunchAt=System.currentTimeMillis();recents.record(current,lastLaunchAt);LauncherSupport.preferences(service).edit().putString("last_native_app",AppCatalog.IDS[current]).putString("last_video_app",AppCatalog.IDS[current]).apply();showChip();}
            buildPanel(runtime);
        });});
    }
    private void buildPanel(String[] runtime){if(panel!=null)return;
        homeStates.clear();
        int[] order=recents.order(foreground);
        java.util.List<DockPanel.App> apps=new java.util.ArrayList<>();
        for(int app:order){
            android.graphics.drawable.Drawable icon;
            if(app==0)icon=service.getDrawable(R.drawable.youtube_icon);
            else if(app==1)icon=EntryIcons.bilibili(service);
            else if(app==2)icon=EntryIcons.netflix(service);
            else if(app==3)icon=EntryIcons.iptv(service);
            else try{icon=service.getPackageManager().getApplicationIcon(AppCatalog.packageName(service,app));}
            catch(Exception ignored){icon=service.getDrawable(R.drawable.phosphor_app_window_fill);}
            String caption=app==0?"TizenTube":"";
            apps.add(new DockPanel.App(app==0?"YouTube":AppCatalog.NAMES[app],caption,icon,()->{if(app==foreground)hidePanel();else launch(app);}));
        }
        int[] icons={R.drawable.phosphor_app_window_fill,R.drawable.phosphor_power_fill,R.drawable.phosphor_pulse_fill,R.drawable.phosphor_sliders_horizontal_fill,R.drawable.phosphor_plugs_connected_fill,R.drawable.phosphor_gear_six_fill,R.drawable.phosphor_house_line_fill,R.drawable.phosphor_terminal_window_fill};
        String[] labels={"开机应用","开机启动","状态栏","FlClash","信号源","系统设置","智能家居","ADB 调试"};
        int[] settingOrder={0,1,2,3,4,5,7};
        TextView[] values=new TextView[8];View[] settings=new View[settingOrder.length];
        for(int i=0;i<settingOrder.length;i++){
            final int setting=settingOrder[i];TextView value=menuText("",11,0xffd7dde6);values[setting]=value;
            settings[i]=controlTile(labels[setting],icons[setting],value,()->settingAction(setting,values));
        }
        values[6]=menuText("",11,0xffd7dde6);updateSettingValues(values);
        java.util.List<View> home=new java.util.ArrayList<>();
        java.util.List<HomeControl> favorites=homeAssistant.favorites();
        // Reserve one cell for the complete existing hub; no configured control is lost.
        for(HomeControl control:favorites.subList(0,Math.min(15,favorites.size()))){
            TextView state=menuText(SmartHomeUi.status(control,homeAssistant.snapshot()),11,0xffd7dde6);homeStates.put(control.id,state);
            home.add(controlTile(control.name,SmartHomeUi.icon(control),state,()->{hidePanel();smartHome.showControl(control);}));
        }
        TextView more=menuText(homeAssistant.configured()?"房间与全部设备":"连接 Home Assistant",11,0xffd7dde6);
        home.add(controlTile("全部设备",R.drawable.phosphor_house_line_fill,more,()->{hidePanel();smartHome.showHub();}));
        DockPanel root=new DockPanel(service,apps,settings,home.toArray(new View[0]),()->{
            cancelPendingLaunch();handler.removeCallbacks(dismiss);handler.postDelayed(dismiss,15000);
        },this::hidePanel);
        panel=root;settingsFirst=null;
        WindowManager.LayoutParams p=params(-1,-2,true);
        p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;p.x=0;p.y=0;
        // Bottom-only scrim fades upward; the video above the dock stays clear.
        p.flags&=~WindowManager.LayoutParams.FLAG_DIM_BEHIND;p.dimAmount=0;
        try{
            wm.addView(root,p);root.focusInitial(openSettingsRow);openSettingsRow=false;handler.postDelayed(dismiss,15000);
            handler.removeCallbacks(homeRefresh);homeRefresh.run();
            android.util.Log.i("HomeOverlay","menu shown current="+foreground+" first="+order[0]+" layout=dock");
        }catch(RuntimeException e){panel=null;android.util.Log.w("HomeOverlay","show failed "+e.getClass().getSimpleName());}
    }
    private View controlTile(String title,int iconId,TextView detail,Runnable action){
        LinearLayout tile=menuTile();tile.setOrientation(LinearLayout.HORIZONTAL);tile.setPadding(dp(9),dp(5),dp(9),dp(5));
        ImageView icon=new ImageView(service);icon.setImageResource(iconId);tile.addView(icon,new LinearLayout.LayoutParams(dp(20),dp(20)));
        LinearLayout words=new LinearLayout(service);words.setOrientation(LinearLayout.VERTICAL);words.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams body=new LinearLayout.LayoutParams(0,-1,1);body.leftMargin=dp(8);tile.addView(words,body);
        words.addView(menuText(title,12,0xfff6f7fa));words.addView(detail);
        tile.setOnClickListener(v->action.run());return tile;
    }
    private GradientDrawable glass(boolean focused){
        GradientDrawable surface=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            focused?new int[]{0xff4a5565,0xff3b4657}:new int[]{0x002c333e,0x002c333e});
        surface.setCornerRadius(dp(9));return surface;
    }
    private LinearLayout menuTile(){
        LinearLayout tile=new LinearLayout(service);tile.setId(View.generateViewId());tile.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);tile.setFocusable(true);tile.setFocusableInTouchMode(true);tile.setDefaultFocusHighlightEnabled(false);tile.setClickable(true);
        tile.setOnFocusChangeListener((view,focused)->view.setBackground(glass(focused)));
        tile.setBackground(glass(false));return tile;
    }
    private TextView menuText(String value,int size,int color){TextView label=text(value,size,color);label.setSingleLine();label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setShadowLayer(dp(1),0,dp(1),0xe6000000);return label;}
    private void updateSettingValues(TextView[] values){
        android.content.SharedPreferences prefs=LauncherSupport.preferences(service);
        String policy=prefs.getString("native_default_app","last");values[0].setText("last".equals(policy)?"跟随上次":AppCatalog.NAMES[AppCatalog.index(policy)]);
        values[1].setText(prefs.getBoolean("native_boot_enabled",true)?"开启":"关闭");
        String mode=statusMode();values[2].setText("on".equals(mode)?"常开":"off".equals(mode)?"常关":"智能");
        values[3].setText("打开");values[4].setText("切换");values[5].setText("打开");
        values[6].setText(homeAssistant.configured()?"设备与快捷项":"连接 HA");values[7].setText("直达开关页");
    }
    private void settingAction(int setting,TextView[] values){
        cancelPendingLaunch();android.content.SharedPreferences prefs=LauncherSupport.preferences(service);
        if(setting==0){String policy=prefs.getString("native_default_app","last");String next="last".equals(policy)?AppCatalog.IDS[0]:AppCatalog.index(policy)==AppCatalog.IDS.length-1?"last":AppCatalog.IDS[AppCatalog.index(policy)+1];prefs.edit().putString("native_default_app",next).apply();}
        else if(setting==1)prefs.edit().putBoolean("native_boot_enabled",!prefs.getBoolean("native_boot_enabled",true)).apply();
        else if(setting==2){String mode=statusMode();prefs.edit().putString("native_status_mode","smart".equals(mode)?"on":"on".equals(mode)?"off":"smart").apply();showChip();}
        else if(setting==3){hidePanel();Intent in=service.getPackageManager().getLaunchIntentForPackage("com.follow.clash");if(in!=null)service.startActivity(in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        else if(setting==4){hidePanel();LauncherSupport.openSourcePopup(service);}
        else if(setting==5){hidePanel();if(!LauncherSupport.openSystemSettings(service))android.widget.Toast.makeText(service,"无法打开系统设置，请使用电视遥控器的设置键",android.widget.Toast.LENGTH_SHORT).show();}
        else if(setting==6){hidePanel();smartHome.showHub();}
        else if(setting==7){hidePanel();if(!LauncherSupport.openAdbSettings(service))android.widget.Toast.makeText(service,"无法打开 ADB 设置，请从系统设置进入",android.widget.Toast.LENGTH_SHORT).show();}
        if(panel!=null)updateSettingValues(values);
    }
    private void updateHomeStates(){for(HomeControl control:homeAssistant.controls()){TextView label=homeStates.get(control.id);if(label!=null)label.setText(SmartHomeUi.status(control,homeAssistant.snapshot()));}}
    private void hidePanel(){panelRequest++;menuLoading=false;openSettingsRow=false;handler.removeCallbacks(dismiss);handler.removeCallbacks(homeRefresh);homeStates.clear();if(panel!=null){try{wm.removeView(panel);}catch(RuntimeException ignored){}panel=null;settingsFirst=null;}}
    private void showNetworkCard(String message){
        if(networkCard!=null){networkLabel.setText(message);networkHint.setText(networkGate.isWaiting()?"网络就绪后自动打开 · 首页键可选择其他应用":"首页键选择应用或重试");return;}
        LinearLayout box=new LinearLayout(service);box.setOrientation(1);box.setPadding(dp(24),dp(18),dp(24),dp(18));box.setBackground(shape(0xf014171d,0xff42464e,14));
        networkLabel=text(message,17,0xfff0f2f6);box.addView(networkLabel);
        TextView hint=text(networkGate.isWaiting()?"网络就绪后自动打开 · 首页键可选择其他应用":"首页键选择应用或重试",11,0xffaab1bf);networkHint=hint;LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-2,-2);h.topMargin=dp(10);box.addView(hint,h);
        WindowManager.LayoutParams w=params(dp(440),-2,false);w.gravity=Gravity.CENTER;w.x=0;w.y=0;w.setTitle("轻幕 · 等待网络");
        networkCard=box;try{wm.addView(box,w);}catch(RuntimeException e){networkCard=null;}
    }
    private void hideNetworkCard(){if(networkCard!=null){try{wm.removeView(networkCard);}catch(RuntimeException ignored){}networkCard=null;networkLabel=null;networkHint=null;}}
    private void settings(){cancelPendingLaunch();if(panel==null){openSettingsRow=true;showPanel();}else if(panel instanceof DockPanel)((DockPanel)panel).focusInitial(true);}
    private void showDialog(AlertDialog d){dialog=d;d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);d.show();}
    private TextView button(String label,Runnable action){TextView v=text(label,15,0xfff0f1f4);v.setGravity(Gravity.CENTER);focusable(v);v.setOnClickListener(w->action.run());return v;}
    private void focusable(View v){v.setId(View.generateViewId());v.setFocusable(true);v.setClickable(true);v.setBackground(shape(0xff22262d,0xff343942,10));v.setOnFocusChangeListener((view,focus)->view.setBackground(shape(focus?0xff41454d:0xff22262d,focus?0xffeed7b6:0xff343942,10)));}
    private TextView text(String value,int size,int color){TextView t=new TextView(service);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif-medium",0));return t;}
    private GradientDrawable shape(int fill,int border,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
    private int dp(int n){return Math.round(n*service.getResources().getDisplayMetrics().density);}
    void close(){closed=true;smartHome.close();homeAssistant.close();if(networkGate!=null)networkGate.close();hideNetworkCard();handler.removeCallbacksAndMessages(null);hidePanel();removeChip();if(dialog!=null)dialog.dismiss();dashboard.close();usageWorker.shutdownNow();try{service.unregisterReceiver(screen);}catch(RuntimeException ignored){}}
}
