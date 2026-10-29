package local.simple.tvoverlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** A single feathered scrim behind both rows. This does not blur other windows. */
final class MenuBackdropDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Bitmap texture;

    MenuBackdropDrawable() {
        // Small, immutable texture: no screen capture, runtime filters, or frame loop.
        final int width = 322, height = 98;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            float vertical = feather(Math.min(y, height - 1 - y) / 13f);
            for (int x = 0; x < width; x++) {
                float horizontal = feather(Math.min(x, width - 1 - x) / 16f);
                // 72% local neutral tint guarantees separation even on bright footage.
                int alpha = Math.round(184 * horizontal * vertical);
                pixels[y * width + x] = (alpha << 24) | 0x0013171d;
            }
        }
        texture = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private static float feather(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawBitmap(texture, null, getBounds(), paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
