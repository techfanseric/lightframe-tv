package local.simple.tvoverlay;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Reads what is on screen right now.
 *
 * The running processes cannot be asked on this firmware: the framework
 * hands back only the processes of the application itself. The importance
 * of a user cannot be asked either: the source player of the television
 * belongs to the system user, so that reading names the whole system user
 * and is the highest one there is, and it cannot tell the source player
 * apart from an application that is really in front.
 *
 * The usage events do answer. They report every activity that comes to the
 * front, so the last one that came to the front is the picture.
 */
final class CurrentApp {
    /** How far back the events are read, in milliseconds. */
    private static final long WINDOW_MS = 30L * 60L * 1000L;
    /** The television's own source player, and the framework's own. */
    private static final String[] SOURCE_PACKAGES = {
        SignalSource.XIAOMI_SOURCE_PACKAGE,
        "com.mediatek.tvinput"
    };

    /** What the events say about the picture. */
    static final class Reading {
        final String packageName;
        /** The moment the picture came to the front. */
        final long time;

        Reading(String packageName, long time) {
            this.packageName = packageName;
            this.time = time;
        }
    }

    private CurrentApp() { }

    /**
     * The package the user is looking at, or an empty string when it cannot
     * be established. The launcher's own windows and the system interface
     * are never reported, because they sit on top of the picture.
     */
    static String foreground(Context context) {
        Reading reading = read(context);
        if (reading == null || reading.packageName.isEmpty()) return "";
        android.util.Log.i("HomeOverlay", "current app=" + reading.packageName + " from usage events");
        return reading.packageName;
    }

    /**
     * The picture and the moment it came to the front, or null when the
     * events do not say.
     */
    static Reading read(Context context) {
        PictureState.Picture picture = PictureState.picture(events(context), candidates(context));
        if (picture == null) return null;
        return new Reading(picture.packageName, picture.time);
    }

    /** The usage events of the television, in the order they happened. */
    private static List<PictureState.Event> events(Context context) {
        java.util.List<PictureState.Event> events = new java.util.ArrayList<>();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return events;
        long now = System.currentTimeMillis();
        try {
            UsageEvents usage = manager.queryEvents(now - WINDOW_MS, now);
            UsageEvents.Event event = new UsageEvents.Event();
            while (usage.hasNextEvent()) {
                usage.getNextEvent(event);
                int type = event.getEventType();
                if (type != UsageEvents.Event.MOVE_TO_FOREGROUND
                        && type != UsageEvents.Event.MOVE_TO_BACKGROUND) continue;
                events.add(new PictureState.Event(event.getPackageName(), event.getClassName(),
                        type == UsageEvents.Event.MOVE_TO_FOREGROUND, event.getTimeStamp()));
            }
        } catch (RuntimeException unavailable) {
            return events;
        }
        return events;
    }

    /**
     * Every package that can be the picture. The launcher's own windows and
     * the system interface are left out, because they sit on top of the
     * picture and would hide it.
     */
    private static Set<String> candidates(Context context) {
        Set<String> names = new LinkedHashSet<>();
        for (int app = 0; app < AppCatalog.IDS.length; app++) {
            String pkg = AppCatalog.packageName(context, app);
            if (pkg != null && !pkg.isEmpty()) names.add(pkg);
        }
        for (String pkg : SOURCE_PACKAGES) names.add(pkg);
        return names;
    }
}
