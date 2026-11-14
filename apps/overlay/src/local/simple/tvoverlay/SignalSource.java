package local.simple.tvoverlay;

/**
 * Detects the TV's own HDMI / external-source player.
 *
 * While that player is in front the screen shows a foreign picture, so the
 * launcher's own overlays must stay out of the way.
 */
final class SignalSource {
    static final String XIAOMI_SOURCE_PACKAGE = "com.xiaomi.mitv.tvplayer";

    private SignalSource() { }

    static boolean is(CharSequence packageName) {
        if (packageName == null) return false;
        String value = packageName.toString();
        if (XIAOMI_SOURCE_PACKAGE.equals(value)) return true;
        // Some Xiaomi firmware keeps the external player under a vendor prefix.
        return value.startsWith(XIAOMI_SOURCE_PACKAGE + ".");
    }
}
