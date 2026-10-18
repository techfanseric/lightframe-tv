package local.simple.tvoverlay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Small, exception-contained bridges to already observed device interfaces. */
public final class LauncherSupport {
    public static final String PREFS_NAME = "tvhome";
    public static final String MANUAL_PROXY_OFF = "manual_proxy_off";
    public static final String ALLOW_STOCK_UNTIL_MS = "allow_stock_until_ms";
    private LauncherSupport() { }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void allowStockTemporarily(Context context) {
        preferences(context).edit().putLong(ALLOW_STOCK_UNTIL_MS,
                System.currentTimeMillis() + 30000L).apply();
    }

    public static boolean isStockTemporarilyAllowed(Context context) {
        return preferences(context).getLong(ALLOW_STOCK_UNTIL_MS, 0L)
                > System.currentTimeMillis();
    }

    public static boolean openHome(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).putExtra("return_home", true)
                    .setComponent(new ComponentName("local.simple.tvhome", "local.simple.tvhome.MainActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            android.app.PendingIntent.getActivities(context, 41,
                    new Intent[] { new Intent(intent), new Intent(intent) },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT).send();
            return true;
        } catch (Exception e) { android.util.Log.w("SimpleHomeBridge", "redirect-error=" + e.getClass().getSimpleName()); return false; }
    }

    public static boolean startFlClashQuickAction(Context context, String action) {
        if (!"START".equals(action) && !"STOP".equals(action)) return false;
        try {
            Intent intent = new Intent("com.follow.clash.action." + action)
                    .setComponent(new ComponentName("com.follow.clash",
                            "com.follow.clash.QuickActionActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (RuntimeException e) { return false; }
    }

    public static void ensureProxyStarted(Context context) {
        try {
            if (preferences(context).getBoolean(MANUAL_PROXY_OFF, false)) return;
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            for (Network network : cm.getAllNetworks()) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return;
            }
            startFlClashQuickAction(context, "START");
        } catch (RuntimeException ignored) { }
    }

    /** REA-verified firmware mapping: HDMI 1/2/3 use input 23/24/25. */
    public static boolean switchHdmi(Context context, int port) {
        if (port < 1 || port > 3) return openSourcePopup(context);
        try {
            Intent intent = new Intent("com.xiaomi.mitv.tvplayer.PLAY")
                    .setComponent(new ComponentName("com.xiaomi.mitv.tvplayer",
                            "com.xiaomi.mitv.tvplayer.ExternalSourceActivity"))
                    .putExtra("input", 22 + port)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (RuntimeException e) { return openSourcePopup(context); }
    }

    public static boolean openSourcePopup(Context context) {
        try {
            Intent intent = new Intent("com.xiaomi.mitv.settings.INPUTSOURCE_POPUP")
                    .setComponent(new ComponentName("com.xiaomi.mitv.settings",
                            "com.xiaomi.mitv.settings.SettingsBroadcastReceiver"));
            context.sendBroadcast(intent);
            return true;
        } catch (RuntimeException e) { return false; }
    }
}
