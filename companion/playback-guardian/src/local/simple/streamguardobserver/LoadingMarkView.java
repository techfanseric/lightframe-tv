package local.simple.streamguardobserver;

import android.animation.ValueAnimator;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** A silver trace around a small screen; one local animation, no full-screen repaint loop. */
final class LoadingMarkView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline=new Path();
    private final Matrix traceMatrix=new Matrix();
    private SweepGradient trace;
    private final boolean motion;
    private ValueAnimator animator;
    private float phase=.16f;
    private boolean attached,observing,busy=true;
    private final ContentObserver motionPreference=new ContentObserver(new Handler(Looper.getMainLooper())){
        @Override public void onChange(boolean selfChange){updateMotion();}
    };
    LoadingMarkView(Context context,boolean motion){
        super(context);this.motion=motion;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        outline.reset();float inset=dp(10);
        outline.addRoundRect(inset,inset,w-inset,h-inset,dp(20),dp(20),Path.Direction.CW);
        trace=new SweepGradient(w/2f,h/2f,new int[]{0x00e4e9f1,0x00e4e9f1,0x18e4e9f1,0x70e4e9f1,0xffe4e9f1,0x00e4e9f1},new float[]{0,.60f,.75f,.90f,.995f,1});
    }
    @Override protected void onDraw(Canvas canvas){
        paint.setStrokeWidth(dp(.8f));paint.setColor(0xff34373d);canvas.drawPath(outline,paint);
        if(trace==null)return;
        traceMatrix.setRotate(phase*360,getWidth()/2f,getHeight()/2f);trace.setLocalMatrix(traceMatrix);
        paint.setStrokeWidth(dp(1.25f));paint.setColor(Color.WHITE);paint.setShader(trace);
        canvas.drawPath(outline,paint);paint.setShader(null);
    }
    void setBusy(boolean busy){this.busy=busy;updateMotion();}
    private void updateMotion(){
        boolean run=busy&&attached&&isShown()&&getWindowVisibility()==VISIBLE&&motion&&ValueAnimator.areAnimatorsEnabled();
        if(run&&animator==null){animator=ValueAnimator.ofFloat(phase,phase+1);animator.setDuration(4600);animator.setInterpolator(new LinearInterpolator());animator.setRepeatCount(ValueAnimator.INFINITE);animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue()%1;invalidate();});animator.start();}
        else if(!run&&animator!=null){animator.cancel();animator=null;}
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;
        try{getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,motionPreference);observing=true;}catch(RuntimeException ignored){}
        updateMotion();
    }
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);updateMotion();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updateMotion();}
    @Override protected void onDetachedFromWindow(){attached=false;updateMotion();if(observing){getContext().getContentResolver().unregisterContentObserver(motionPreference);observing=false;}super.onDetachedFromWindow();}
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
}
