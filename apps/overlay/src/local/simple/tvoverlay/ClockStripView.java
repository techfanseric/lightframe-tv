package local.simple.tvoverlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** One minute-aligned update while attached; no seconds or permanent drawing loop. */
final class ClockStripView extends TextView {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean attached,registered;
    private final BroadcastReceiver changed=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){refresh();}};
    private final Runnable tick=()->refresh();
    ClockStripView(Context context){super(context);
        setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);setSingleLine(true);setTextColor(0xffffffff);setTextSize(9);setIncludeFontPadding(false);
        setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));setPadding(dp(1),0,dp(1),0);
    }
    @Override protected void onDraw(android.graphics.Canvas canvas){
        android.graphics.Paint fill=getPaint();fill.setColor(0xffffffff);
        android.graphics.Paint outline=new android.graphics.Paint(fill);outline.setColor(0x66000000);outline.setStyle(android.graphics.Paint.Style.STROKE);outline.setStrokeWidth(.9f*getResources().getDisplayMetrics().density);outline.setStrokeJoin(android.graphics.Paint.Join.ROUND);
        float baseline=(getHeight()-fill.ascent()-fill.descent())/2;
        String value=getText().toString();canvas.drawText(value,dp(1),baseline,outline);canvas.drawText(value,dp(1),baseline,fill);
    }
    private void refresh(){
        if(!attached)return;
        Date now=new Date();String time=new SimpleDateFormat("HH:mm",Locale.ROOT).format(now);
        String date=new SimpleDateFormat("M月d日 EEEE",Locale.SIMPLIFIED_CHINESE).format(now).replace("星期","周");
        setText(time+"   "+date);
        handler.removeCallbacks(tick);handler.postDelayed(tick,60000-Math.floorMod(System.currentTimeMillis(),60000)+30);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);filter.addAction(Intent.ACTION_DATE_CHANGED);filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        getContext().registerReceiver(changed,filter);registered=true;refresh();
    }
    @Override protected void onDetachedFromWindow(){attached=false;handler.removeCallbacks(tick);if(registered){getContext().unregisterReceiver(changed);registered=false;}super.onDetachedFromWindow();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
