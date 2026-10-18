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
    private View chip,panel,networkCard;
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
        if(screenOn){showChip();poll.run();if(!networkGate.isWaiting())boot();}else{hidePanel();removeChip();if(networkGate!=null)networkGate.cancel();hideNetworkCard();automaticLaunch=false;}
    }};
    LauncherOverlay(Service s){service=s;recents=new RecentApps(s);android.content.SharedPreferences initial=LauncherSupport.preferences(s);if(!initial.contains("native_status_mode"))initial.edit().putString("native_status_mode","smart").apply();if(!initial.getBoolean("follow_last_default_v1",false))initial.edit().putBoolean("follow_last_default_v1",true).putString("native_default_app","last").apply();String last=LauncherSupport.preferences(s).getString("last_native_app","");if(!last.isEmpty()){foreground=AppCatalog.index(last);if(!initial.contains("last_video_app"))initial.edit().putString("last_video_app",last).apply();}wm=(WindowManager)s.getSystemService(Context.WINDOW_SERVICE);screenOn=((PowerManager)s.getSystemService(Context.POWER_SERVICE)).isInteractive();
        usageWorker.execute(()->{long[] history=recents.readHistoricalUse();handler.post(()->{if(!closed)for(int i=0;i<history.length;i++)recents.record(i,history[i]);});});
        networkGate=new NetworkReadyGate(s,new NetworkReadyGate.Listener(){
            public void waiting(String value){showNetworkCard(value);showChip();}
            public void ready(int app){hideNetworkCard();launchNow(app);}
            public void failed(int app,String reason){completeAutomaticLaunch();showNetworkCard(reason+"，暂未打开 "+AppCatalog.NAMES[app]);}
        });
        IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_SCREEN_OFF);s.registerReceiver(screen,filter);
        handler.post(poll);
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
    private void removeChip(){if(chip!=null){try{wm.removeView(chip);}catch(RuntimeException ignored){}chip=null;statusStrip=null;}}
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
        LinearLayout root=new LinearLayout(service){@Override public boolean dispatchKeyEvent(KeyEvent e){
            if(e.getAction()==KeyEvent.ACTION_DOWN)cancelPendingLaunch();
            handler.removeCallbacks(dismiss);handler.postDelayed(dismiss,15000);
            if(e.getKeyCode()==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP)hidePanel();return true;}
            if(e.getKeyCode()==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_UP&&settingsFirst!=null)settingsFirst.requestFocus();return true;}
            return super.dispatchKeyEvent(e);
        }};
        root.setOrientation(1);root.setPadding(dp(32),dp(28),dp(32),dp(28));
        root.setBackground(new MenuBackdropDrawable());
        LinearLayout apps=new LinearLayout(service);apps.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams appRow=new LinearLayout.LayoutParams(-1,dp(70));root.addView(apps,appRow);
        LinearLayout options=new LinearLayout(service);options.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams optionRow=new LinearLayout.LayoutParams(-1,dp(62));optionRow.topMargin=dp(8);root.addView(options,optionRow);
        int[] order=recents.order(foreground);
        TextView[] states=new TextView[3];View[] appTiles=new View[order.length],settingTiles=new View[6];TextView[] values=new TextView[6];
        for(int position=0;position<order.length;position++){
            final int app=order[position];
            LinearLayout tile=menuTile();tile.setOrientation(LinearLayout.HORIZONTAL);tile.setPadding(dp(12),dp(6),dp(12),dp(6));
            ImageView icon=new ImageView(service);try{icon.setImageDrawable(service.getPackageManager().getApplicationIcon(AppCatalog.packageName(service,app)));}catch(Exception ignored){icon.setImageResource(R.drawable.lucide_app_window);}
            tile.addView(icon,new LinearLayout.LayoutParams(dp(32),dp(32)));
            LinearLayout words=new LinearLayout(service);words.setOrientation(1);words.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams wordLayout=new LinearLayout.LayoutParams(0,-1,1);wordLayout.leftMargin=dp(10);tile.addView(words,wordLayout);
            words.addView(menuText(AppCatalog.NAMES[app],14,Color.WHITE));
            TextView state=menuText(app==foreground?"正在使用":"正在使用".equals(runtime[app])?"后台驻留":runtime[app],10,0xffe3e6ea);words.addView(state);states[app]=state;
            tile.setOnClickListener(v->{if(app==foreground)hidePanel();else launch(app);});
            appTiles[position]=tile;LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(dp(184),-1);if(position>0)cell.leftMargin=dp(6);apps.addView(tile,cell);
        }
        int[] icons={R.drawable.lucide_app_window,R.drawable.lucide_power,R.drawable.lucide_activity,R.drawable.lucide_sliders_horizontal,R.drawable.lucide_cable,R.drawable.lucide_settings};
        String[] labels={"开机应用","开机启动","状态栏","FlClash","信号源","系统设置"};
        int[] settingOrder={4,0,1,2,3,5};
        for(int i=0;i<labels.length;i++){
            final int setting=settingOrder[i];LinearLayout tile=menuTile();tile.setOrientation(LinearLayout.HORIZONTAL);tile.setPadding(dp(8),dp(6),dp(8),dp(6));
            ImageView icon=new ImageView(service);icon.setImageResource(icons[setting]);tile.addView(icon,new LinearLayout.LayoutParams(dp(18),dp(18)));
            LinearLayout words=new LinearLayout(service);words.setOrientation(1);words.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams wordLayout=new LinearLayout.LayoutParams(0,-1,1);wordLayout.leftMargin=dp(7);tile.addView(words,wordLayout);
            TextView label=menuText(labels[setting],12,Color.WHITE);words.addView(label,new LinearLayout.LayoutParams(-1,-2));
            TextView value=menuText("",10,0xffe3e6ea);words.addView(value,new LinearLayout.LayoutParams(-1,-2));values[setting]=value;
            tile.setOnClickListener(v->settingAction(setting,values));settingTiles[i]=tile;
            LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-1,1);if(i>0)cell.leftMargin=dp(6);options.addView(tile,cell);
        }
        updateSettingValues(values);settingsFirst=settingTiles[0];
        for(int i=0;i<appTiles.length;i++){
            appTiles[i].setNextFocusLeftId(appTiles[(i+appTiles.length-1)%appTiles.length].getId());appTiles[i].setNextFocusRightId(appTiles[(i+1)%appTiles.length].getId());
            appTiles[i].setNextFocusDownId(settingTiles[0].getId());appTiles[i].setNextFocusUpId(appTiles[i].getId());
        }
        for(int i=0;i<settingTiles.length;i++){
            settingTiles[i].setNextFocusLeftId(settingTiles[(i+5)%6].getId());settingTiles[i].setNextFocusRightId(settingTiles[(i+1)%6].getId());
            settingTiles[i].setNextFocusUpId(appTiles[0].getId());settingTiles[i].setNextFocusDownId(settingTiles[i].getId());
        }
        panel=root;WindowManager.LayoutParams p=params(dp(644),dp(196),true);p.gravity=Gravity.LEFT|Gravity.CENTER_VERTICAL;p.x=0;p.y=0;
        // Keep the rest of the picture at its original brightness.
        p.flags&=~WindowManager.LayoutParams.FLAG_DIM_BEHIND;p.dimAmount=0;
        try{
            wm.addView(panel,p);if(openSettingsRow){settingsFirst.requestFocus();openSettingsRow=false;}else appTiles[0].requestFocus();handler.postDelayed(dismiss,15000);
            android.util.Log.i("HomeOverlay","menu shown current="+foreground+" first="+order[0]);
        }catch(RuntimeException e){panel=null;settingsFirst=null;android.util.Log.w("HomeOverlay","show failed "+e.getClass().getSimpleName());}
    }
    private GradientDrawable glass(boolean focused){
        GradientDrawable surface=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            focused?new int[]{0x557d8896,0x3d596574}:new int[]{Color.TRANSPARENT,Color.TRANSPARENT});
        surface.setCornerRadius(dp(8));return surface;
    }
    private LinearLayout menuTile(){
        LinearLayout tile=new LinearLayout(service);tile.setId(View.generateViewId());tile.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);tile.setFocusable(true);tile.setFocusableInTouchMode(true);tile.setClickable(true);
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
    }
    private void settingAction(int setting,TextView[] values){
        cancelPendingLaunch();android.content.SharedPreferences prefs=LauncherSupport.preferences(service);
        if(setting==0){String policy=prefs.getString("native_default_app","last");String next="last".equals(policy)?AppCatalog.IDS[0]:AppCatalog.index(policy)==2?"last":AppCatalog.IDS[AppCatalog.index(policy)+1];prefs.edit().putString("native_default_app",next).apply();}
        else if(setting==1)prefs.edit().putBoolean("native_boot_enabled",!prefs.getBoolean("native_boot_enabled",true)).apply();
        else if(setting==2){String mode=statusMode();prefs.edit().putString("native_status_mode","smart".equals(mode)?"on":"on".equals(mode)?"off":"smart").apply();showChip();}
        else if(setting==3){hidePanel();Intent in=service.getPackageManager().getLaunchIntentForPackage("com.follow.clash");if(in!=null)service.startActivity(in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        else if(setting==4){hidePanel();LauncherSupport.openSourcePopup(service);}
        else if(setting==5){hidePanel();service.startActivity(new Intent().setComponent(new ComponentName("com.xiaomi.mitv.settings","com.xiaomi.mitv.settings.entry.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        if(panel!=null)updateSettingValues(values);
    }
    private void hidePanel(){panelRequest++;menuLoading=false;openSettingsRow=false;handler.removeCallbacks(dismiss);if(panel!=null){try{wm.removeView(panel);}catch(RuntimeException ignored){}panel=null;settingsFirst=null;}}
    private void showNetworkCard(String message){
        if(networkCard!=null){networkLabel.setText(message);networkHint.setText(networkGate.isWaiting()?"网络就绪后自动打开 · 首页键可选择其他应用":"首页键选择应用或重试");return;}
        LinearLayout box=new LinearLayout(service);box.setOrientation(1);box.setPadding(dp(24),dp(18),dp(24),dp(18));box.setBackground(shape(0xf014171d,0xff42464e,14));
        networkLabel=text(message,17,0xfff0f2f6);box.addView(networkLabel);
        TextView hint=text(networkGate.isWaiting()?"网络就绪后自动打开 · 首页键可选择其他应用":"首页键选择应用或重试",11,0xffaab1bf);networkHint=hint;LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-2,-2);h.topMargin=dp(10);box.addView(hint,h);
        WindowManager.LayoutParams w=params(dp(440),-2,false);w.gravity=Gravity.CENTER;w.x=0;w.y=0;w.setTitle("轻幕 · 等待网络");
        networkCard=box;try{wm.addView(box,w);}catch(RuntimeException e){networkCard=null;}
    }
    private void hideNetworkCard(){if(networkCard!=null){try{wm.removeView(networkCard);}catch(RuntimeException ignored){}networkCard=null;networkLabel=null;networkHint=null;}}
    private void settings(){cancelPendingLaunch();if(panel==null){openSettingsRow=true;showPanel();}else if(settingsFirst!=null)settingsFirst.requestFocus();}
    private void showDialog(AlertDialog d){dialog=d;d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);d.show();}
    private TextView button(String label,Runnable action){TextView v=text(label,15,0xfff0f1f4);v.setGravity(Gravity.CENTER);focusable(v);v.setOnClickListener(w->action.run());return v;}
    private void focusable(View v){v.setId(View.generateViewId());v.setFocusable(true);v.setClickable(true);v.setBackground(shape(0xff22262d,0xff343942,10));v.setOnFocusChangeListener((view,focus)->view.setBackground(shape(focus?0xff41454d:0xff22262d,focus?0xffeed7b6:0xff343942,10)));}
    private TextView text(String value,int size,int color){TextView t=new TextView(service);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif-medium",0));return t;}
    private GradientDrawable shape(int fill,int border,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
    private int dp(int n){return Math.round(n*service.getResources().getDisplayMetrics().density);}
    void close(){closed=true;if(networkGate!=null)networkGate.close();hideNetworkCard();handler.removeCallbacksAndMessages(null);hidePanel();removeChip();if(dialog!=null)dialog.dismiss();dashboard.close();usageWorker.shutdownNow();try{service.unregisterReceiver(screen);}catch(RuntimeException ignored){}}
}
