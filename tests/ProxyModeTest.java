package local.simple.tvoverlay;

/** Run with a plain JDK. Validates proxy-mode parsing, labels and cycling. */
public final class ProxyModeTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // The cycle order is stable and returns home after a full turn.
        check(ProxyMode.next(ProxyMode.AUTO).equals(ProxyMode.DIRECT));
        check(ProxyMode.next(ProxyMode.DIRECT).equals(ProxyMode.OFF));
        check(ProxyMode.next(ProxyMode.OFF).equals(ProxyMode.AUTO));
        check(ProxyMode.next("nonsense").equals(ProxyMode.AUTO));
        check(ProxyMode.next(null).equals(ProxyMode.AUTO));

        // Labels stay short enough for the compact settings tile.
        check(ProxyMode.label(ProxyMode.AUTO).equals("自动"));
        check(ProxyMode.label(ProxyMode.DIRECT).equals("直连"));
        check(ProxyMode.label(ProxyMode.OFF).equals("关闭"));
        check(ProxyMode.label("nonsense").equals("自动"));

        // Only the automatic mode may pull FlClash up or demand a local VPN.
        check(ProxyMode.MODES.length==3);
        check(ProxyMode.AUTO.equals(ProxyMode.MODES[0]));
        check(ProxyMode.DIRECT.equals(ProxyMode.MODES[1]));
        check(ProxyMode.OFF.equals(ProxyMode.MODES[2]));

        // Resolution: an unset mode with no legacy flag stays on the historical
        // automatic behaviour, and an unknown value never silently disables it.
        check(ProxyMode.resolve(null,false).equals(ProxyMode.AUTO));
        check(ProxyMode.resolve(null,true).equals(ProxyMode.DIRECT));
        check(ProxyMode.resolve(ProxyMode.AUTO,false).equals(ProxyMode.AUTO));
        check(ProxyMode.resolve(ProxyMode.DIRECT,false).equals(ProxyMode.DIRECT));
        check(ProxyMode.resolve(ProxyMode.OFF,false).equals(ProxyMode.OFF));
        check(ProxyMode.resolve("nonsense",false).equals(ProxyMode.AUTO));
        check(ProxyMode.resolve("",true).equals(ProxyMode.AUTO));
        // An explicit stored mode wins over the legacy flag.
        check(ProxyMode.resolve(ProxyMode.AUTO,true).equals(ProxyMode.AUTO));
        check(ProxyMode.resolve(ProxyMode.OFF,true).equals(ProxyMode.OFF));

        System.out.println("ProxyModeTest passed");
    }
}
