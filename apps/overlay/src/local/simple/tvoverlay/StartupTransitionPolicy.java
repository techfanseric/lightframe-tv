package local.simple.tvoverlay;

/** Foreground-event policy for one bounded startup request; no Android or UI state. */
final class StartupTransitionPolicy {
    static final int KEEP=0, REVEAL=1, CANCEL=2;
    static final long PROXY_RETURN_GRACE_MS=4000;
    static final long HOME_RELAY_RETURN_GRACE_MS=1000;

    /** UsageStats polls overlap; a consumed event must not change meaning later. */
    static boolean isNewEvent(String pkg,long eventTime,long requestAt,String lastPackage,long lastTime){
        if(pkg==null||pkg.isEmpty()||requestAt<=0||eventTime<requestAt||eventTime<lastTime)return false;
        return eventTime!=lastTime||!pkg.equals(lastPackage);
    }

    /**
     * Wall-clock event/request times are compared only with each other. The proxy
     * grace uses elapsed time and must be set by an actual QuickAction request,
     * never extended merely because a Clash foreground event was observed. HOME
     * has a separate short grace for its transparent relay moving to the back.
     */
    static int foreground(String pkg,String targetPackage,String baselinePackage,
            String homePackage,String overlayPackage,long eventTime,long requestedAt,
            long dispatchedAt,long nowElapsed,long proxyReturnUntilElapsed,long homeRelayReturnUntilElapsed){
        if(pkg==null||pkg.isEmpty()||requestedAt<=0||eventTime<requestedAt)return KEEP;
        if(pkg.equals(homePackage)||pkg.equals(overlayPackage)||pkg.equals("com.android.systemui"))return KEEP;
        if(dispatchedAt>0){
            if(eventTime<dispatchedAt)return KEEP;
            // A new target event ends the transition, not a claim of playback.
            return pkg.equals(targetPackage)?REVEAL:CANCEL;
        }
        // Network readiness is owned by the gate, never by a foreground event.
        if(pkg.equals(targetPackage)||pkg.equals("com.follow.clash"))return KEEP;
        if(pkg.equals(baselinePackage)&&nowElapsed>=0
                &&((proxyReturnUntilElapsed>0&&nowElapsed<=proxyReturnUntilElapsed)
                ||(homeRelayReturnUntilElapsed>0&&nowElapsed<=homeRelayReturnUntilElapsed)))return KEEP;
        return CANCEL;
    }
}
