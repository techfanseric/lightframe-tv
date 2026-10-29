package local.simple.tvoverlay;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;

/** Small native motion on a quiet, opaque stage. No artwork decoding or video capture. */
final class StartupBackdropView extends FrameLayout {
    private final Runnable cancel,home;
    private final TextView status;
    StartupBackdropView(Context context,String theme,String appName,Drawable icon,Runnable cancel,Runnable home){
        super(context);this.cancel=cancel;this.home=home;
        setBackgroundColor(0xff111214);setFocusable(true);setFocusableInTouchMode(true);
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(context);
        mark.addView(new LoadingMarkView(context,!"still".equals(theme)),new LayoutParams(-1,-1));
        ImageView logo=new ImageView(context);logo.setImageDrawable(icon);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        mark.addView(logo,new LayoutParams(dp(48),dp(40),Gravity.CENTER));
        content.addView(mark,new LinearLayout.LayoutParams(dp(164),dp(104)));
        TextView title=text(22,0xfff0f1f3);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setText(appName);
        LinearLayout.LayoutParams titleLayout=new LinearLayout.LayoutParams(-2,-2);titleLayout.topMargin=dp(18);content.addView(title,titleLayout);
        LayoutParams center=new LayoutParams(dp(280),dp(236),Gravity.CENTER);center.bottomMargin=dp(48);addView(content,center);
        status=text(11,0xff9a9fa8);status.setSingleLine();status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams details=new LinearLayout.LayoutParams(dp(240),dp(24));details.topMargin=dp(6);content.addView(status,details);
        TextView hint=text(10,0xff8c919b);hint.setText("返回取消 · 首页选择应用");hint.setSingleLine();
        LinearLayout.LayoutParams actions=new LinearLayout.LayoutParams(dp(280),dp(20));actions.topMargin=dp(16);content.addView(hint,actions);
        setContentDescription(appName+"，加载中，按首页或返回取消");
    }
    void setStatus(String value){if(!value.contentEquals(status.getText()))status.setText(value);}
    private TextView text(int size,int color){TextView view=new TextView(getContext());view.setTextColor(color);view.setTextSize(size);view.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));view.setGravity(Gravity.CENTER);return view;}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int code=event.getKeyCode();if(code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_HOME){if(event.getAction()==KeyEvent.ACTION_UP)(code==KeyEvent.KEYCODE_HOME?home:cancel).run();return true;}
        return super.dispatchKeyEvent(event);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
