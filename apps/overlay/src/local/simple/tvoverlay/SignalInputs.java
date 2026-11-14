package local.simple.tvoverlay;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads which external sources are attached to the television.
 *
 * The vendor's own source screen asks the framework for the state of every
 * hardware input. A port reports CONNECTED only while a device is attached;
 * an unused port reports CONNECTED_STANDBY. That is the same rule used here,
 * so the launcher never claims a source is connected when it is not.
 */
final class SignalInputs {
    static final int MAX_PORTS = 4;
    /** Vendor source id of HDMI 1 is 23, so the port is the source id minus 22. */
    static final int SOURCE_OFFSET = 22;

    private static final Pattern HDMI_PORT = Pattern.compile("HDMI\\s*[-:]?\\s*([0-9])(?![0-9])",
            Pattern.CASE_INSENSITIVE);
    /** Generic names the system uses for a device that has not announced itself. */
    private static final Pattern GENERIC_NAME = Pattern.compile("^[A-Za-z]+(_\\d+)?$");

    /** Last name a port was seen with, so a temporary generic name cannot replace it. */
    private static final Map<Integer, String> KNOWN_NAMES = new ConcurrentHashMap<>();

    /** One physical HDMI port and what the framework reports about it. */
    static final class Port {
        final int port;
        final String label;
        /** Name of the device behind the port, empty when it names no device. */
        final String device;
        final boolean connected;
        final boolean current;

        Port(int port, String label, String device, boolean connected, boolean current) {
            this.port = port;
            this.label = label;
            this.device = device;
            this.connected = connected;
            this.current = current;
        }

        /** Name shown on the entry: the device when known, otherwise the port. */
        String title() {
            return device.isEmpty() ? label : device;
        }
    }

    private SignalInputs() { }

    /** Port number carried by a vendor input label, or -1 when it is not an HDMI port. */
    static int portFromLabel(CharSequence label) {
        if (label == null) return -1;
        Matcher match = HDMI_PORT.matcher(label.toString());
        return match.find() ? Integer.parseInt(match.group(1)) : -1;
    }

    /** Short summary for the settings tile, for example "HDMI 2 当前". */
    static String summary(List<Port> ports) {
        if (ports == null || ports.isEmpty()) return "未接入";
        StringBuilder text = new StringBuilder();
        for (Port port : ports) {
            if (!port.connected) continue;
            if (text.length() > 0) text.append(' ');
            text.append(port.label);
            if (port.current) text.append(" 当前");
        }
        return text.length() == 0 ? "未接入" : text.toString();
    }

    /**
     * Reads every HDMI port with its connection state. A port counts as
     * connected when any of the inputs behind it reports CONNECTED. A port
     * without a device reports CONNECTED_STANDBY and stays dark, which is
     * what the vendor's own source screen shows.
     */
    static List<Port> read(Context context) {
        List<Port> ports = new ArrayList<>();
        TvInputManager manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        if (manager == null) return ports;
        int current = currentPort(context);
        boolean[] connected = new boolean[MAX_PORTS + 1];
        String[] labels = new String[MAX_PORTS + 1];
        String[] devices = new String[MAX_PORTS + 1];
        for (int port = 0; port <= MAX_PORTS; port++) devices[port] = "";
        try {
            for (TvInputInfo info : manager.getTvInputList()) {
                int port = portOf(context, manager, info);
                String label = labelOf(context, info);
                int state;
                try {
                    state = manager.getInputState(info.getId());
                } catch (RuntimeException unavailable) {
                    state = -1;
                }
                if (port < 1 || port > MAX_PORTS) continue;
                // The port keeps its own name; a device is named separately.
                if (labels[port] == null && portFromLabel(label) > 0) labels[port] = label;
                if (portFromLabel(label) < 0) {
                    String name = deviceName(label);
                    if (!name.isEmpty()) {
                        KNOWN_NAMES.put(port, name);
                        devices[port] = name;
                    }
                }
                if (state == TvInputManager.INPUT_STATE_CONNECTED) connected[port] = true;
            }
        } catch (RuntimeException unavailable) {
            return ports;
        }
        for (int port = 1; port <= MAX_PORTS; port++) {
            String label = labels[port] != null ? labels[port] : "HDMI " + port;
            String device = devices[port].isEmpty() ? KNOWN_NAMES.get(port) : devices[port];
            ports.add(new Port(port, label, device == null ? "" : device, connected[port], port == current));
        }
        return ports;
    }

    /**
     * A name worth showing. The system falls back to names such as
     * "Playback_1" when a device has not announced itself, and those carry no
     * information, so they are never used.
     */
    private static String deviceName(String label) {
        if (label == null || label.isEmpty()) return "";
        if (GENERIC_NAME.matcher(label.trim()).matches()) return "";
        return label.trim();
    }

    /**
     * Physical port behind an input. Named devices such as "Apple TV" hang
     * off their socket, so the parent label is resolved first. An input whose
     * own label names no port belongs to no socket and is ignored.
     */
    private static int portOf(Context context, TvInputManager manager, TvInputInfo info) {
        if (info == null) return -1;
        String parentId = info.getParentId();
        if (parentId != null && manager != null) {
            try {
                TvInputInfo parent = manager.getTvInputInfo(parentId);
                int port = portFromLabel(labelOf(context, parent));
                if (port > 0) return port;
            } catch (RuntimeException ignored) { }
        }
        return portFromLabel(labelOf(context, info));
    }

    private static String labelOf(Context context, TvInputInfo info) {
        if (info == null) return null;
        try {
            CharSequence label = info.loadLabel(context);
            if (label != null && label.length() > 0) return label.toString();
        } catch (RuntimeException ignored) { }
        return null;
    }

    /** The source the television is showing right now, as a port number. */
    static int currentPort(Context context) {
        // The vendor keeps the last source in two places and one of them can
        // hold a stale value, so the one that names an HDMI port wins.
        int last = readInt(context, "mitv.tvplayer.hdmi.last.source");
        int current = readInt(context, "current_source_for_tvplayer");
        int port = portOfSource(last);
        if (port > 0) return port;
        return portOfSource(current);
    }

    private static int portOfSource(int source) {
        int port = source - SOURCE_OFFSET;
        return port >= 1 && port <= MAX_PORTS ? port : -1;
    }

    /**
     * Reads one setting. The vendor keeps it under the system table, but a
     * restricted entry is only readable as a global value, so both are tried.
     */
    private static int readInt(Context context, String name) {
        int value = -1;
        try {
            value = Settings.Global.getInt(context.getContentResolver(), name, -1);
        } catch (RuntimeException unavailable) {
        }
        if (value >= 0) return value;
        try {
            value = Settings.System.getInt(context.getContentResolver(), name, -1);
        } catch (RuntimeException unavailable) {
        }
        return value;
    }
}
