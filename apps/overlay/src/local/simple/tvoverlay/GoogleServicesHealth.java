package local.simple.tvoverlay;

import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import java.util.concurrent.*;

/** Bounded preflight; never enables or installs Google packages implicitly. */
final class GoogleServicesHealth {
    static String issue(Context c) {
        PackageManager pm=c.getPackageManager();
        try {
            PackageInfo gms=pm.getPackageInfo("com.google.android.gms",0);
            PackageInfo gsf=pm.getPackageInfo("com.google.android.gsf",0);
            if(!gms.applicationInfo.enabled)return "Google Play 服务已停用";
            if(!gsf.applicationInfo.enabled)return "Google 服务框架已停用";
            // These exact installed builds were observed crashing on this firmware.
            if(gsf.getLongVersionCode()==28 && pm.checkPermission("android.permission.MANAGE_USERS",gsf.packageName)!=PackageManager.PERMISSION_GRANTED)
                return "Google 服务框架与电视系统权限不兼容";
            if(gms.getLongVersionCode()==263933000 && pm.checkPermission("android.permission.LOCATION_HARDWARE",gms.packageName)!=PackageManager.PERMISSION_GRANTED)
                return "Google Play 服务与电视系统权限不兼容";
            return null;
        } catch(PackageManager.NameNotFoundException e){return "Google Play 服务或服务框架未安装";}
    }
    /** Called only from a background worker. Does not read or retain Google account/ID values. */
    static String probe(Context c) {
        String problem=issue(c);if(problem!=null)return problem;
        try(Cursor cursor=c.getContentResolver().query(Uri.parse("content://com.google.android.gsf.gservices"),null,null,new String[]{"android_id"},null)){
            if(cursor==null)return "Google 服务框架尚未就绪";
        }catch(RuntimeException e){return "Google 服务框架无法正常响应";}
        CountDownLatch connected=new CountDownLatch(1);
        final IBinder[] binder={null};final java.util.concurrent.atomic.AtomicBoolean disconnected=new java.util.concurrent.atomic.AtomicBoolean();
        ServiceConnection link=new ServiceConnection(){
            public void onServiceConnected(ComponentName n,IBinder b){binder[0]=b;connected.countDown();}
            public void onServiceDisconnected(ComponentName n){disconnected.set(true);}
            public void onNullBinding(ComponentName n){connected.countDown();}
            public void onBindingDied(ComponentName n){disconnected.set(true);connected.countDown();}
        };
        boolean bound=false;
        try {
            bound=c.bindService(new Intent("com.google.android.gms.common.service.START").setPackage("com.google.android.gms"),link,Context.BIND_AUTO_CREATE);
            if(!bound||!connected.await(5,TimeUnit.SECONDS)||binder[0]==null)return "Google Play 服务尚未就绪";
            // A connected broker alone can still belong to a repeatedly crashing process.
            for(int i=0;i<6;i++){
                if(disconnected.get()||!binder[0].pingBinder())return "Google Play 服务运行异常";
                Thread.sleep(500);
            }
            return issue(c);
        } catch(InterruptedException e){Thread.currentThread().interrupt();return "Google 服务检查已取消";}
        catch(RuntimeException e){return "Google Play 服务无法正常响应";}
        finally{if(bound)try{c.unbindService(link);}catch(RuntimeException ignored){}}
    }
}
