package local.simple.streamguardobserver;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.*;

/** Launcher intent observation and exact Netflix compatibility-dialog handling. No account text is retained. */
public final class EntryObserver extends AccessibilityService {
 private static final String TAG="StreamGuardObserver";
 private static EntryObserver current;
 private final Handler handler=new Handler();
 private final DialogPolicy dialogPolicy=new DialogPolicy();
 private final Set<Integer> knownWindows=new LinkedHashSet<>();
 private final Map<String,Long> eventLoggedAt=new HashMap<>();
 private boolean baselineReady,dialogQueued,pending,guardShown;
 private int generation,requestBlockedWindow=-1;
 private String baselinePackage="";
 private String requestOverlay="",requestHome="";
 private long requestCreated,requestEventStarted,connectedAt,lastVideoStateEvent;
 private boolean bootPending;private int bootCount;
 private long bootStableSince;private int bootStableWindow=-1;private android.net.Network bootStableNetwork;
 static int renewRequest(int token){if(current==null||!current.pending||current.generation!=token||!current.getPackageName().equals(current.rootPackage()))return -1;current.requestCreated=SystemClock.elapsedRealtime();current.requestEventStarted=SystemClock.uptimeMillis();return ++current.generation;}
 static boolean requestValid(int token){return current!=null&&current.pending&&current.generation==token&&SystemClock.elapsedRealtime()-current.requestCreated<125000;}
 static void finishRequest(int token){if(current!=null&&current.generation==token){current.pending=false;current.guardShown=false;current.generation++;}}
 static String activePackage(){return current==null?"":current.rootPackage();}
 private String rootPackage(){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return "";try{return root.getPackageName()==null?"":root.getPackageName().toString();}finally{root.recycle();}}
 private static String label(CharSequence value){
  if(value==null)return null;String text=value.toString().trim();
  if("Netflix".equals(text))return "netflix";
  if("YouTube".equals(text)||"TizenTube".equals(text))return "youtube";
  if(DialogPolicy.BLOCKED.equals(text))return "netflix_blocked";
  return null;
 }
 private static void collect(AccessibilityNodeInfo node,Set<String> labels,int depth,int[] remaining){
  if(node==null||depth>4||remaining[0]--<=0||!Bridge.launcher(node.getPackageName())||!node.isVisibleToUser())return;
  String text=label(node.getText()),description=label(node.getContentDescription());
  if(text!=null)labels.add(text);if(description!=null)labels.add(description);
  for(int n=0;n<node.getChildCount()&&remaining[0]>0;n++){AccessibilityNodeInfo child=node.getChild(n);if(child!=null){try{collect(child,labels,depth+1,remaining);}finally{child.recycle();}}}
 }
 private void remember(int id){knownWindows.add(id);if(knownWindows.size()>256)knownWindows.remove(knownWindows.iterator().next());}
 private void refreshServiceConfiguration(){
  // Some Android TV builds retain the enabled service's old dynamic XML fields
  // across package replacement. Reapply exactly the fields in res/xml/observer.xml.
  // Retain the framework-owned descriptor and manifest-granted capabilities.
  AccessibilityServiceInfo info=getServiceInfo();
  if(info==null){Log.w(TAG,"service_configuration unavailable");return;}
  info.eventTypes=AccessibilityEvent.TYPE_VIEW_CLICKED|AccessibilityEvent.TYPE_VIEW_FOCUSED
   |AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
   |AccessibilityEvent.TYPE_WINDOWS_CHANGED;
  info.packageNames=new String[]{LauncherIdentity.OVERLAY,LauncherIdentity.HOME,LauncherIdentity.DOCK_OVERLAY,LauncherIdentity.DOCK_HOME,Bridge.NETFLIX,Bridge.YOUTUBE,"com.xiaodianshi.tv.yst","top.yogiczy.mytv.tv"};
  info.feedbackType=AccessibilityServiceInfo.FEEDBACK_GENERIC;
  info.notificationTimeout=100;
  info.flags=AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
   |AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
  setServiceInfo(info);
  Log.i(TAG,"service_configuration applied eventTypes="+info.eventTypes+" flags="+info.flags+" packages="+info.packageNames.length);
 }
 @Override protected void onServiceConnected(){
  refreshServiceConfiguration();
  handler.removeCallbacksAndMessages(null);dialogQueued=false;knownWindows.clear();
  current=this;connectedAt=SystemClock.elapsedRealtime();baselineReady=false;pending=false;generation++;
  bootCount=android.provider.Settings.Global.getInt(getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,-1);
  android.content.SharedPreferences state=getSharedPreferences("guardian",MODE_PRIVATE);
  int previous=state.getInt("last_connected_boot",-1);
  bootPending=previous>=0&&bootCount>=0&&previous!=bootCount&&connectedAt<180000&&state.getInt("bootstrap_consumed_boot",-1)!=bootCount;
  resetBootStability();
  state.edit().putInt("last_connected_boot",bootCount).commit();
  handler.postDelayed(()->{for(AccessibilityWindowInfo window:getWindows())remember(window.getId());baselineReady=true;Log.i(TAG,"guardian connected; existing-window baseline recorded");},1000);
  handler.postDelayed(monitor,5000);
 }
 @Override public void onAccessibilityEvent(AccessibilityEvent event){
  if(event==null)return;String pkg=event.getPackageName()==null?"":event.getPackageName().toString();int type=event.getEventType();
  boolean wasBootPending=bootPending;
  if(bootPending&&(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOWS_CHANGED))bootCompensation(rootPackage());
  if(wasBootPending&&!bootPending)return;
  if(Bridge.NETFLIX.equals(pkg)||Bridge.YOUTUBE.equals(pkg)||"com.xiaodianshi.tv.yst".equals(pkg)||"top.yogiczy.mytv.tv".equals(pkg)){
   if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){lastVideoStateEvent=Math.max(lastVideoStateEvent,event.getEventTime());if(pending&&!guardShown&&!pkg.equals(baselinePackage)&&event.getEventTime()>requestEventStarted)cancelRequest("different_video_opened");}
   if(Bridge.NETFLIX.equals(pkg))queueDialogCheck();return;
  }
  if(!Bridge.launcher(pkg))return;
  // A HOME switch to the other installed launcher revokes this request even when
  // the system launches it without a VIEW_CLICKED accessibility event.
  if(pending&&event.getEventTime()>=requestEventStarted&&!LauncherIdentity.samePair(requestOverlay,pkg)&&pkg.equals(rootPackage())){cancelRequest("different_launcher_opened");return;}
  Set<String> labels=new LinkedHashSet<>();for(CharSequence text:event.getText()){String allowed=label(text);if(allowed!=null)labels.add(allowed);}
  String description=label(event.getContentDescription());if(description!=null)labels.add(description);
  AccessibilityNodeInfo source=event.getSource();boolean sourceNull=source==null;
  if(source!=null){try{collect(source,labels,0,new int[]{48});}finally{source.recycle();}}
  long now=SystemClock.elapsedRealtime();String key=pkg+":"+type;Long last=eventLoggedAt.get(key);
  if(type==AccessibilityEvent.TYPE_VIEW_CLICKED||type==AccessibilityEvent.TYPE_VIEW_FOCUSED||last==null||now-last>=750){
   Log.i(TAG,"event type="+type+" package="+pkg+" sourceNull="+sourceNull+" window="+event.getWindowId()+" whitelist="+labels);eventLoggedAt.put(key,now);
  }
  if(type==AccessibilityEvent.TYPE_VIEW_CLICKED&&bootPending){cancelBoot("launcher click");return;}
  if(type==AccessibilityEvent.TYPE_VIEW_CLICKED&&pending&&event.getEventTime()>requestEventStarted)cancelRequest("new_launcher_click");
  // During a real cold boot the old launcher can fail before the VPN is ready.
  // Leave that card intact; the stable-foreground bootstrap path owns it.
  if(bootPending&&labels.contains("netflix_blocked"))return;
  if(labels.contains("netflix_blocked")&&baselineReady&&!knownWindows.contains(event.getWindowId())){
   remember(event.getWindowId());
   if(!pending&&interactive()&&Bridge.googleDisabled(this)&&SystemClock.uptimeMillis()-event.getEventTime()>=0&&SystemClock.uptimeMillis()-event.getEventTime()<2000&&event.getEventTime()>=lastVideoStateEvent&&visibleBlockedWindow(event.getWindowId()))beginNetflixRequest(event.getWindowId());
  }
 }
 private boolean visibleBlockedWindow(int id){
  for(AccessibilityWindowInfo window:getWindows())if(window.getId()==id){AccessibilityNodeInfo root=window.getRoot();if(root==null)return false;try{return Bridge.launcher(root.getPackageName())&&exactText(root,DialogPolicy.BLOCKED,false);}finally{root.recycle();}}
  return false;
 }
 private String overlayPackageForWindow(int id){
  for(AccessibilityWindowInfo window:getWindows())if(window.getId()==id){AccessibilityNodeInfo root=window.getRoot();if(root==null)return null;try{return Bridge.overlay(root.getPackageName())?root.getPackageName().toString():null;}finally{root.recycle();}}return null;
 }
 private boolean allowedBaseline(String pkg){return LauncherIdentity.allowedBaseline(pkg);}
 private boolean interactive(){android.os.PowerManager power=(android.os.PowerManager)getSystemService(POWER_SERVICE);return power!=null&&power.isInteractive();}
 private boolean expectedForeground(){if(!interactive())return false;String p=rootPackage();return p.equals(requestOverlay)||p.equals(requestHome)||(!Bridge.launcher(p)&&p.equals(baselinePackage));}
 private void beginNetflixRequest(int blockedId){
  String overlay=overlayPackageForWindow(blockedId),home=LauncherIdentity.homeForOverlay(overlay);
  if(home==null)return;
  requestBlockedWindow=blockedId;
  baselinePackage=rootPackage();if(!allowedBaseline(baselinePackage))return;
  if(Bridge.launcher(baselinePackage)&&!LauncherIdentity.samePair(overlay,baselinePackage))return;
  requestOverlay=overlay;requestHome=home;
  pending=true;guardShown=false;requestCreated=SystemClock.elapsedRealtime();requestEventStarted=SystemClock.uptimeMillis();final int token=++generation;
  Log.i(TAG,"intent blocked_card target=netflix; starting bounded preflight");
  if(!expectedForeground()||!Bridge.originalHome(this,requestOverlay)){cancelRequest("home_clear_unavailable");return;}
  handler.postDelayed(()->waitLauncherCleanup(token,SystemClock.elapsedRealtime()+8000,false),300);
 }
 private boolean blockedCardPresent(){
  java.util.List<AccessibilityWindowInfo> windows=getWindows();if(windows.isEmpty())return true;
  for(AccessibilityWindowInfo window:windows){if(window.getId()==requestBlockedWindow)return true;AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;try{if(Bridge.launcher(root.getPackageName())&&exactText(root,DialogPolicy.BLOCKED,false))return true;}finally{root.recycle();}}
  return false;
 }
 private void waitLauncherCleanup(int token,long deadline,boolean menuClosed){
  if(!requestValid(token))return;
  if(!expectedForeground()){cancelRequest("foreground_changed_during_clear");return;}
  if(SystemClock.elapsedRealtime()>deadline){cancelRequest("launcher_cleanup_timeout");return;}
  String active=rootPackage();
  if(blockedCardPresent()){handler.postDelayed(()->waitLauncherCleanup(token,deadline,menuClosed),250);return;}
  if(Bridge.overlay(active)){
   if(!menuClosed){boolean sent=dismissOriginalMenuOnce();handler.postDelayed(()->waitLauncherCleanup(token,deadline,sent),sent?900:250);}
   else handler.postDelayed(()->waitLauncherCleanup(token,deadline,true),250);
   return;
  }
  // A real menu-close acknowledgement prevents the old HOME callback from stealing the wait page.
  if(!menuClosed){handler.postDelayed(()->waitLauncherCleanup(token,deadline,false),250);return;}
  guardShown=true;Log.i(TAG,"launcher blocked card and menu cleared");
  try{startActivity(new Intent(this,GuardActivity.class).putExtra("request",token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
  catch(RuntimeException e){cancelRequest("guard_activity_unavailable");}
 }

 private boolean menuMatches(AccessibilityNodeInfo root){
  if(root==null||!requestOverlay.contentEquals(root.getPackageName()==null?"":root.getPackageName())||!Bridge.overlay(root.getPackageName())||!focusedWindow(root.getWindowId()))return false;
  // Original releases expose both labels. The dock keeps settings controls collapsed,
  // so identify its own visible container as well as its two persistent category labels.
  if(exactText(root,"Netflix",false)&&exactText(root,"系统设置",false))return true;
  if(!exactDescription(root,"设置")||!exactDescription(root,"智能家居"))return false;
  List<AccessibilityNodeInfo> docks=root.findAccessibilityNodeInfosByViewId(root.getPackageName()+":id/lightframe_dock");
  boolean visible=false;for(AccessibilityNodeInfo node:docks){try{if(node.isVisibleToUser())visible=true;}finally{node.recycle();}}
  return visible;
 }
 private boolean exactDescription(AccessibilityNodeInfo root,String value){
  List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(value);boolean found=false;
  for(AccessibilityNodeInfo node:nodes){try{if(node.isVisibleToUser()&&node.isClickable()&&node.isFocusable()&&value.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))found=true;}finally{node.recycle();}}return found;
 }
 private boolean dismissOriginalMenuOnce(){
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;int id=root.getWindowId();boolean matches;
  try{matches=menuMatches(root);}finally{root.recycle();}if(!matches)return false;
  root=getRootInActiveWindow();if(root==null)return false;
  try{matches=root.getWindowId()==id&&menuMatches(root);}finally{root.recycle();}
  if(!matches)return false;
  boolean accepted=performGlobalAction(GLOBAL_ACTION_BACK);Log.i(TAG,"launcher menu back attempted accepted="+accepted);return true;
 }
 private void cancelRequest(String reason){pending=false;guardShown=false;generation++;Log.i(TAG,"request cancelled reason="+reason);}
 private static boolean exactText(AccessibilityNodeInfo root,String text,boolean button){return exactNode(root,text,button,null);}
 private static boolean exactNode(AccessibilityNodeInfo root,String text,boolean button,String viewId){
  List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text);boolean found=false;
  for(AccessibilityNodeInfo n:nodes){try{if(n.isVisibleToUser()&&(viewId==null||viewId.equals(n.getViewIdResourceName()))&&text.contentEquals(n.getText()==null?"":n.getText())&&(!button||n.isClickable()&&n.isEnabled()))found=true;}finally{n.recycle();}}
  return found;
 }
 private boolean exactDialog(AccessibilityNodeInfo root){return root!=null&&Bridge.NETFLIX.contentEquals(root.getPackageName()==null?"":root.getPackageName())&&exactNode(root,DialogPolicy.TITLE,false,"android:id/alertTitle")&&exactNode(root,DialogPolicy.BODY,false,"android:id/message")&&exactNode(root,DialogPolicy.BUTTON,true,"android:id/button1");}
 private boolean focusedWindow(int id){for(AccessibilityWindowInfo w:getWindows())if(w.getId()==id)return w.isActive()&&w.isFocused();return false;}
 private void queueDialogCheck(){if(dialogQueued)return;dialogQueued=true;handler.postDelayed(()->{dialogQueued=false;checkDialog();},650);}
 private void checkDialog(){
  if(!interactive()||!Bridge.googleDisabled(this))return;
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
  int id=root.getWindowId();boolean exact=false;
  try{if(!Bridge.NETFLIX.contentEquals(root.getPackageName()==null?"":root.getPackageName()))return;exact=exactDialog(root);}finally{root.recycle();}
  long now=SystemClock.elapsedRealtime();if(dialogPolicy.observe(exact,now))Log.i(TAG,"exact_google_dialog absence_confirmed");if(!exact||!dialogPolicy.allow(id,now)||!focusedWindow(id))return;
  // Re-read immediately before global BACK. Never act on a cached background window.
  root=getRootInActiveWindow();if(root==null)return;boolean stillExact;
  try{stillExact=root.getWindowId()==id&&exactDialog(root)&&focusedWindow(id);}finally{root.recycle();}
  if(!stillExact||!Bridge.googleDisabled(this))return;
  dialogPolicy.consume(id,now);boolean sent=performGlobalAction(GLOBAL_ACTION_BACK);
  Log.i(TAG,"exact_google_dialog back_requested="+sent+" window="+id);
  handler.postDelayed(()->checkSameDialogWindow(id),8200);
 }
 private void checkSameDialogWindow(int id){
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;boolean same;
  try{same=root.getWindowId()==id;}finally{root.recycle();}
  if(same)queueDialogCheck();
 }
 private Boolean lastVpn;
 private long outageSince,lastRecovery,restoredSince;private int recoveryAttempts;
 private void cancelBoot(String reason){
  if(bootStableWindow>=0)remember(bootStableWindow);
  for(AccessibilityWindowInfo window:getWindows()){AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;try{if(Bridge.launcher(root.getPackageName())&&exactText(root,DialogPolicy.BLOCKED,false))remember(window.getId());}finally{root.recycle();}}
  bootPending=false;resetBootStability();Log.i(TAG,"boot preflight cancelled: "+reason);
 }
 private void resetBootStability(){bootStableSince=0;bootStableWindow=-1;bootStableNetwork=null;}
 private void bootCompensation(String pkg){
  if(!bootPending)return;
  long now=SystemClock.elapsedRealtime();
  if(now>180000){cancelBoot("timeout");return;}
  if(!pkg.isEmpty()&&!Bridge.launcher(pkg)&&!"com.android.systemui".equals(pkg)&&!Bridge.CLASH.equals(pkg)){
   cancelBoot("foreground change");return;
  }
  android.net.Network vpn=Bridge.vpn(this);
  if(!baselineReady||pending||!interactive()||!Bridge.launcher(pkg)||!Bridge.googleDisabled(this)||!Bridge.correctVpnPolicy(this)||vpn==null){resetBootStability();return;}
  int blockedWindow=-1;
  for(AccessibilityWindowInfo window:getWindows()){
   AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
   boolean blocked;try{blocked=Bridge.overlay(root.getPackageName())&&LauncherIdentity.samePair(root.getPackageName().toString(),pkg)&&exactText(root,DialogPolicy.BLOCKED,false);}finally{root.recycle();}
   if(blocked){blockedWindow=window.getId();break;}
  }
  if(blockedWindow<0){resetBootStability();return;}
  if(bootStableSince==0||blockedWindow!=bootStableWindow||!vpn.equals(bootStableNetwork)){
   bootStableSince=now;bootStableWindow=blockedWindow;bootStableNetwork=vpn;
   Log.i(TAG,"boot preflight waiting for stable launcher and VPN");return;
  }
  if(now-bootStableSince<4000)return;
  // Recheck immediately before consuming the one-shot token and clearing UI.
  if(!interactive()||!LauncherIdentity.samePair(overlayPackageForWindow(blockedWindow),rootPackage())||!vpn.equals(Bridge.vpn(this))||!Bridge.correctVpnPolicy(this)||!Bridge.googleDisabled(this)||!visibleBlockedWindow(blockedWindow)){
   resetBootStability();return;
  }
  bootPending=false;resetBootStability();
  if(!getSharedPreferences("guardian",MODE_PRIVATE).edit().putInt("bootstrap_consumed_boot",bootCount).commit()){
   Log.w(TAG,"boot preflight token persistence failed");return;
  }
  remember(blockedWindow);Log.i(TAG,"boot preflight stable; consuming bootstrap token");beginNetflixRequest(blockedWindow);
 }
 private final Runnable monitor=new Runnable(){public void run(){
  if(current!=EntryObserver.this)return;
  String pkg=rootPackage();bootCompensation(pkg);
  if(interactive()&&Bridge.NETFLIX.equals(pkg))queueDialogCheck();
  if(interactive()&&(Bridge.NETFLIX.equals(pkg)||Bridge.YOUTUBE.equals(pkg))){
   boolean vpn=Bridge.vpn(EntryObserver.this)!=null;long now=SystemClock.elapsedRealtime();
   if(lastVpn==null||lastVpn!=vpn){lastVpn=vpn;Log.i(TAG,"video_network vpn="+vpn);}
   if(vpn){outageSince=0;if(restoredSince==0)restoredSince=now;if(now-restoredSince>=8000)recoveryAttempts=0;}
   else{restoredSince=0;if(outageSince==0)outageSince=now;
    if(now-outageSince>=8000&&recoveryAttempts<3&&(lastRecovery==0||now-lastRecovery>=30000)){
     String active=rootPackage();
     if(interactive()&&(Bridge.NETFLIX.equals(active)||Bridge.YOUTUBE.equals(active))&&Bridge.vpn(EntryObserver.this)==null&&Bridge.correctVpnPolicy(EntryObserver.this)){
      lastRecovery=now;recoveryAttempts++;boolean sent=Bridge.startProxy(EntryObserver.this);Log.i(TAG,"vpn_recovery requested="+sent+" attempt="+recoveryAttempts);
     }
    }
   }
  }else{outageSince=0;restoredSince=0;}
  handler.postDelayed(this,2000);
 }};
 @Override public void onInterrupt(){cancelRequest("service_interrupted");}
 @Override public void onDestroy(){if(current==this)current=null;pending=false;generation++;handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
