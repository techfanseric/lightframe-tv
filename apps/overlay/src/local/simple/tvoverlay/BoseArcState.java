package local.simple.tvoverlay;

/**
 * What the speaker is playing right now.
 *
 * The speaker answers with a document that names the account only while a
 * product source is selected. When it is idle it answers with STANDBY and no
 * account at all, which is different from a product source that carries no
 * audio.
 */
final class BoseArcState {
    /** The speaker is idle and holds no source. */
    static final String IDLE = "";

    private BoseArcState() { }

    /**
     * The account the speaker is playing, or an empty string when it holds
     * no product source.
     *
     * @param xml the document from the speaker
     */
    static String account(String xml) {
        if (xml == null || xml.isEmpty()) return IDLE;
        // An idle speaker names no account. It is not on the ARC channel and
        // it is not on its own socket either.
        if (xml.contains("source=\"STANDBY\"") && !xml.contains("sourceAccount=\"")) return IDLE;
        int index = xml.indexOf("sourceAccount=\"");
        if (index < 0) return IDLE;
        int start = index + "sourceAccount=\"".length();
        int end = xml.indexOf('"', start);
        return end < 0 ? IDLE : xml.substring(start, end);
    }

    /**
     * True when the speaker already plays the wanted account.
     *
     * An idle speaker does not: it has to be told which channel to take.
     */
    static boolean satisfied(String current, String wanted) {
        if (wanted == null || wanted.isEmpty()) return true;
        return wanted.equals(current);
    }
}
