package local.simple.tvoverlay;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;
import java.lang.reflect.Method;

/**
 * Reads what is on screen right now.
 *
 * Usage events arrive late and can describe a screen that has already been
 * left, so the running processes are asked instead. The process with the
 * lowest importance among the known candidates is the one the user sees.
 */
final class CurrentApp {
    /** Anything at or below this importance is on screen. */
    private static final int VISIBLE = 200;
    /** The television's own source player runs as a system application. */
    private static final String[] SOURCE_PACKAGES = {
        "com.xiaomi.mitv.tvplayer",
        "com.mediatek.tvinput"
    };

    private CurrentApp() { }

    /**
     * The package the user is looking at, or an empty string when it cannot
     * be established. The launcher's own windows and the system interface are
     * never reported, because they sit on top of the picture.
     */
    static String foreground(Context context) {
        String best = "";
        int bestImportance = Integer.MAX_VALUE;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return best;
        Method uidImportance = uidImportance();
        if (uidImportance != null) {
            for (String pkg : candidates(context)) {
                int importance = importance(context, manager, uidImportance, pkg);
                if (importance < 0) continue;
                if (importance < bestImportance) {
                    bestImportance = importance;
                    best = pkg;
                }
            }
            if (!best.isEmpty()) {
                android.util.Log.i("HomeOverlay", "current app=" + best + " importance=" + bestImportance);
                return bestImportance <= VISIBLE ? best : "";
            }
        }
        List<ActivityManager.RunningAppProcessInfo> processes;
        try {
            processes = manager.getRunningAppProcesses();
        } catch (RuntimeException unavailable) {
            return best;
        }
        if (processes == null) return best;
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process == null || process.pkgList == null) continue;
            for (String pkg : process.pkgList) {
                if (!candidate(pkg)) continue;
                int importance = process.importance;
                if (importance < bestImportance) {
                    bestImportance = importance;
                    best = pkg;
                }
            }
        }
        android.util.Log.i("HomeOverlay", "current app=" + best + " importance=" + bestImportance);
        return bestImportance <= VISIBLE ? best : "";
    }

    /** The system API that this firmware exposes to usage-access holders. */
    private static Method uidImportance() {
        try {
            return ActivityManager.class.getMethod("getUidImportance", int.class);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return null;
        }
    }

    private static int importance(Context context, ActivityManager manager, Method uidImportance, String pkg) {
        try {
            int uid = context.getPackageManager().getApplicationInfo(pkg, 0).uid;
            return ((Integer) uidImportance.invoke(manager, uid)).intValue();
        } catch (Exception | LinkageError unavailable) {
            return -1;
        }
    }

    /** Every package that could be the picture, including the source player. */
    private static String[] candidates(Context context) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int app = 0; app < AppCatalog.IDS.length; app++) {
            String pkg = AppCatalog.packageName(context, app);
            if (pkg != null && !pkg.isEmpty()) names.add(pkg);
        }
        for (String pkg : SOURCE_PACKAGES) names.add(pkg);
        return names.toArray(new String[0]);
    }

    /** True when the package can be shown as the current screen. */
    private static boolean candidate(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        if (pkg.equals("com.android.systemui")) return false;
        if (pkg.equals("local.simple.tvhome") || pkg.startsWith("local.simple.tvhome.")) return false;
        if (pkg.equals("local.simple.tvoverlay") || pkg.startsWith("local.simple.tvoverlay.")) return false;
        return AppCatalog.forPackage(pkg) >= 0 || SignalSource.is(pkg);
    }
}
