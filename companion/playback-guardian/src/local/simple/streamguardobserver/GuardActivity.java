package local.simple.streamguardobserver;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.view.Gravity;
import android.widget.*;

/** Private transient wait page. It is never a HOME activity and never launches after losing the user's focus. */
public final class GuardActivity extends Activity {
 private final Handler handler=new Handler();private NetworkGate gate;private TextView status;private Button retry;private int token;private boolean resumed,launching;private long proxyGrace;
 @Override public void onCreate(Bundle state){super.onCreate(state);token=getIntent().getIntExtra("request",-1);if(!EntryObserver.requestValid(token)){finish();return;}
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setPadding(48,48,48,48);root.setBackgroundColor(0xff111318);
  TextView title=new TextView(this);title.setText("正在准备 Netflix");title.setTextSize(28);title.setTextColor(Color.WHITE);root.addView(title);
  status=new TextView(this);status.setTextSize(20);status.setTextColor(0xffdddddd);status.setPadding(0,24,0,24);root.addView(status);
  retry=new Button(this);retry.setText("重试");retry.setVisibility(android.view.View.GONE);retry.setOnClickListener(v->{int next=EntryObserver.renewRequest(token);if(next<0){cancelAndFinish();return;}token=next;retry.setVisibility(android.view.View.GONE);gate.start();});root.addView(retry);
  Button cancel=new Button(this);cancel.setText("取消");cancel.setOnClickListener(v->cancelAndFinish());root.addView(cancel);setContentView(root);cancel.requestFocus();
  gate=new NetworkGate(this,new NetworkGate.Listener(){
   public void status(String value){status.setText(value);}
   public void startingProxy(){proxyGrace=SystemClock.elapsedRealtime()+4000;}
   public void failed(String value){status.setText(value);retry.setVisibility(android.view.View.VISIBLE);retry.requestFocus();}
   public void ready(){launchNetflix();}
  });handler.postDelayed(()->{if(!isFinishing()&&EntryObserver.requestValid(token))gate.start();},150);
 }
 private void launchNetflix(){
  if(!resumed||!hasWindowFocus()||!EntryObserver.requestValid(token)||!getPackageName().equals(EntryObserver.activePackage())||!Bridge.googleDisabled(this)){cancelAndFinish();return;}
  Intent target=getPackageManager().getLeanbackLaunchIntentForPackage(Bridge.NETFLIX);if(target==null)target=getPackageManager().getLaunchIntentForPackage(Bridge.NETFLIX);
  if(target==null){status.setText("未找到 Netflix 电视版");return;}
  launching=true;EntryObserver.finishRequest(token);
  try{startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));android.util.Log.i("StreamGuardObserver","netflix launched after network preflight");finish();}catch(RuntimeException e){launching=false;status.setText("暂时无法打开 Netflix，请返回重试");}
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
