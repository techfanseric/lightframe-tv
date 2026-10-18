package local.simple.tvoverlay;

import android.content.Context;
import android.content.Intent;

final class AppCatalog {
    static final String[] IDS={"youtube","bilibili","netflix"};
    static final String[] NAMES={"YouTube","哔哩哔哩","Netflix"};
    static final String[][] PACKAGES={{"com.amazon.firetv.youtube"},{"com.xiaodianshi.tv.yst"},{"com.netflix.ninja","com.netflix.mediaclient"}};
    static int index(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return i;return 0;}
    static int forPackage(String pkg){for(int i=0;i<PACKAGES.length;i++)for(String p:PACKAGES[i])if(p.equals(pkg))return i;return -1;}
    static Intent intent(Context c,int i){for(String p:PACKAGES[i]){
        Intent in=c.getPackageManager().getLeanbackLaunchIntentForPackage(p);
        if(in==null)in=c.getPackageManager().getLaunchIntentForPackage(p);
        if(in!=null)return in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }return null;}
    static boolean launch(Context c,int i){try{if(i==2){String issue=GoogleServicesHealth.issue(c);if(issue!=null){android.util.Log.w("HomeNetwork","blocked netflix: "+issue);android.widget.Toast.makeText(c,issue+"，暂未打开 Netflix",android.widget.Toast.LENGTH_LONG).show();return false;}}Intent in=intent(c,i);if(in==null)return false;c.startActivity(in);return true;}catch(RuntimeException e){return false;}}
    static String packageName(Context c,int i){Intent in=intent(c,i);return in!=null&&in.getComponent()!=null?in.getComponent().getPackageName():PACKAGES[i][0];}
}
