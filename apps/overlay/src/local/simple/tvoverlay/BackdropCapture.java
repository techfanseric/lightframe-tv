package local.simple.tvoverlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import java.nio.ByteBuffer;

/** Opt-in low-resolution sampling. No frames are saved to disk. */
final class BackdropCapture {
    private static MediaProjection projection;
    private static ImageReader reader;
    private static VirtualDisplay display;
    static synchronized void start(Context context, MediaProjection value) {
        stop(); projection=value;
        android.util.DisplayMetrics dm=context.getResources().getDisplayMetrics();
        int w=Math.max(1,dm.widthPixels/8),h=Math.max(1,dm.heightPixels/8);
        reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);
        value.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){synchronized(BackdropCapture.class){if(projection==value)stop();}}},new Handler(Looper.getMainLooper()));
        display=value.createVirtualDisplay("Lightframe backdrop",w,h,dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,null);
    }
    static synchronized void stop() {
        if(display!=null){display.release();display=null;}
        if(reader!=null){reader.close();reader=null;}
        MediaProjection old=projection;projection=null;
        if(old!=null)old.stop();
    }
    static synchronized Bitmap sample() {
        if(reader==null)return null;
        display.setSurface(reader.getSurface());
        android.os.SystemClock.sleep(180);
        try(Image image=reader.acquireLatestImage()) {
            if(image==null)return null;
            int w=image.getWidth(),h=image.getHeight();
            Image.Plane plane=image.getPlanes()[0];ByteBuffer bytes=plane.getBuffer();
            int[] pixels=new int[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                int i=y*plane.getRowStride()+x*plane.getPixelStride();
                pixels[y*w+x]=0xff000000|((bytes.get(i)&255)<<16)|((bytes.get(i+1)&255)<<8)|(bytes.get(i+2)&255);
            }
            // Two small box passes soften detail while retaining background shapes.
            int[] tmp=new int[pixels.length];
            for(int pass=0;pass<2;pass++){blur(pixels,tmp,w,h,true);blur(tmp,pixels,w,h,false);}
            return Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
        }catch(RuntimeException e){android.util.Log.w("HomeOverlay","Backdrop capture unavailable",e);return null;}finally{if(display!=null)display.setSurface(null);}
    }
    private static void blur(int[] src,int[] dst,int w,int h,boolean horizontal){
        int radius=1;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int r=0,g=0,b=0;
            for(int k=-radius;k<=radius;k++){
                int xx=horizontal?Math.max(0,Math.min(w-1,x+k)):x;
                int yy=horizontal?y:Math.max(0,Math.min(h-1,y+k));
                int c=src[yy*w+xx];r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;
            }
            dst[y*w+x]=0xff000000|((r/3)<<16)|((g/3)<<8)|(b/3);
        }
    }
}
