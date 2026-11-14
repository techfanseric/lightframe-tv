package local.simple.tvoverlay;

/**
 * The colour of a dock entry.
 *
 * A tile colour is an ARGB value and therefore usually a negative number, so
 * "the entry carries its own colour" cannot be told from the value itself.
 * The flag is kept apart from the value.
 */
final class DockColors {
    /**
     * The tile of an attached source. The white Apple mark sits on it, so
     * the tile is a clear grey rather than a pale one that would swallow
     * the mark.
     */
    static final int SOURCE_TILE = 0xffb9bec7;

    /** The colour of an entry that has no colour of its own. */
    static int forKind(String title) {
        if ("YouTube".equals(title)) return 0xfff5f4f1;
        if ("Netflix".equals(title)) return 0xff17171b;
        if ("哔哩哔哩".equals(title)) return 0xffed5188;
        return 0xff1b6773;
    }

    /** The colour shown for an entry. */
    static int tileColor(boolean tinted, int color, String title) {
        return tinted ? color : forKind(title);
    }

    private DockColors() { }
}
