package local.simple.tvoverlay;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Which applications appear in the row.
 *
 * Netflix is off by default because the television is watched through the
 * attached console, where the same service is offered with a better
 * experience. The entry can be brought back at any time from the settings.
 */
final class AppVisibility {
    /** The applications the owner has asked to see again. */
    static final String KEY_SHOWN = "shown_apps";
    /** The applications that are hidden unless the owner asks for them. */
    static final String[] HIDDEN_BY_DEFAULT = {"netflix"};

    private AppVisibility() { }

    /** True when the application has an entry in the row. */
    static boolean visible(Context context, int app) {
        if (app < 0 || app >= AppCatalog.IDS.length) return false;
        if (!hiddenByDefault(app)) return true;
        SharedPreferences prefs = LauncherSupport.preferences(context);
        String shown = prefs.getString(KEY_SHOWN, "");
        for (String id : shown.split(",")) {
            if (AppCatalog.IDS[app].equals(id)) return true;
        }
        return false;
    }

    /** True when the application is hidden unless the owner asks for it. */
    static boolean hiddenByDefault(int app) {
        if (app < 0 || app >= AppCatalog.IDS.length) return false;
        for (String id : HIDDEN_BY_DEFAULT) if (AppCatalog.IDS[app].equals(id)) return true;
        return false;
    }

    /** True when the named application is hidden unless the owner asks for it. */
    static boolean hiddenByDefault(String id) {
        for (String hidden : HIDDEN_BY_DEFAULT) if (hidden.equals(id)) return true;
        return false;
    }

    /** Turns the entry on or off and remembers the choice. */
    static void setVisible(Context context, int app, boolean visible) {
        if (app < 0 || app >= AppCatalog.IDS.length) return;
        String id = AppCatalog.IDS[app];
        SharedPreferences prefs = LauncherSupport.preferences(context);
        StringBuilder shown = new StringBuilder();
        for (String existing : prefs.getString(KEY_SHOWN, "").split(",")) {
            if (existing.isEmpty() || existing.equals(id)) continue;
            if (shown.length() > 0) shown.append(',');
            shown.append(existing);
        }
        if (visible) {
            if (shown.length() > 0) shown.append(',');
            shown.append(id);
        }
        prefs.edit().putString(KEY_SHOWN, shown.toString()).apply();
    }

    /** The state shown in the settings tile. */
    static String label(Context context, int app) {
        if (!hiddenByDefault(app)) return "";
        return visible(context, app) ? "已显示" : "已隐藏";
    }
}
