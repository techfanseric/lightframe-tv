package local.simple.tvoverlay;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

/**
 * Decides what is on screen from the usage events of the television.
 *
 * The running processes cannot be asked on this firmware: the framework
 * hands back only the processes of the application itself. The importance
 * of a user cannot be asked either: the source player of the television
 * belongs to the system user, so that reading names the whole system user
 * and is the highest one there is, and it cannot tell the source player
 * apart from an application that is really in front.
 *
 * The usage events do answer. Every activity that comes to the front is
 * reported, and every activity that leaves the screen is reported too. An
 * activity is in front when it came to the front and has not been reported
 * as leaving the screen since. The activities are followed one by one,
 * because one application can show several of them at once.
 */
final class PictureState {
    /** One usage event, reduced to what the decision needs. */
    static final class Event {
        final String packageName;
        /** The activity the event belongs to, empty when it names none. */
        final String className;
        /** True when the activity came to the front. */
        final boolean foreground;
        final long time;

        Event(String packageName, String className, boolean foreground, long time) {
            this.packageName = packageName;
            this.className = className;
            this.foreground = foreground;
            this.time = time;
        }
    }

    /** The picture, with the moment it came to the front. */
    static final class Picture {
        final String packageName;
        final long time;

        Picture(String packageName, long time) {
            this.packageName = packageName;
            this.time = time;
        }
    }

    private PictureState() { }

    /**
     * The picture, or null when the events do not say. Only the candidates
     * are followed, so the launcher of the television and its own windows
     * never hide the picture.
     */
    static Picture picture(List<Event> events, Set<String> candidates) {
        if (events == null || candidates == null) return null;
        // The activities that are in front, with the moment they came there.
        Map<String, Long> inFront = new LinkedHashMap<>();
        Map<String, String> activityOf = new LinkedHashMap<>();
        for (Event event : events) {
            if (event == null) continue;
            String pkg = event.packageName;
            if (pkg == null || pkg.isEmpty()) continue;
            if (!candidates.contains(pkg)) continue;
            // The activity is the unit: one application can show several of
            // them, and only the one that leaves the screen gives it up.
            String key = pkg + "/" + (event.className == null ? "" : event.className);
            if (event.foreground) {
                inFront.put(key, event.time);
                activityOf.put(key, pkg);
            } else {
                inFront.remove(key);
            }
        }
        String bestPackage = "";
        long bestTime = Long.MIN_VALUE;
        for (Map.Entry<String, Long> entry : inFront.entrySet()) {
            if (entry.getValue() >= bestTime) {
                bestTime = entry.getValue();
                bestPackage = activityOf.get(entry.getKey());
            }
        }
        if (bestPackage == null || bestPackage.isEmpty()) return null;
        return new Picture(bestPackage, bestTime);
    }
}
