package local.simple.tvoverlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** A single feathered scrim behind both rows. This does not blur other windows. */
final class MenuBackdropDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final Bitmap TEXTURE = createTexture();

    private static Bitmap createTexture() {
        // Small, immutable texture: no screen capture, runtime filters, or frame loop.
        final int width = 322, height = 98;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            float vertical = feather(Math.min(y, height - 1 - y) / 13f);
            for (int x = 0; x < width; x++) {
                float horizontal = feather(Math.min(x, width - 1 - x) / 16f);
                // Broad reflected light stays feathered; a darker center supports labels.
                float nx = x / (float) (width - 1);
                float ny = y / (float) (height - 1);
                float sheen = (float) Math.exp(-Math.pow((ny - 0.19f - nx * 0.10f) / 0.13f, 2));
                float lowerGlow = (float) Math.exp(-Math.pow((ny - 0.82f) / 0.18f, 2));
                int alpha = Math.round((128 + 18 * ny) * horizontal * vertical);
                int red = Math.round(20 + 20 * sheen + 4 * lowerGlow);
                int green = Math.round(25 + 22 * sheen + 6 * lowerGlow);
                int blue = Math.round(31 + 25 * sheen + 8 * lowerGlow);
                pixels[y * width + x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private static float feather(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawBitmap(TEXTURE, null, getBounds(), paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
