package local.simple.tvoverlay;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DashboardClient implements AutoCloseable {
    public interface Callback {
        void onSnapshot(Snapshot snapshot);
    }

    public static final class Snapshot {
        public final String node;
        public final Integer delayMs;
        public final long up;
        public final long down;
        public final boolean available;

        private Snapshot(String node, Integer delayMs, long up, long down, boolean available) {
            this.node = node;
            this.delayMs = delayMs;
            this.up = up;
            this.down = down;
            this.available = available;
        }

        public static Snapshot unavailable() {
            return new Snapshot(null, null, -1, -1, false);
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean closed;
    private volatile HttpURLConnection activeConnection;

    public void readSnapshot(final String secret, final Callback callback) {
        if (callback == null || closed || !busy.compareAndSet(false, true)) return;
        try {
            executor.execute(() -> {
                Snapshot result;
                try {
                    if (secret == null || secret.isEmpty()) throw new IOException();
                    JSONObject proxies = new JSONObject(request("/proxies", secret, false))
                            .optJSONObject("proxies");
                    if (proxies == null) throw new IOException();
                    JSONObject traffic = new JSONObject(request("/traffic", secret, true));
                    String node = resolveNode(proxies, chooseGroup(proxies));
                    Integer delay = lastDelay(node == null ? null : proxies.optJSONObject(node));
                    result = new Snapshot(node, delay, nonnegative(traffic, "up"),
                            nonnegative(traffic, "down"), true);
                } catch (Exception ignored) {
                    result = Snapshot.unavailable();
                } finally {
                    busy.set(false);
                }
                final Snapshot delivered = result;
                main.post(() -> {
                    if (!closed) callback.onSnapshot(delivered);
                });
            });
        } catch (RejectedExecutionException ignored) {
            busy.set(false);
        }
    }

    private String request(String path, String secret, boolean firstLine) throws Exception {
        if (closed) throw new IOException();
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:9090" + path).openConnection();
        activeConnection = connection;
        try {
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(firstLine ? 2500 : 1500);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Authorization", "Bearer " + secret);
            connection.setRequestProperty("Accept", "application/json");
            if (closed || connection.getResponseCode() != 200) throw new IOException();
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                int limit = firstLine ? 8192 : 2 * 1024 * 1024;
                if (firstLine) {
                    int value;
                    while ((value = input.read()) != -1 && value != '\n') {
                        if (output.size() >= limit) throw new IOException();
                        output.write(value);
                    }
                } else {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (output.size() + count > limit) throw new IOException();
                        output.write(buffer, 0, count);
                    }
                }
                if (output.size() == 0) throw new IOException();
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
            activeConnection = null;
        }
    }

    private static String chooseGroup(JSONObject proxies) {
        if (proxies.optJSONObject("🚀 国外流量") != null) return "🚀 国外流量";
        if (proxies.optJSONObject("GLOBAL") != null) return "GLOBAL";
        String onlyUrlTest = null;
        Iterator<String> keys = proxies.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject proxy = proxies.optJSONObject(key);
            if (proxy != null && "urltest".equalsIgnoreCase(proxy.optString("type"))
                    && !proxy.optString("now").isEmpty()) {
                if (onlyUrlTest != null) return null;
                onlyUrlTest = key;
            }
        }
        return onlyUrlTest;
    }

    private static String resolveNode(JSONObject proxies, String name) {
        Set<String> seen = new HashSet<>();
        while (name != null && !name.isEmpty() && seen.add(name)) {
            JSONObject proxy = proxies.optJSONObject(name);
            if (proxy == null) return null;
            String next = proxy.optString("now");
            if (!next.isEmpty()) {
                name = next;
                continue;
            }
            String type = proxy.optString("type").toLowerCase(Locale.ROOT);
            if (proxy.has("all") || type.equals("selector") || type.equals("urltest")
                    || type.equals("fallback") || type.equals("loadbalance")
                    || type.equals("relay")) return null;
            return name;
        }
        return null;
    }

    private static Integer lastDelay(JSONObject proxy) {
        JSONArray history = proxy == null ? null : proxy.optJSONArray("history");
        if (history == null || history.length() == 0) return null;
        JSONObject last = history.optJSONObject(history.length() - 1);
        long delay = last == null ? -1 : last.optLong("delay", -1);
        return delay > 0 && delay <= Integer.MAX_VALUE ? (int) delay : null;
    }

    private static long nonnegative(JSONObject object, String key) {
        long value = object.optLong(key, -1);
        return value < 0 ? -1 : value;
    }

    public static String formatRate(long bytesPerSecond) {
        if (bytesPerSecond < 0) return "—";
        if (bytesPerSecond < 1024) return bytesPerSecond + " B/s";
        if (bytesPerSecond < 1024L * 1024)
            return String.format(Locale.ROOT, "%.1f KB/s", bytesPerSecond / 1024.0);
        if (bytesPerSecond < 1024L * 1024 * 1024)
            return String.format(Locale.ROOT, "%.1f MB/s", bytesPerSecond / (1024.0 * 1024));
        return String.format(Locale.ROOT, "%.1f GB/s", bytesPerSecond / (1024.0 * 1024 * 1024));
    }

    @Override public void close() {
        closed = true;
        HttpURLConnection connection = activeConnection;
        if (connection != null) connection.disconnect();
        executor.shutdownNow();
    }
}
