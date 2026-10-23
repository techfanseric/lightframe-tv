package local.simple.tvoverlay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.media.projection.MediaProjectionManager;

/** Shell-only entry into the user-approved snapshot blur preview. */
public final class PreviewActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state==null){
            MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent consent=android.os.Build.VERSION.SDK_INT>=34
                ?manager.createScreenCaptureIntent(android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay())
                :manager.createScreenCaptureIntent();
            startActivityForResult(consent,42);
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==42){
            if(result==RESULT_OK&&data!=null)startForegroundService(new Intent(this,OverlayRuntimeService.class)
                .setAction("CAPTURE").putExtra("result",result).putExtra("data",data));
            finish();
        }
    }
}
