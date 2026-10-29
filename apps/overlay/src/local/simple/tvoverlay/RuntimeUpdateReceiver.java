package local.simple.tvoverlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restore the status overlay after an in-place update without opening a video app or menu. */
public final class RuntimeUpdateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        if (!LauncherSupport.isDefaultHome(context)) return;
        try {
            context.startForegroundService(new Intent(context, OverlayRuntimeService.class)
                    .setAction("RESTORE"));
        } catch (RuntimeException error) {
            android.util.Log.w("HomeOverlay", "runtime restore after update failed", error);
        }
    }
}
