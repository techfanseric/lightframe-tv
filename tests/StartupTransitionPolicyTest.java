package local.simple.tvoverlay;

public final class StartupTransitionPolicyTest {
    private static final String TARGET="io.gh.reisxd.tizentube.cobalt";
    private static final String BASELINE="com.netflix.ninja";
    private static final String HOME="local.simple.tvhome.dock";
    private static final String OVERLAY="local.simple.tvoverlay.dock";
    private static int checks;
    private static void check(boolean value){if(!value)throw new AssertionError();checks++;}
    private static void expect(int expected,String pkg,long event,long dispatch,long now,long grace){
        expect(expected,pkg,event,dispatch,now,grace,0);
    }
    private static void expect(int expected,String pkg,long event,long dispatch,long now,long grace,long homeGrace){
        int actual=StartupTransitionPolicy.foreground(pkg,TARGET,BASELINE,HOME,OVERLAY,
                event,100000,dispatch,now,grace,homeGrace);
        if(expected!=actual)throw new AssertionError("Expected "+expected+" but got "+actual+" for "+pkg);
        checks++;
    }
    public static void main(String[] args){
        int keep=StartupTransitionPolicy.KEEP,reveal=StartupTransitionPolicy.REVEAL,cancel=StartupTransitionPolicy.CANCEL;
        // Cached state cannot dismiss or cancel a new request.
        expect(keep,BASELINE,99999,0,20000,0);
        expect(keep,null,100001,0,20000,0);
        expect(keep,"",100001,0,20000,0);
        expect(keep,HOME,100001,0,20000,0);
        expect(keep,OVERLAY,100001,0,20000,0);
        expect(keep,"com.android.systemui",100001,0,20000,0);
        // Seeing YouTube before network verification does not bypass the gate.
        expect(keep,TARGET,100001,0,20000,0);
        expect(keep,"com.follow.clash",100001,0,20000,0);
        // QuickAction may return to the original player. Only that package and
        // that actual QuickAction's finite elapsed-time window are exempt.
        expect(keep,BASELINE,100001,0,20000,24000);
        expect(keep,BASELINE,100001,0,24000,24000);
        expect(cancel,BASELINE,100001,0,24001,24000);
        expect(cancel,BASELINE,100001,0,20000,0);
        expect(cancel,"com.android.settings",100001,0,20000,24000);
        expect(cancel,"com.xiaodianshi.tv.yst",100001,0,20000,24000);
        // A HOME relay returning to its underlying task is independent of VPN
        // startup. It exempts only the baseline, and only for its shorter window.
        expect(keep,BASELINE,100001,0,20000,0,21000);
        expect(keep,BASELINE,100001,0,21000,0,21000);
        expect(cancel,BASELINE,100001,0,21001,0,21000);
        expect(cancel,"com.android.settings",100001,0,20000,0,21000);
        expect(cancel,"com.xiaodianshi.tv.yst",100001,0,20000,0,21000);
        // Dispatch starts a new boundary; even a target event from before it is stale.
        expect(keep,TARGET,100099,100100,20000,0);
        expect(reveal,TARGET,100100,100100,20000,0);
        expect(reveal,TARGET,100101,100100,20000,0);
        expect(cancel,BASELINE,100101,100100,20000,24000);
        expect(cancel,"com.follow.clash",100101,100100,20000,24000);
        expect(keep,HOME,100101,100100,20000,0);
        expect(cancel,BASELINE,100101,100100,20000,0,21000);
        // Overlapping queries must not reinterpret a HOME baseline event after
        // its grace expires. Different packages can genuinely share one ms.
        check(StartupTransitionPolicy.isNewEvent(BASELINE,100001,100000,null,0));
        check(!StartupTransitionPolicy.isNewEvent(BASELINE,100001,100000,BASELINE,100001));
        check(!StartupTransitionPolicy.isNewEvent(TARGET,100000,100000,BASELINE,100001));
        check(StartupTransitionPolicy.isNewEvent(TARGET,100001,100000,BASELINE,100001));
        check(StartupTransitionPolicy.isNewEvent(BASELINE,100002,100000,BASELINE,100001));
        check(!StartupTransitionPolicy.isNewEvent(BASELINE,99999,100000,null,0));
        check(!StartupTransitionPolicy.isNewEvent(null,100001,100000,null,0));
        check(!StartupTransitionPolicy.isNewEvent("",100001,100000,null,0));
        check(!StartupTransitionPolicy.isNewEvent(BASELINE,100001,0,null,0));
        check(StartupTransitionPolicy.isNewEvent(BASELINE,100000,100000,null,0));
        int repeatedDecision=keep;
        if(StartupTransitionPolicy.isNewEvent(BASELINE,100001,100000,BASELINE,100001)){
            repeatedDecision=StartupTransitionPolicy.foreground(BASELINE,TARGET,BASELINE,HOME,OVERLAY,
                    100001,100000,0,22000,0,21000);
        }
        check(repeatedDecision==keep);
        System.out.println("StartupTransitionPolicyTest passed ("+checks+" checks)");
    }
}
