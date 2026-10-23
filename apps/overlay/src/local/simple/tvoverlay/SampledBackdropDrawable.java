package local.simple.tvoverlay;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Screen-aligned frozen blur with a broad feather, rather than a rectangular pane. */
final class SampledBackdropDrawable extends Drawable {
    private final Bitmap screen;
    private final View view;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private Bitmap texture;
    private int sampledX=Integer.MIN_VALUE,sampledY=Integer.MIN_VALUE;
    @Override protected void onBoundsChange(Rect bounds){texture=null;}

    SampledBackdropDrawable(Bitmap screen,View view){this.screen=screen;this.view=view;}
    @Override public void draw(Canvas canvas){
        int[] location=new int[2];view.getLocationOnScreen(location);
        if(texture==null||sampledX!=location[0]||sampledY!=location[1]){
            sampledX=location[0];sampledY=location[1];
            int w=Math.max(1,getBounds().width()/8),h=Math.max(1,getBounds().height()/8);
            texture=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            android.util.DisplayMetrics dm=view.getResources().getDisplayMetrics();
            Rect src=new Rect(location[0]*screen.getWidth()/dm.widthPixels,location[1]*screen.getHeight()/dm.heightPixels,
                (location[0]+getBounds().width())*screen.getWidth()/dm.widthPixels,
                (location[1]+getBounds().height())*screen.getHeight()/dm.heightPixels);
            new Canvas(texture).drawBitmap(screen,src,new Rect(0,0,w,h),paint);
            int[] pixels=new int[w*h];texture.getPixels(pixels,0,w,0,0,w,h);

            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                float nx=Math.abs(2f*x/Math.max(1,w-1)-1);
                float ny=Math.abs(2f*y/Math.max(1,h-1)-1);
                // Continuous bell-shaped falloff: no rectangular plateau or corner seams.
                double edge=Math.exp(-4);
                double fx=(Math.exp(-4*Math.pow(nx,6))-edge)/(1-edge);
                double fy=(Math.exp(-4*Math.pow(ny,4))-edge)/(1-edge);
                int a=(int)Math.round(250*fx*fy);int c=pixels[y*w+x];
                int r=Math.round(((c>>16)&255)*0.51f),g=Math.round(((c>>8)&255)*0.51f),b=Math.round((c&255)*0.51f);
                pixels[y*w+x]=(a<<24)|(r<<16)|(g<<8)|b;
            }
            texture.setPixels(pixels,0,w,0,0,w,h);
        }
        canvas.drawBitmap(texture,null,getBounds(),paint);
    }
    @Override public void setAlpha(int a){paint.setAlpha(a);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
