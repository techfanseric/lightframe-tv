package local.simple.tvoverlay;

/** Resolves a fixed app or the last successfully used video app. No launching or settings writes. */
final class StartupChoice {
    /** Policy and remembered value that point at an attached external source. */
    static final String SOURCE_ID = "source";
    /** Policy that keeps the television from taking over on its own. */
    static final String OFF_ID = "off";
    /** Policy that follows the last application that was used. */
    static final String LAST_ID = "last";
    /** Resolved target that is a source rather than one of the applications. */
    static final int SOURCE = -2;

    static int resolve(String policy, String lastApp, int recent, String[] ids) {
        if (OFF_ID.equals(policy)) return -1;
        if (SOURCE_ID.equals(policy)) return SOURCE;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(policy)) return i;
        if (SOURCE_ID.equals(lastApp)) return SOURCE;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(lastApp)) return i;
        return recent >= 0 && recent < ids.length ? recent : 0;
    }

    /** Index of an application policy, or -1 when the policy is not an app. */
    static int index(String value, String[] ids) {
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(value)) return i;
        return -1;
    }

    /**
     * The next policy in the settings cycle. The attached source is offered
     * after the applications, then the television is left alone, and the
     * cycle returns to "follow the last one".
     */
    static String next(String policy, String[] ids) {
        if (OFF_ID.equals(policy)) return LAST_ID;
        if (SOURCE_ID.equals(policy)) return OFF_ID;
        if (!LAST_ID.equals(policy)) {
            int index = index(policy, ids);
            if (index < 0) return LAST_ID;
            return index == ids.length - 1 ? SOURCE_ID : ids[index + 1];
        }
        return ids.length > 0 ? ids[0] : LAST_ID;
    }

    /** True when the value names a source rather than an application. */
    static boolean isSource(String value) {
        return SOURCE_ID.equals(value);
    }

    /** True when the value means the television is left alone. */
    static boolean isOff(String value) {
        return OFF_ID.equals(value);
    }

    /**
     * The policy in force. When the takeover is switched off the remembered
     * target is kept, so the tile can show that the television stays alone
     * and can bring the takeover back.
     */
    static String effective(String policy, boolean enabled) {
        return enabled ? policy : OFF_ID;
    }

    /**
     * The place in the row where an attached source belongs: directly
     * behind the video entry. When the video entry is the one on screen it
     * is not in the row, so the source follows the first entry that is.
     *
     * @param visible the applications that actually reach the row, in order
     * @param video   the index of the video entry in the catalog
     */
    static int sourceSlot(int[] visible, int video) {
        for (int i = 0; i < visible.length; i++) if (visible[i] == video) return i + 1;
        return visible.length > 0 ? 1 : 0;
    }
}
