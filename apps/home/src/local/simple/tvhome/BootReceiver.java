package local.simple.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** The normal boot starts a fresh automatic-connection session. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        try {
            LauncherSupport.preferences(context).edit()
                    .remove(LauncherSupport.MANUAL_PROXY_OFF)
                    .remove(LauncherSupport.ALLOW_STOCK_UNTIL_MS).apply();
            LauncherSupport.ensureProxyStarted(context);
            context.startForegroundService(new Intent("BOOT")
                .setComponent(new android.content.ComponentName("local.simple.tvoverlay","local.simple.tvoverlay.OverlayRuntimeService"))
                .putExtra("core_secret",LauncherSupport.preferences(context).getString("core_secret","")));
        } catch (RuntimeException ignored) { }
    }
}
