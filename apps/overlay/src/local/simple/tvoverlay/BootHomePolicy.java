package local.simple.tvoverlay;

/** Join the one initial HOME to this boot's pending request; subsequent HOME escapes. */
final class BootHomePolicy {
    static final long PENDING_BOOT_WINDOW_MS=125000;
    static boolean preservePendingBoot(boolean firstHomeForBoot,boolean settings,
            boolean automaticPending,boolean bootUnconsumed,int relayBoot,int currentBoot,
            long systemLaunchAt,long relaySentAt,long now){
        if(!firstHomeForBoot||settings||!automaticPending||!bootUnconsumed)return false;
        if(currentBoot<0||relayBoot!=currentBoot||systemLaunchAt<0||relaySentAt<0)return false;
        // Cold boot can delay HOME beyond a short debounce window. System recovery and HOME IPCs
        // belong to the same boot and may cross in either order; the runtime's
        // pending request owns their lifetime. The bound matches its cleanup
        // failsafe and rejects stale/future elapsed-time values.
        // Android supplies no trusted distinction between a delayed initial HOME
        // and the first HOME key before a relay exists. Only this once-per-boot
        // initial relay is joined; onNewIntent/settings are always explicit exits.
        if(now<systemLaunchAt||now<relaySentAt)return false;
        return now-systemLaunchAt<=PENDING_BOOT_WINDOW_MS
                &&now-relaySentAt<=PENDING_BOOT_WINDOW_MS;
    }
}
