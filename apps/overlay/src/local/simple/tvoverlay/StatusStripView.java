package local.simple.tvoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.Locale;

/** Transparent, non-interactive status line. Redraws only on real controller samples. */
final class StatusStripView extends View {
    private static final int HISTORY = 40;
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final long[] upload = new long[HISTORY], download = new long[HISTORY];
    private final float density;
    private int count;
    private boolean available;
    private String node = "读取中", delay = "", downRate = "—", upRate = "—";
    private final Drawable networkIcon, downIcon, upIcon;

    StatusStripView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        networkIcon = context.getDrawable(R.drawable.lucide_network);
        downIcon = context.getDrawable(R.drawable.lucide_arrow_down);
        upIcon = context.getDrawable(R.drawable.lucide_arrow_up);
        text.setColor(Color.WHITE);
        text.setTextSize(9 * getResources().getDisplayMetrics().scaledDensity);
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        line.setColor(Color.WHITE);
        line.setStrokeWidth(dp(0.7f));
        line.setStrokeCap(Paint.Cap.ROUND);
        setFocusable(false); setClickable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // The window and this view have no background, outline, scrim or shadow.
    }

    void update(DashboardClient.Snapshot snapshot) {
        available = snapshot.available;
        node = available ? conciseNode(snapshot.node) : "暂不可用";
        delay = available && snapshot.delayMs != null ? " · " + snapshot.delayMs + "ms" : "";
        downRate = available ? rate(snapshot.down) : "—";
        upRate = available ? rate(snapshot.up) : "—";
        if (count == HISTORY) {
            System.arraycopy(upload, 1, upload, 0, HISTORY - 1);
            System.arraycopy(download, 1, download, 0, HISTORY - 1);
            count--;
        }
        upload[count] = available ? snapshot.up : -1;
        download[count] = available ? snapshot.down : -1;
        count++;
        invalidate();
    }

    private String conciseNode(String name) {
        if (name == null || name.trim().isEmpty()) return "已连接";
        int separator = name.indexOf('|');
        if (separator >= 0) name = name.substring(0, separator);
        StringBuilder clean = new StringBuilder();
        boolean started = false;
        for (int offset = 0; offset < name.length();) {
            int cp = name.codePointAt(offset); offset += Character.charCount(cp);
            if (!started && Character.isLetter(cp)) started = true;
            if (started && cp <= 0xffff && (Character.isLetterOrDigit(cp) || cp == '-' || cp == '_' || cp == ' ' || cp == '.')) clean.appendCodePoint(cp);
        }
        return clean.length() == 0 ? "已连接" : clean.toString().trim();
    }

    private String rate(long value) {
        if (value < 0) return "—";
        if (value < 1024) return value + "B/s";
        if (value < 1024 * 1024) return String.format(Locale.ROOT, "%.0fKB/s", value / 1024.0);
        return String.format(Locale.ROOT, "%.1fMB/s", value / (1024.0 * 1024));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gap = dp(9), graphWidth = dp(62), iconWidth = dp(13), arrow = dp(9), smallGap = dp(3), metricGap = dp(8);
        float baseline = (getHeight() - text.ascent() - text.descent()) / 2;
        String prefix = "Clash · ";
        float rateWidth = arrow * 2 + smallGap * 2 + metricGap + text.measureText(downRate) + text.measureText(upRate);
        float fixed = text.measureText(prefix + delay) + rateWidth + graphWidth + gap * 2 + iconWidth;
        String visibleNode = ellipsize(node, Math.max(dp(30), getWidth() - fixed - dp(3)));
        String label = prefix + visibleNode + delay;
        float contentWidth = iconWidth + text.measureText(label) + rateWidth + graphWidth + gap * 2;
        float x = Math.max(dp(1), getWidth() - contentWidth - dp(1));

        double scale = 1;
        for (int i = 0; i < count; i++) scale = Math.max(scale, Math.log1p(Math.max(upload[i], download[i]) / 1024.0));
        float top = dp(3), bottom = getHeight() - dp(3);
        drawSeries(canvas, download, x, graphWidth, top, bottom, scale, 225);
        drawSeries(canvas, upload, x, graphWidth, top, bottom, scale, 100);
        x += graphWidth + gap;

        networkIcon.setAlpha(available ? 255 : 120);
        drawIcon(canvas, networkIcon, x, dp(10));
        x += iconWidth;
        text.setAlpha(255); canvas.drawText(label, x, baseline, text);
        x += text.measureText(label) + gap;
        drawIcon(canvas, downIcon, x, arrow); x += arrow + smallGap;
        canvas.drawText(downRate, x, baseline, text); x += text.measureText(downRate) + metricGap;
        drawIcon(canvas, upIcon, x, arrow); x += arrow + smallGap;
        canvas.drawText(upRate, x, baseline, text);
    }

    private void drawIcon(Canvas canvas, Drawable drawable, float left, float size) {
        int x = Math.round(left), y = Math.round((getHeight() - size) / 2);
        drawable.setBounds(x, y, Math.round(left + size), Math.round(y + size));
        drawable.draw(canvas);
    }

    private void drawSeries(Canvas canvas, long[] values, float left, float width, float top, float bottom, double scale, int alpha) {
        path.reset(); boolean connected = false;
        for (int i = 0; i < count; i++) {
            if (values[i] < 0) { connected = false; continue; }
            float x = left + width * i / Math.max(1, count - 1);
            float y = bottom - (float) (Math.log1p(values[i] / 1024.0) / scale) * (bottom - top);
            if (connected) path.lineTo(x, y); else path.moveTo(x, y);
            connected = true;
        }
        line.setStyle(Paint.Style.STROKE); line.setAlpha(alpha);
        canvas.drawPath(path, line);
    }

    private String ellipsize(String value, float width) {
        if (text.measureText(value) <= width) return value;
        int end = value.length();
        while (end > 0 && text.measureText(value.substring(0, end) + "…") > width) end--;
        return value.substring(0, end) + "…";
    }

    private float dp(float value) { return density * value; }
}
