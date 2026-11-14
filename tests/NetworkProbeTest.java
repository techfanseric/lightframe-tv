package local.simple.tvoverlay;

/** Run with a plain JDK. Validates what a probe response proves. */
public final class NetworkProbeTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // The endpoint depends on the application.
        check(NetworkProbe.url(0).equals(NetworkProbe.YOUTUBE_URL));
        check(NetworkProbe.url(2).equals(NetworkProbe.NETFLIX_URL));

        // The expected answers prove the path works.
        check(NetworkProbe.reachable(0,204));
        check(NetworkProbe.reachable(2,200));

        // A redirect is the target host answering, so the path works. A
        // router that proxies the traffic may answer with 301 or 302.
        check(NetworkProbe.reachable(0,301));
        check(NetworkProbe.reachable(0,302));
        check(NetworkProbe.reachable(0,307));
        check(NetworkProbe.reachable(2,301));
        check(NetworkProbe.reachable(2,302));

        // A failure or a blocked answer does not.
        check(!NetworkProbe.reachable(0,404));
        check(!NetworkProbe.reachable(0,500));
        check(!NetworkProbe.reachable(2,403));
        check(!NetworkProbe.reachable(0,-1));
        check(!NetworkProbe.reachable(2,-1));

        // The wait runs out after the budget, measured from the request.
        check(!NetworkWaitPolicy.expired(0,0));
        check(!NetworkWaitPolicy.expired(119999,0));
        check(NetworkWaitPolicy.expired(120000,0));
        check(NetworkWaitPolicy.expired(130000,1000));

        // The proxy is asked for again while there is budget left.
        check(!NetworkWaitPolicy.startProxyAgain(0,0,0));
        check(!NetworkWaitPolicy.startProxyAgain(14999,0,0));
        check(NetworkWaitPolicy.startProxyAgain(15000,0,0));
        // The window closes before the budget runs out, so a slow tunnel is
        // still pulled up instead of being waited out.
        check(NetworkWaitPolicy.startProxyAgain(60000,0,45000));
        check(!NetworkWaitPolicy.startProxyAgain(90000,0,75000));
        check(!NetworkWaitPolicy.startProxyAgain(95000,0,75000));

        System.out.println("NetworkProbeTest passed");
    }
}
