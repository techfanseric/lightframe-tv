package local.simple.tvoverlay;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal SoundTouch client that keeps the soundbar's ARC input selected.
 *
 * When another device grabs the ARC channel the speaker stops following the
 * TV. Selecting PRODUCT/HDMI_1 again returns the TV's own audio to it. The
 * speaker address lives in local preferences and is never part of Git.
 */
final class BoseArc {
    static final String KEY_HOST = "bose_arc_host";
    static final String KEY_PORT = "bose_arc_port";
    /** Account that carries the television's own audio, that is the ARC channel. */
    static final String KEY_ACCOUNT = "bose_arc_account";
    /** Account of the device plugged into the speaker's own HDMI socket. */
    static final String KEY_HDMI_ACCOUNT = "bose_hdmi_account";
    /** The speaker lists its ARC input as "TV" and its HDMI sockets as "HDMI_n". */
    static final String DEFAULT_ACCOUNT = "TV";
    static final String DEFAULT_HDMI_ACCOUNT = "HDMI_1";
    private static final int TIMEOUT_MS = 2500;

    private BoseArc() { }

    static boolean configured(String host) {
        return host != null && !host.trim().isEmpty();
    }

    /** The account the speaker is playing right now, or an empty string. */
    static String currentAccount(String host, int port) {
        return BoseArcState.account(get(host, port, "/now_playing"));
    }

    /**
 * Selects the given source account. Returns true when the request was
 * accepted; the speaker may still be settling on the new source.
 */
    static boolean select(String host, int port, String account) {
        if (account == null || account.isEmpty()) return false;
        String body = "<ContentItem source=\"PRODUCT\" sourceAccount=\"" + account
                + "\" isPresetable=\"false\" />";
        return post(host, port, "/select", body);
    }

    /**
     * The account that should be playing for the given picture. The speaker
     * keeps two channels apart: its ARC input carries the television's own
     * audio, while its HDMI socket carries the device behind it.
     */
    static String target(boolean externalSource, String arcAccount, String hdmiAccount) {
        String wanted = externalSource ? hdmiAccount : arcAccount;
        return wanted == null ? "" : wanted;
    }

    private static String get(String host, int port, String path) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://" + host + ":" + port + path).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            if (connection.getResponseCode() != 200) return null;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[2048];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    if (out.size() + count > 65536) break;
                    out.write(buffer, 0, count);
                }
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception unavailable) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean post(String host, int port, String path, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://" + host + ":" + port + path).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/xml");
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (java.io.OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }
            int code = connection.getResponseCode();
            // The speaker answers 200 with a plain status document on success.
            return code == 200;
        } catch (Exception unavailable) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
