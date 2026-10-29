package local.simple.streamguardobserver;
import android.content.*;
import android.os.SystemClock;
/** Only the real boot broadcast wakes the configured VPN. Never starts a video app. */
public final class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(i==null||!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||SystemClock.elapsedRealtime()>180000)return;if(Bridge.correctVpnPolicy(c)&&Bridge.vpn(c)==null){Bridge.startProxy(c);android.util.Log.i("StreamGuardObserver","boot requested configured VPN");}}
}
