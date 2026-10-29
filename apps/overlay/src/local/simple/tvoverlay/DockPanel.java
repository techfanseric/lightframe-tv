package local.simple.tvoverlay;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Presentation only. Focus never starts playback or sends a smart-home command. */
final class DockPanel extends LinearLayout {
    static final class App {
        final String title, subtitle; final Drawable icon; final Runnable open;
        App(String title,String subtitle,Drawable icon,Runnable open){this.title=title;this.subtitle=subtitle;this.icon=icon;this.open=open;}
    }
    private final Runnable interaction,close;
    private final int tileWidth;
    private final FrameLayout details;
    private final Paint scrimPaint=new Paint();
    private final View settingsPanel,homePanel;
    private final View[] settingsControls,homeControls,dock;
    private final View settingsTile,homeTile;
    private int category; // 0: apps, 1: settings, 2: home
    private boolean ready;

    DockPanel(Context context,List<App> apps,View[] settings,View[] home,Runnable interaction,Runnable close){
        super(context);int count=apps.size()+2;int available=Math.min(dp(940),getResources().getDisplayMetrics().widthPixels-dp(24))-dp(48);tileWidth=Math.min(dp(116),(available-dp(16)*(count-1))/count);this.interaction=interaction;this.close=close;this.settingsControls=settings;this.homeControls=home;
        setId(R.id.lightframe_dock);setOrientation(VERTICAL);setPadding(dp(24),dp(80),dp(24),dp(42));
        setWillNotDraw(false);
        setClipChildren(false);setClipToPadding(false);
        details=new FrameLayout(context);
        LayoutParams detailLayout=new LayoutParams(tileWidth*count+dp(16)*(count-1),dp(50+52*Math.max((settings.length+3)/4,(home.length+3)/4)));detailLayout.gravity=Gravity.CENTER_HORIZONTAL;detailLayout.bottomMargin=dp(18);addView(details,detailLayout);
        settingsPanel=section("设置","↑ 选择控制   ↓ 返回应用栏",settings,4);
        homePanel=section("智能家居","↑ 选择设备   ↓ 返回应用栏",home,4);
        details.addView(settingsPanel,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));details.addView(homePanel,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER);row.setClipChildren(false);row.setClipToPadding(false);
        addView(row,new LayoutParams(-1,dp(68)));
        dock=new View[apps.size()+2];
        for(int i=0;i<apps.size();i++){
            App app=apps.get(i);View tile=appTile(app.title,app.subtitle,app.icon,appColor(app.title));dock[i]=tile;
            tile.setOnClickListener(v->app.open.run());bindCategory(tile,0);addDock(row,tile,i);
        }
        settingsTile=appTile("设置","",EntryIcons.settings(context),0xffa5b3c8);
        homeTile=appTile("智能家居","",EntryIcons.home(context),0xffb9742c);
        dock[apps.size()]=settingsTile;dock[apps.size()+1]=homeTile;
        bindCategory(settingsTile,1);bindCategory(homeTile,2);
        settingsTile.setOnClickListener(v->enterControls(1));homeTile.setOnClickListener(v->enterControls(2));
        addDock(row,settingsTile,apps.size());addDock(row,homeTile,apps.size()+1);
        for(int i=0;i<dock.length;i++){
            View tile=dock[i];tile.setNextFocusLeftId(dock[MenuNavigation.wrap(i,-1,dock.length)].getId());tile.setNextFocusRightId(dock[MenuNavigation.wrap(i,1,dock.length)].getId());
            tile.setNextFocusDownId(tile.getId());tile.setNextFocusUpId(tile.getId());
        }
        wireControls(settings,4,settingsTile);wireControls(home,4,homeTile);
        if(settings.length>0)settingsTile.setNextFocusUpId(settings[0].getId());
        if(home.length>0)homeTile.setNextFocusUpId(home[0].getId());
        ready=true;showCategory(0);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float top=Math.max(0,getHeight()-dp(210));
        scrimPaint.setShader(new LinearGradient(0,top,0,getHeight(),new int[]{0x00090c12,0xa6090c12,0xe6090c12},new float[]{0,.55f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(0,top,getWidth(),getHeight(),scrimPaint);
    }
    private void addDock(LinearLayout row,View tile,int index){LayoutParams cell=new LayoutParams(tileWidth,-1);if(index>0)cell.leftMargin=dp(16);row.addView(tile,cell);}
    private static int appColor(String title){if("YouTube".equals(title))return 0xfff5f4f1;if("Netflix".equals(title))return 0xff17171b;if("哔哩哔哩".equals(title))return 0xffed5188;return 0xff1b6773;}
    private View appTile(String title,String subtitle,Drawable icon,int color){
        LinearLayout tile=new LinearLayout(getContext());tile.setId(View.generateViewId());tile.setGravity(Gravity.CENTER);tile.setPadding(dp(16),dp(14),dp(16),dp(14));
        tile.setFocusable(true);tile.setFocusableInTouchMode(true);tile.setDefaultFocusHighlightEnabled(false);tile.setClickable(true);
        tile.setContentDescription(title+(subtitle.isEmpty()?"":" · "+subtitle));tile.setBackground(surface(color));
        ImageView image=new ImageView(getContext());image.setImageDrawable(icon);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(image,new LayoutParams(dp(60),dp(40)));return tile;
    }
    private void bindCategory(View tile,int target){tile.setOnFocusChangeListener((v,focused)->{
        v.animate().cancel();
        v.animate().scaleX(focused?1.12f:1f).scaleY(focused?1.12f:1f).setDuration(150).setInterpolator(new DecelerateInterpolator()).start();
        if(focused&&ready)showCategory(target);
    });}
    private void showCategory(int target){
        category=target;settingsPanel.setVisibility(target==1?VISIBLE:INVISIBLE);homePanel.setVisibility(target==2?VISIBLE:INVISIBLE);
        details.setVisibility(target==0?INVISIBLE:VISIBLE);
    }
    private void enterControls(int target){showCategory(target);View[] controls=target==1?settingsControls:homeControls;if(controls.length>0)controls[0].requestFocus();}
    void focusInitial(boolean settings){if(settings)settingsTile.requestFocus();else dock[0].requestFocus();}
    boolean controlsVisible(){return category!=0;}
    boolean homeVisible(){return category==2;}
    private View section(String title,String hint,View[] controls,int columns){
        LinearLayout box=new LinearLayout(getContext());box.setOrientation(VERTICAL);box.setPadding(dp(14),dp(12),dp(14),dp(14));
        GradientDrawable background=new GradientDrawable();background.setColor(0xdc181d25);background.setCornerRadius(dp(16));box.setBackground(background);
        LinearLayout header=new LinearLayout(getContext());header.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=label(title,16,0xfff6f7fa);name.setGravity(Gravity.LEFT);header.addView(name,new LayoutParams(0,-2,1));
        TextView guidance=label(hint,10,0xffcbd2dd);header.addView(guidance,new LayoutParams(-2,-2));box.addView(header,new LayoutParams(-1,dp(24)));
        for(int start=0;start<controls.length;start+=columns){
            LinearLayout row=new LinearLayout(getContext());int count=Math.min(columns,controls.length-start);
            LayoutParams rowLayout=new LayoutParams(-1,dp(46));rowLayout.topMargin=dp(6);box.addView(row,rowLayout);
            for(int column=0;column<count;column++){LayoutParams cell=new LayoutParams(0,-1,1);if(column>0)cell.leftMargin=dp(6);row.addView(controls[start+column],cell);}
            for(int column=count;column<columns;column++){LayoutParams cell=new LayoutParams(0,-1,1);cell.leftMargin=dp(6);row.addView(new android.widget.Space(getContext()),cell);}
        }
        return box;
    }
    private void wireControls(View[] controls,int columns,View owner){
        for(int i=0;i<controls.length;i++){
            View tile=controls[i];int rowStart=(i/columns)*columns,count=Math.min(columns,controls.length-rowStart),col=i-rowStart;
            tile.setNextFocusLeftId(controls[rowStart+MenuNavigation.wrap(col,-1,count)].getId());tile.setNextFocusRightId(controls[rowStart+MenuNavigation.wrap(col,1,count)].getId());
            tile.setNextFocusUpId(i>=columns?controls[i-columns].getId():tile.getId());
            tile.setNextFocusDownId(rowStart+columns<controls.length?controls[Math.min(rowStart+columns+col,controls.length-1)].getId():owner.getId());
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getAction()==KeyEvent.ACTION_DOWN)interaction.run();
        if(event.getKeyCode()==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)close.run();return true;}
        if(event.getKeyCode()==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_UP)settingsTile.requestFocus();return true;}
        return super.dispatchKeyEvent(event);
    }
    private TextView label(String value,int size,int color){TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));v.setGravity(Gravity.CENTER);v.setSingleLine();v.setEllipsize(android.text.TextUtils.TruncateAt.END);v.setShadowLayer(dp(2),0,dp(1),0xc0000000);return v;}
    private Drawable surface(int color){GradientDrawable tile=new GradientDrawable();tile.setColor(color);tile.setCornerRadius(dp(14));return tile;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
