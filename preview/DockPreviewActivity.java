package local.simple.tvoverlay;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Isolated native presentation preview. Contains no networking, video launch or device control. */
public final class DockPreviewActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        FrameLayout stage=new FrameLayout(this);boolean bright=getIntent().getBooleanExtra("bright",false);stage.addView(new Scene(bright));
        TextView caption=new TextView(this);caption.setText("轻幕 / 原生界面预览 · 背景为示意画面");caption.setTextColor(bright?0xff182c37:0xffe3e7ec);caption.setTextSize(14);FrameLayout.LayoutParams note=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);note.leftMargin=dp(36);note.topMargin=dp(48);stage.addView(caption,note);
        ClockStripView clock=new ClockStripView(this);FrameLayout.LayoutParams clockLayout=new FrameLayout.LayoutParams(-2,dp(18),Gravity.TOP|Gravity.LEFT);clockLayout.leftMargin=dp(4);clockLayout.topMargin=dp(2);stage.addView(clock,clockLayout);
        List<DockPanel.App> apps=new ArrayList<>();
        apps.add(new DockPanel.App("YouTube","TizenTube",getDrawable(R.drawable.youtube_icon),()->clicked("youtube")));
        apps.add(new DockPanel.App("Netflix","",EntryIcons.netflix(this),()->clicked("netflix")));
        apps.add(new DockPanel.App("哔哩哔哩","",EntryIcons.bilibili(this),()->clicked("bilibili")));
        apps.add(new DockPanel.App("IPTV","",EntryIcons.iptv(this),()->clicked("iptv")));
        String[] settings={"开机应用","开机启动","状态栏","FlClash","信号源","系统设置","ADB 调试"};String[] values={"跟随上次","开启","智能","打开","切换","打开","直达开关页"};View[] settingsViews=new View[settings.length];
        for(int i=0;i<settings.length;i++)settingsViews[i]=control(settings[i],values[i]);
        View[] home={control("客厅灯","已开启"),control("空调","制冷 · 26°C"),control("窗帘","已关闭"),control("全部设备","房间与快捷项")};
        DockPanel dock=new DockPanel(this,apps,settingsViews,home,()->{},()->finish());
        FrameLayout.LayoutParams layout=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);stage.addView(dock,layout);setContentView(stage);dock.focusInitial(getIntent().getBooleanExtra("settings",false));
    }
    private void clicked(String value){android.util.Log.i("DockPreview","ACTION "+value);}
    private View control(String title,String subtitle){
        LinearLayout tile=new LinearLayout(this);tile.setOrientation(0);tile.setGravity(Gravity.CENTER_VERTICAL);tile.setPadding(dp(9),dp(5),dp(9),dp(5));tile.setId(View.generateViewId());tile.setFocusable(true);tile.setFocusableInTouchMode(true);tile.setDefaultFocusHighlightEnabled(false);tile.setClickable(true);style(tile,false);tile.setOnFocusChangeListener((v,f)->style(v,f));tile.setOnClickListener(v->clicked(title));
        String[] names={"开机应用","开机启动","状态栏","FlClash","信号源","系统设置","ADB 调试","客厅灯","空调","窗帘","全部设备"};
        int[] icons={R.drawable.phosphor_app_window_fill,R.drawable.phosphor_power_fill,R.drawable.phosphor_pulse_fill,R.drawable.phosphor_sliders_horizontal_fill,R.drawable.phosphor_plugs_connected_fill,R.drawable.phosphor_gear_six_fill,R.drawable.phosphor_terminal_window_fill,R.drawable.phosphor_lightbulb_fill,R.drawable.phosphor_thermometer_fill,R.drawable.phosphor_columns_fill,R.drawable.phosphor_house_line_fill};
        int icon=icons[0];for(int i=0;i<names.length;i++)if(names[i].equals(title))icon=icons[i];
        ImageView image=new ImageView(this);image.setImageResource(icon);tile.addView(image,new LinearLayout.LayoutParams(dp(20),dp(20)));
        LinearLayout words=new LinearLayout(this);words.setOrientation(1);LinearLayout.LayoutParams body=new LinearLayout.LayoutParams(0,-2,1);body.leftMargin=dp(8);tile.addView(words,body);
        for(String text:new String[]{title,subtitle}){TextView label=new TextView(this);label.setText(text);label.setTextColor(0xffedf1f6);label.setTextSize(text.equals(title)?12:11);label.setSingleLine();words.addView(label);}return tile;
    }
    private void style(View tile,boolean focus){GradientDrawable d=new GradientDrawable();d.setColor(focus?0xff485568:0x002c333e);d.setCornerRadius(dp(12));tile.setBackground(d);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private class Scene extends View {final boolean bright;final Paint p=new Paint(3);Scene(boolean value){super(DockPreviewActivity.this);bright=value;}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new LinearGradient(0,0,0,h,bright?new int[]{0xffafd5df,0xffd6dace,0xff8ca6a5}:new int[]{0xff152e3b,0xff3e6470,0xffb6a58b},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);p.setColor(bright?0xffe4e0c6:0xffd4c2a2);c.drawCircle(w*.74f,h*.23f,h*.063f,p);mountain(c,w,h,0.43f,bright?0xff7d999c:0xff496e77);mountain(c,w,h,0.56f,bright?0xff5f8588:0xff2c515c);p.setColor(bright?0xff87aaa9:0xff557d80);c.drawRect(0,h*.68f,w,h,p);p.setColor(bright?0xffe0e6db:0xff95ada3);p.setStrokeWidth(2);for(int i=0;i<15;i++){float y=h*(.73f+i*.013f);c.drawLine(w*.55f-i*18,y,w*.88f+i*6,y,p);}}void mountain(Canvas c,float w,float h,float base,int color){Path path=new Path();path.moveTo(-w*.1f,h*.8f);path.lineTo(w*.17f,h*(base+.02f));path.lineTo(w*.25f,h*(base+.13f));path.lineTo(w*.37f,h*(base-.1f));path.lineTo(w*.54f,h*(base+.2f));path.lineTo(w*.65f,h*(base+.11f));path.lineTo(w,h*.8f);path.close();p.setColor(color);c.drawPath(path,p);}}
}
