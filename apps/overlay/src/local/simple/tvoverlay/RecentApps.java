package local.simple.tvoverlay;

import android.content.Context;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import java.util.Arrays;
import java.util.List;

/** Last actual use, not launcher display order or periodic sampling time. */
final class RecentApps {
    private final Context context;
    private final long[] used = new long[AppCatalog.IDS.length];
    RecentApps(Context context) {
        this.context = context;
        for (int i = 0; i < used.length; i++) used[i] = LauncherSupport.preferences(context).getLong("switch_used_" + i, 0);
    }
    void record(int app, long time) {
        if (app < 0 || app >= used.length || time <= used[app]) return;
        used[app] = time;
        LauncherSupport.preferences(context).edit().putLong("switch_used_" + app, time).apply();
    }
    int latest() {
        int latest = 0;
        for (int app = 1; app < used.length; app++) if (used[app] > used[latest]) latest = app;
        return latest;
    }
    int[] order(int current) {
        Integer[] sorted = new Integer[used.length];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i;
        Arrays.sort(sorted, (a, b) -> {
            if (a.equals(b)) return 0;
            if (a == current) return 1;
            if (b == current) return -1;
            int byUse = Long.compare(used[b], used[a]);
            return byUse == 0 ? Integer.compare(a, b) : byUse;
        });
        int[] order = new int[current >= 0 && current < used.length ? used.length - 1 : used.length];
        for (int i = 0; i < order.length; i++) order[i] = sorted[i];
        return order;
    }
    long[] readHistoricalUse() {
        long[] historical = new long[AppCatalog.IDS.length];
        try {
            UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 30L * 86400000, now);
            if (stats != null) for (UsageStats stat : stats) {
                int app = AppCatalog.forPackage(stat.getPackageName());
                if (app >= 0) historical[app] = Math.max(historical[app], stat.getLastTimeUsed());
            }
        } catch (RuntimeException ignored) { }
        return historical;
    }
}
