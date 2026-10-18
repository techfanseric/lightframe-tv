package local.simple.tvoverlay;

import android.app.ActivityManager;
import android.content.Context;
import java.lang.reflect.Method;
import java.util.List;

/** Read-only status. An unavailable system query never becomes a claim that an app is stopped. */
final class AppRuntimeState {
    static String[] read(Context context) {
        String[] states = {"状态未知", "状态未知", "状态未知"};
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return states;
        Method uidImportance = null;
        try {
            // This firmware may expose the system API to usage-access holders. No API exemptions are installed.
            uidImportance = ActivityManager.class.getMethod("getUidImportance", int.class);
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        for (int app = 0; app < states.length; app++) {
            String pkg = AppCatalog.packageName(context, app);
            try {
                if (uidImportance == null) continue;
                int uid = context.getPackageManager().getApplicationInfo(pkg, 0).uid;
                int importance = ((Integer) uidImportance.invoke(manager, uid)).intValue();
                states[app] = importance <= 100 ? "正在使用" : importance >= 1000 ? "未运行" : importance >= 400 ? "后台缓存" : "后台驻留";
                android.util.Log.i("SwitcherState", "uidImportance " + pkg + "=" + importance);
            } catch (Exception | LinkageError ignored) { }
        }
        try {
            List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
            if (processes != null) for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.pkgList == null) continue;
                for (String pkg : process.pkgList) {
                    int app = AppCatalog.forPackage(pkg);
                    if (app >= 0 && "状态未知".equals(states[app])) states[app] = process.importance >= 400 ? "后台缓存" : "后台驻留";
                }
            }
        } catch (RuntimeException ignored) { }
        return states;
    }
}
