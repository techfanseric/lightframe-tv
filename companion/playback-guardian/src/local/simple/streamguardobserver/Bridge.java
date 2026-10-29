package local.simple.streamguardobserver;
import android.content.*;
import android.net.*;
import android.provider.Settings;
final class Bridge {
 static final String NETFLIX="com.netflix.ninja",YOUTUBE="io.gh.reisxd.tizentube.cobalt",CLASH="com.follow.clash";
 static boolean launcher(CharSequence p){return LauncherIdentity.launcher(p);}
 static boolean overlay(CharSequence p){return LauncherIdentity.overlay(p);}
 static boolean googleDisabled(Context c){try{return !c.getPackageManager().getApplicationInfo("com.google.android.gms",0).enabled;}catch(Exception e){return false;}}
 static Network vpn(Context c){try{ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities cap=n==null?null:cm.getNetworkCapabilities(n);return cap!=null&&cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN)&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)?n:null;}catch(RuntimeException e){return null;}}
 static boolean correctVpnPolicy(Context c){return CLASH.equals(Settings.Secure.getString(c.getContentResolver(),"always_on_vpn_app"));}
 static boolean startProxy(Context c){try{if(!correctVpnPolicy(c))return false;c.startActivity(new Intent("com.follow.clash.action.START").setComponent(new ComponentName(CLASH,"com.follow.clash.QuickActionActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}catch(RuntimeException e){return false;}}
 static boolean originalHome(Context c,String overlay){String home=LauncherIdentity.homeForOverlay(overlay);if(home==null)return false;try{c.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(new ComponentName(home,home+".MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));return true;}catch(RuntimeException e){return false;}}
}
