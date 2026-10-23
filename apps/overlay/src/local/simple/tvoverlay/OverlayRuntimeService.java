package local.simple.tvoverlay;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.os.ResultReceiver;

import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** HDMI hotplug handling. No key interception or window reads. */
public final class OverlayRuntimeService extends Service {
    private static OverlayRuntimeService instance;
    private LauncherOverlay overlay;
    static OverlayRuntimeService current(){return instance;}
    void requestBoot(){if(overlay!=null)overlay.boot();}
    void requestHome(boolean settings){if(overlay!=null)overlay.home(settings);}
    private static final String STOCK = "com.mitv.tvhome";
    private static final Pattern HDMI_PORT = Pattern.compile("HDMI\\s*[-:]?\\s*([1-3])(?![0-9])",
            Pattern.CASE_INSENSITIVE);
    private TvInputManager inputManager;
    private final Map<String, Integer> inputStates = new HashMap<>();
    private boolean callbackRegistered;
    private final TvInputManager.TvInputCallback inputCallback = new TvInputManager.TvInputCallback() {
        @Override public void onInputAdded(String inputId) {
            try {
                if (inputManager != null) {
                    int state = inputManager.getInputState(inputId);
                    // Boot and CEC discovery also add already connected inputs.
                    // Establish a baseline only; registration is not a cable insertion.
                    inputStates.put(inputId, state);
                }
            } catch (RuntimeException ignored) { }
        }
        @Override public void onInputRemoved(String inputId) { inputStates.remove(inputId); }
        @Override public void onInputStateChanged(String inputId, int state) {
            Integer previous = inputStates.put(inputId, state);
            // A baseline must exist; discovering an already connected input is not hotplug.
            if (previous == null || previous == TvInputManager.INPUT_STATE_CONNECTED
                    || state != TvInputManager.INPUT_STATE_CONNECTED) return;
            try {
                TvInputInfo info = inputManager == null ? null : inputManager.getTvInputInfo(inputId);
                if (info == null || info.getType() != TvInputInfo.TYPE_HDMI) return;
                int port = physicalHdmiPort(info);
                if (port > 0) LauncherSupport.switchHdmi(OverlayRuntimeService.this, port);
                else LauncherSupport.openSourcePopup(OverlayRuntimeService.this);
            } catch (RuntimeException ignored) { }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new NotificationChannel("overlay_runtime","电视贴片",NotificationManager.IMPORTANCE_MIN));
        Intent home=new Intent().setComponent(new android.content.ComponentName("local.simple.tvhome","local.simple.tvhome.MainActivity"));
        PendingIntent open=PendingIntent.getActivity(this,1,home,PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(1,new Notification.Builder(this,"overlay_runtime").setSmallIcon(R.drawable.home_icon)
            .setContentTitle("轻幕贴片").setContentText("首页键切换应用").setContentIntent(open).setOngoing(true).setShowWhen(false).build());
        instance=this;
        if(overlay!=null)overlay.close();
        overlay=new LauncherOverlay(this);
        registerInputCallback();
        android.util.Log.i("HomeOverlay","runtime created overlayAllowed="+android.provider.Settings.canDrawOverlays(this));
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null){
            try{
                String secret=intent.getStringExtra("core_secret");if(secret!=null&&!secret.isEmpty()&&secret.length()<257)LauncherSupport.preferences(this).edit().putString("core_secret",secret).apply();
                if(!android.provider.Settings.canDrawOverlays(this)){ResultReceiver missing=intent.getParcelableExtra("reply");if(missing!=null)missing.send(0,null);return START_STICKY;}
                ResultReceiver reply=intent.getParcelableExtra("reply");if(reply!=null)reply.send(1,null);
                if("CAPTURE".equals(intent.getAction())){
                    android.media.projection.MediaProjectionManager manager=(android.media.projection.MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
                    BackdropCapture.start(this,manager.getMediaProjection(intent.getIntExtra("result",0),intent.getParcelableExtra("data")));
                    new Handler(Looper.getMainLooper()).postDelayed(()->requestHome(true),900);
                }
                else if("HOME".equals(intent.getAction()))requestHome(intent.getBooleanExtra("settings",false));
                else if("BOOT".equals(intent.getAction()))requestBoot();
                android.util.Log.i("HomeOverlay","runtime handled "+intent.getAction());
            }catch(RuntimeException error){
                android.util.Log.e("HomeOverlay","runtime command failed",error);
                ResultReceiver reply=intent.getParcelableExtra("reply");if(reply!=null)reply.send(0,null);
            }
        }
        if(intent==null)requestBoot();
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intent){return null;}

    private void registerInputCallback() {
        if (callbackRegistered) return;
        try {
            inputManager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
            if (inputManager == null) return;
            inputStates.clear();
            for (TvInputInfo info : inputManager.getTvInputList()) {
                inputStates.put(info.getId(), inputManager.getInputState(info.getId()));
            }
            inputManager.registerCallback(inputCallback, new Handler(Looper.getMainLooper()));
            callbackRegistered = true;
            android.util.Log.i("SimpleTVHome", "HDMI input callback registered");
        } catch (RuntimeException ignored) {
            // Some vendor firmware restricts TV input access; home bridging still works.
            inputStates.clear();
        }
    }

    private int physicalHdmiPort(TvInputInfo info) {
        // CEC children may be labelled "Apple TV". Resolve their parent label first;
        // hardware IDs and encoded virtual input IDs are deliberately never guessed.
        String parentId = info.getParentId();
        if (parentId != null && inputManager != null) {
            TvInputInfo parent = inputManager.getTvInputInfo(parentId);
            if (parent != null) {
                int parentPort = portFromLabel(parent.loadLabel(this));
                if (parentPort > 0) return parentPort;
            }
        }
        return portFromLabel(info.loadLabel(this));
    }

    private int portFromLabel(CharSequence label) {
        if (label == null) return -1;
        Matcher match = HDMI_PORT.matcher(label.toString());
        return match.find() ? Integer.parseInt(match.group(1)) : -1;
    }

    @Override public void onDestroy() {
        BackdropCapture.stop();
        instance=null;if(overlay!=null){overlay.close();overlay=null;}

        if (callbackRegistered && inputManager != null) {
            try { inputManager.unregisterCallback(inputCallback); }
            catch (RuntimeException ignored) { }
        }
        callbackRegistered = false;
        inputStates.clear();
        super.onDestroy();
    }
}
