package local.simple.tvoverlay;

import android.net.TrafficStats;

/**
 * Device-wide byte counters.
 *
 * In direct mode the traffic never touches a Clash core on this device, so the
 * controller's counters stay empty. The system counters still see every byte
 * the TV sends and receives, which is what the strip should show.
 */
final class DeviceTraffic {
    private DeviceTraffic() { }

    /** Bytes this UID sent and received since boot. */
    static long uidBytes() {
        try {
            int uid = android.os.Process.myUid();
            return Math.max(0, TrafficStats.getUidRxBytes(uid)) + Math.max(0, TrafficStats.getUidTxBytes(uid));
        } catch (RuntimeException unavailable) {
            return -1;
        }
    }

    static long receivedBytes() {
        try {
            return Math.max(0, TrafficStats.getTotalRxBytes());
        } catch (RuntimeException unavailable) {
            return -1;
        }
    }

    static long sentBytes() {
        try {
            return Math.max(0, TrafficStats.getTotalTxBytes());
        } catch (RuntimeException unavailable) {
            return -1;
        }
    }
}
