package local.simple.tvoverlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Cached soft light, not a blur of the content underneath. */
final class FocusHaloDrawable extends Drawable {
    private static final Bitmap TEXTURE = createTexture();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final int spread;

    FocusHaloDrawable(int spread) { this.spread = spread; }

    private static Bitmap createTexture() {
        int width = 192, height = 96;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            float ny = Math.abs(2f * y / (height - 1) - 1);
            for (int x = 0; x < width; x++) {
                float nx = Math.abs(2f * x / (width - 1) - 1);
                // A broad center rolling gently into a fully transparent perimeter.
                float distance = (float) Math.pow(Math.pow(nx, 4) + Math.pow(ny, 4), 0.25);
                float t = Math.max(0, Math.min(1, (1 - distance) / 0.55f));
                float fade = t * t * (3 - 2 * t);
                int alpha = Math.round(48 * fade * (1 - 0.18f * y / (height - 1)));
                pixels[y * width + x] = (alpha << 24) | 0x00b6bec4;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    @Override public void draw(Canvas canvas) {
        destination.set(getBounds());
        destination.inset(-spread, -spread);
        canvas.drawBitmap(TEXTURE, null, destination, paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
