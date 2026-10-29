package local.simple.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The HOME entry is a transparent relay; the service owns the overlay. */
public final class MainActivity extends Activity {
    private final Handler handler=new Handler();
    private int attempts;
    @Override public void onCreate(Bundle state){super.onCreate(state);relay(initialHomeForBoot(state));}
    private boolean initialHomeForBoot(Bundle state){
        Intent intent=getIntent();
        if(intent==null||!Intent.ACTION_MAIN.equals(intent.getAction())||!intent.hasCategory(Intent.CATEGORY_HOME))return false;
        int boot=android.provider.Settings.Global.getInt(getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,-1);
        android.content.SharedPreferences prefs=LauncherSupport.preferences(this);
        boolean first=boot>=0&&prefs.getInt("initial_home_relay_boot",-2)!=boot;
        // Record even a recreation. A later user-created HOME activity must not
        // acquire initial-bootstrap semantics after the first relay was seen.
        if(boot>=0)prefs.edit().putInt("initial_home_relay_boot",boot).apply();
        return first&&state==null&&!intent.getBooleanExtra("overlay_settings",false);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);attempts=0;relay(false);}
    private int requestId;
    private void relay(boolean firstHomeForBoot){
        final int request=++requestId;
        final boolean[] answered={false};
        android.os.ResultReceiver reply=new android.os.ResultReceiver(handler){
            @Override protected void onReceiveResult(int code,Bundle data){
                if(request!=requestId||answered[0]||isFinishing())return;answered[0]=true;
                if(code==1){boolean moved=moveTaskToBack(true);android.util.Log.i("HomeOverlay","HOME ready, moved="+moved);if(!moved)finish();}
                else fallback();
            }
        };
        // Send the framework parcel type across APKs, not this app's anonymous subclass.
        android.os.Parcel parcel=android.os.Parcel.obtain();reply.writeToParcel(parcel,0);parcel.setDataPosition(0);
        android.os.ResultReceiver remoteReply=android.os.ResultReceiver.CREATOR.createFromParcel(parcel);parcel.recycle();
        Intent command=new Intent("HOME")
            .setComponent(new android.content.ComponentName("local.simple.tvoverlay","local.simple.tvoverlay.OverlayRuntimeService"))
            .putExtra("settings",getIntent().getBooleanExtra("overlay_settings",false)).putExtra("reply",remoteReply)
            .putExtra("first_home_for_boot",firstHomeForBoot)
            .putExtra("home_boot_count",android.provider.Settings.Global.getInt(getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,-1))
            .putExtra("home_relay_elapsed",android.os.SystemClock.elapsedRealtime())
            .putExtra("core_secret",LauncherSupport.preferences(this).getString("core_secret",""));
        handler.postDelayed(()->{if(request==requestId&&!answered[0]&&!isFinishing())fallback();},5000);
        try{startForegroundService(command);}catch(RuntimeException error){answered[0]=true;fallback();}
    }
    private void fallback(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setBackgroundColor(0xff111318);
        TextView title=new TextView(this);title.setText("轻幕 · 贴片暂未启动");title.setTextColor(0xffeeeeee);title.setTextSize(24);root.addView(title);
        Button access=new Button(this);access.setText("允许贴片悬浮显示");access.setOnClickListener(v->{try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:local.simple.tvoverlay")));}catch(RuntimeException e){if(!LauncherSupport.openSystemSettings(this))android.widget.Toast.makeText(this,"无法打开授权页面，请从系统设置允许轻幕悬浮显示",android.widget.Toast.LENGTH_SHORT).show();}});root.addView(access);
        for(int i=0;i<AppCatalog.IDS.length;i++){final int app=i;Button b=new Button(this);b.setText(AppCatalog.NAMES[i]);b.setOnClickListener(v->AppCatalog.launch(this,app));root.addView(b);}
        setContentView(root);access.requestFocus();
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){moveTaskToBack(true);}
}
