package local.simple.tvoverlay;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** Decode large source artwork at icon resolution; keep original transparent files intact. */
final class EntryIcons {
    private static Bitmap sample(Context c,int resource){
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeResource(c.getResources(),resource,o);o.inSampleSize=1;
        while(Math.max(o.outWidth,o.outHeight)/(o.inSampleSize*2)>=160)o.inSampleSize*=2;
        o.inJustDecodeBounds=false;o.inScaled=false;return BitmapFactory.decodeResource(c.getResources(),resource,o);
    }
    static Drawable iptv(Context c){return c.getDrawable(R.drawable.iptv_symbol).mutate();}
    static Drawable settings(Context c){Drawable d=new BitmapDrawable(c.getResources(),sample(c,R.drawable.settings_entry_generated));d.setTint(0xff29384e);return d;}
    static Drawable home(Context c){return c.getDrawable(R.drawable.smart_home_symbol).mutate();}
    static Drawable bilibili(Context c){Drawable d=c.getDrawable(R.drawable.bilibili_symbol_official).mutate();d.setTint(0xfffffafb);return d;}
    static Drawable netflix(Context c){
        Bitmap bitmap=sample(c,R.drawable.netflix_symbol_official);
        // Official PNG's transparent canvas is 991 x 1801, ink bounds (247,451)-(743,1351).
        // Use that source region for layout while preserving all logo pixels and colors.
        Rect ink=new Rect(Math.round(bitmap.getWidth()*247f/991),Math.round(bitmap.getHeight()*451f/1801),Math.round(bitmap.getWidth()*743f/991),Math.round(bitmap.getHeight()*1351f/1801));
        return new Drawable(){final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);public void draw(Canvas canvas){canvas.drawBitmap(bitmap,ink,getBounds(),paint);}public int getIntrinsicWidth(){return ink.width();}public int getIntrinsicHeight(){return ink.height();}public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};
    }
}
