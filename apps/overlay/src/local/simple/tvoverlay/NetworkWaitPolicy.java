package local.simple.tvoverlay;

/**
 * When the network check gives up and when it asks the proxy again.
 *
 * The rules are apart from the Android plumbing so they can be checked
 * without a device.
 */
final class NetworkWaitPolicy {
    /** The longest a single request may take. */
    static final long TOTAL_BUDGET_MS=120000;
    /** How long the proxy is asked for again while the tunnel comes up. */
    static final long PROXY_START_WINDOW_MS=90000;
    /** The gap between two attempts to pull the proxy up. */
    static final long PROXY_START_INTERVAL_MS=15000;

    private NetworkWaitPolicy() { }

    /** True when the wait has run out. */
    static boolean expired(long now, long started) {
        return now - started >= TOTAL_BUDGET_MS;
    }

    /**
     * True when the proxy should be asked for again.
     *
     * The proxy is asked for until most of the budget is gone, so a slow
     * tunnel is still pulled up instead of being waited out passively.
     */
    static boolean startProxyAgain(long now, long started, long lastStart) {
        if (now - started >= PROXY_START_WINDOW_MS) return false;
        return now - lastStart >= PROXY_START_INTERVAL_MS;
    }
}
