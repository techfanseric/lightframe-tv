package local.simple.tvoverlay;

/**
 * What a probe response says about the network.
 *
 * The rules are kept apart from the Android plumbing so they can be checked
 * without a device.
 */
final class NetworkProbe {
    /** The endpoint that answers with 204 when YouTube is reachable. */
    static final String YOUTUBE_URL = "https://www.youtube.com/generate_204";
    /** The endpoint that answers with a page when Netflix is reachable. */
    static final String NETFLIX_URL = "https://www.netflix.com/robots.txt";

    private NetworkProbe() { }

    /** The endpoint used for the given application. */
    static String url(int app) {
        return app == 2 ? NETFLIX_URL : YOUTUBE_URL;
    }

    /**
     * True when the response shows the target is reachable.
     *
     * A redirect counts as reachable: a router that proxies the traffic may
     * answer with 301 or 302 instead of the expected status, and that is a
     * working network rather than a broken one.
     */
    static boolean reachable(int app, int response) {
        if (response < 0) return false;
        // generate_204 answers 204 and robots.txt answers 200. A redirect is
        // still the target host answering, which proves the path works.
        return response >= 200 && response < 400;
    }
}
