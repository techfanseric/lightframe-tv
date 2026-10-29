package local.simple.streamguardobserver;

import android.content.Context;
import android.graphics.*;
import android.view.Gravity;
import android.widget.*;

/** Same quiet loading language as the launcher, for the optional Netflix repair handoff. */
final class GuardBackdropView extends FrameLayout {
 private final LoadingMarkView mark;
 private final LinearLayout content;
 GuardBackdropView(Context context){
  super(context);setBackgroundColor(0xff111214);
  content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);
  FrameLayout frame=new FrameLayout(context);mark=new LoadingMarkView(context,true);frame.addView(mark,new LayoutParams(-1,-1));
  ImageView logo=new ImageView(context);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=8;options.inScaled=false;
  Bitmap source=BitmapFactory.decodeResource(getResources(),R.drawable.netflix_symbol_official,options);
  if(source!=null){int x=Math.round(source.getWidth()*247f/991),y=Math.round(source.getHeight()*451f/1801);int w=Math.round(source.getWidth()*496f/991),h=Math.round(source.getHeight()*900f/1801);logo.setImageBitmap(Bitmap.createBitmap(source,x,y,w,h));}
  frame.addView(logo,new LayoutParams(dp(48),dp(40),Gravity.CENTER));content.addView(frame,new LinearLayout.LayoutParams(dp(164),dp(104)));
  TextView title=new TextView(context);title.setText("Netflix");title.setTextSize(22);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setTextColor(0xfff0f1f3);title.setGravity(Gravity.CENTER);
  LinearLayout.LayoutParams label=new LinearLayout.LayoutParams(-2,-2);label.topMargin=dp(18);content.addView(title,label);
  LayoutParams center=new LayoutParams(dp(360),dp(276),Gravity.CENTER);center.bottomMargin=dp(28);addView(content,center);
 }
 void addStatusContent(android.view.View controls){content.addView(controls,new LinearLayout.LayoutParams(-2,-2));}
 void setBusy(boolean busy){mark.setBusy(busy);}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
