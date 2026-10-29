package local.simple.streamguardobserver;
import java.util.ArrayDeque;

/** Does not interact with Android. At most two attempts per visible dialog, with a rolling action budget. */
final class DialogPolicy {
 static final String TITLE="启用 Google Play 服务";
 static final String BODY="您必须先启用 Google Play 服务，然后才能运行Netflix。";
 static final String BUTTON="启用";
 static final String BLOCKED="Google Play 服务已停用，暂未打开 Netflix";
 private final ArrayDeque<Long> actions=new ArrayDeque<>();
 private int consumedWindow=-1,windowAttempts;
 private long lastAction=-100000,absentSince=-1;
 boolean allow(int window,long now){
  while(!actions.isEmpty()&&now-actions.peekFirst()>=120000)actions.removeFirst();
  return window>=0&&(window!=consumedWindow||windowAttempts<2)&&now-lastAction>=8000&&actions.size()<3;
 }
 void consume(int window,long now){windowAttempts=window==consumedWindow?windowAttempts+1:1;consumedWindow=window;lastAction=now;absentSince=-1;actions.addLast(now);}
 boolean observe(boolean exactDialogPresent,long now){
  if(exactDialogPresent){absentSince=-1;return false;}
  if(absentSince<0)absentSince=now;
  // Only a confirmed absence from Netflix itself resets a reused window ID.
  if(now-absentSince>=1500&&consumedWindow>=0){consumedWindow=-1;windowAttempts=0;return true;}
  return false;
 }
}
