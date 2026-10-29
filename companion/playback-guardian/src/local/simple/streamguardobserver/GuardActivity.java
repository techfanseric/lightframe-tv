package local.simple.streamguardobserver;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.Gravity;
import android.widget.*;

/** Private transient wait page. It is never a HOME activity and never launches after losing the user's focus. */
public final class GuardActivity extends Activity {
 private final Handler handler=new Handler();private NetworkGate gate;private GuardBackdropView backdrop;private LinearLayout actions;private TextView status;private Button retry;private int token;private boolean resumed,launching;private long proxyGrace;
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);token=getIntent().getIntExtra("request",-1);if(!EntryObserver.requestValid(token)){finish();return;}
  FrameLayout root=new FrameLayout(this);backdrop=new GuardBackdropView(this);root.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);content.setPadding(dp(20),dp(6),dp(20),0);
  backdrop.addStatusContent(content);
  status=new TextView(this);status.setTextSize(11);status.setTextColor(0xff9a9fa8);status.setGravity(Gravity.CENTER);status.setPadding(0,0,0,0);status.setSingleLine();content.addView(status,new LinearLayout.LayoutParams(dp(240),dp(24)));
  TextView hint=new TextView(this);hint.setText("返回取消 · 首页选择应用");hint.setTextSize(10);hint.setTextColor(0xff8c919b);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hintLayout=new LinearLayout.LayoutParams(dp(280),dp(20));hintLayout.topMargin=dp(16);content.addView(hint,hintLayout);
  actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);actions.setVisibility(android.view.View.GONE);content.addView(actions,new LinearLayout.LayoutParams(-1,-2));
  retry=quietButton("重试");retry.setVisibility(android.view.View.GONE);retry.setOnClickListener(v->{int next=EntryObserver.renewRequest(token);if(next<0){cancelAndFinish();return;}token=next;retry.setVisibility(android.view.View.GONE);actions.setVisibility(android.view.View.GONE);backdrop.setBusy(true);gate.start();});actions.addView(retry,actionLayout());
  Button cancel=quietButton("取消");cancel.setOnClickListener(v->cancelAndFinish());actions.addView(cancel,actionLayout());setContentView(root);
  gate=new NetworkGate(this,new NetworkGate.Listener(){
   public void status(String value){status.setText(value.contains("等待")?"连接准备中":value.contains("验证")?"正在确认连接":"连接准备中");}
   public void startingProxy(){proxyGrace=SystemClock.elapsedRealtime()+4000;}
   public void failed(String value){backdrop.setBusy(false);actions.setVisibility(android.view.View.VISIBLE);status.setText(value);retry.setVisibility(android.view.View.VISIBLE);retry.requestFocus();}
   public void ready(){launchNetflix();}
  });handler.postDelayed(()->{if(!isFinishing()&&EntryObserver.requestValid(token))gate.start();},150);
 }
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private LinearLayout.LayoutParams actionLayout(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(112),dp(40));p.leftMargin=dp(6);p.rightMargin=dp(6);return p;}
 private Button quietButton(String label){
  Button button=new Button(this);button.setText(label);button.setTextSize(12);button.setAllCaps(false);button.setTextColor(0xffeceef1);button.setMinHeight(0);button.setMinimumHeight(0);button.setPadding(dp(12),0,dp(12),0);button.setElevation(0);button.setStateListAnimator(null);button.setDefaultFocusHighlightEnabled(false);
  android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
  states.addState(new int[]{android.R.attr.state_focused},buttonSurface(true));states.addState(new int[]{android.R.attr.state_pressed},buttonSurface(true));states.addState(new int[]{},buttonSurface(false));button.setBackground(states);return button;
 }
 private android.graphics.drawable.Drawable buttonSurface(boolean focused){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(focused?0xff303339:0x00111214);d.setCornerRadius(dp(10));return d;}
 private void launchNetflix(){
  if(!resumed||!hasWindowFocus()||!EntryObserver.requestValid(token)||!getPackageName().equals(EntryObserver.activePackage())||!Bridge.googleDisabled(this)){cancelAndFinish();return;}
  Intent target=getPackageManager().getLeanbackLaunchIntentForPackage(Bridge.NETFLIX);if(target==null)target=getPackageManager().getLaunchIntentForPackage(Bridge.NETFLIX);
  if(target==null){backdrop.setBusy(false);actions.setVisibility(android.view.View.VISIBLE);status.setText("未找到 Netflix 电视版");return;}
  launching=true;EntryObserver.finishRequest(token);
  try{startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));android.util.Log.i("StreamGuardObserver","netflix launched after network preflight");finish();}catch(RuntimeException e){launching=false;backdrop.setBusy(false);actions.setVisibility(android.view.View.VISIBLE);status.setText("暂时无法打开 Netflix，请返回重试");}
 }
 private void cancelAndFinish(){if(gate!=null)gate.cancel();EntryObserver.finishRequest(token);finish();}
 @Override protected void onResume(){super.onResume();resumed=true;}
 @Override protected void onPause(){super.onPause();resumed=false;if(launching||isFinishing())return;handler.postDelayed(checkDeparture,250);}
 private final Runnable checkDeparture=new Runnable(){public void run(){
  if(launching||isFinishing())return;
  String active=EntryObserver.activePackage();
  if(resumed&&hasWindowFocus()&&getPackageName().equals(active))return;
  if(SystemClock.elapsedRealtime()<proxyGrace&&Bridge.CLASH.equals(active)){handler.postDelayed(this,250);return;}
  cancelAndFinish();
 }};
 @Override protected void onStop(){super.onStop();if(!launching&&!isFinishing()&&SystemClock.elapsedRealtime()>=proxyGrace)cancelAndFinish();}
 @Override public void onBackPressed(){cancelAndFinish();}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(gate!=null)gate.close();if(!launching)EntryObserver.finishRequest(token);super.onDestroy();}
}
