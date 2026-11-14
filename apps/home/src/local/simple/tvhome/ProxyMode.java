package local.simple.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * How the launcher reaches the network.
 *
 * AUTO starts FlClash and waits for its VPN, matching the historical behaviour.
 * DIRECT leaves FlClash alone and relies on an upstream proxy (a router, for
 * example) that already intercepts the TV's traffic.
 * OFF never starts or waits for a proxy at all.
 *
 * The legacy MANUAL_PROXY_OFF flag is honoured so an existing installation that
 * already carries the flag keeps behaving as DIRECT.
 */
final class ProxyMode {
    static final String KEY = "proxy_mode";
    static final String AUTO = "auto";
    static final String DIRECT = "direct";
    static final String OFF = "off";
    static final String[] MODES = {AUTO, DIRECT, OFF};

    private ProxyMode() { }

    static String mode(Context context) {
        SharedPreferences prefs = LauncherSupport.preferences(context);
        return resolve(prefs.getString(KEY, null), prefs.getBoolean(LauncherSupport.MANUAL_PROXY_OFF, false));
    }

    /** Pure parsing so the mode contract stays verifiable without Android. */
    static String resolve(String stored, boolean legacyOptOut) {
        if (stored == null) {
            // Pre-v0.2.3 installs used a single flag. A flag that is present
            // already means the owner asked for a proxy-free connection.
            return legacyOptOut ? DIRECT : AUTO;
        }
        for (String candidate : MODES) if (candidate.equals(stored)) return candidate;
        return AUTO;
    }

    static void set(Context context, String mode) {
        for (String candidate : MODES) if (candidate.equals(mode)) {
            LauncherSupport.preferences(context).edit().putString(KEY, candidate)
                    .putBoolean(LauncherSupport.MANUAL_PROXY_OFF, DIRECT.equals(candidate)).apply();
            return;
        }
    }

    /** True only when the launcher itself may pull FlClash up. */
    static boolean startsFlClash(Context context) {
        return AUTO.equals(mode(context));
    }

    /** True when reaching the target requires a local VPN tunnel. */
    static boolean requiresVpn(Context context) {
        return AUTO.equals(mode(context));
    }

    static String label(String mode) {
        if (DIRECT.equals(mode)) return "直连";
        if (OFF.equals(mode)) return "关闭";
        return "自动";
    }

    static String next(String mode) {
        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(mode)) return MODES[(i + 1) % MODES.length];
        return AUTO;
    }

    /**
     * The status strip names its line only while a local proxy is in charge.
     * A direct connection is the expected case and stays unlabelled.
     */
    static String statusLabel(boolean direct, String node, String delay) {
        if (direct) return "";
        return "Clash · " + node + delay;
    }
}
