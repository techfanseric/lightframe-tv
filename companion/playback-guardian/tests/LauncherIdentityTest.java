package local.simple.streamguardobserver;
public final class LauncherIdentityTest {
 public static void main(String[] args){
  check(LauncherIdentity.HOME.equals(LauncherIdentity.homeForOverlay(LauncherIdentity.OVERLAY)));
  check(LauncherIdentity.DOCK_HOME.equals(LauncherIdentity.homeForOverlay(LauncherIdentity.DOCK_OVERLAY)));
  for(String p:new String[]{null,"", "local.simple.tvoverlay.fake", "local.simple.tvhome.dock.evil", "com.netflix.ninja"}){check(!LauncherIdentity.launcher(p));check(LauncherIdentity.homeForOverlay(p)==null);}
  check(LauncherIdentity.launcher(LauncherIdentity.DOCK_HOME));check(!LauncherIdentity.overlay(LauncherIdentity.DOCK_HOME));
  for(String overlay:new String[]{LauncherIdentity.OVERLAY,LauncherIdentity.DOCK_OVERLAY}){
   check(LauncherIdentity.samePair(overlay,overlay));
   check(LauncherIdentity.samePair(overlay,LauncherIdentity.homeForOverlay(overlay)));
   String other=overlay.equals(LauncherIdentity.OVERLAY)?LauncherIdentity.DOCK_OVERLAY:LauncherIdentity.OVERLAY;
   check(!LauncherIdentity.samePair(overlay,other));
   check(!LauncherIdentity.samePair(overlay,LauncherIdentity.homeForOverlay(other)));
   for(String invalid:new String[]{null,"",overlay+".evil","com.netflix.ninja"})check(!LauncherIdentity.samePair(overlay,invalid));
  }
  check(!LauncherIdentity.samePair(null,LauncherIdentity.HOME));
  check(!LauncherIdentity.samePair(LauncherIdentity.HOME,LauncherIdentity.HOME));
  for(String p:new String[]{LauncherIdentity.HOME,LauncherIdentity.OVERLAY,LauncherIdentity.DOCK_HOME,LauncherIdentity.DOCK_OVERLAY,"com.netflix.ninja","io.gh.reisxd.tizentube.cobalt","com.xiaodianshi.tv.yst","top.yogiczy.mytv.tv","com.follow.clash"})check(LauncherIdentity.allowedBaseline(p));
  for(String p:new String[]{null,"","com.android.settings","com.android.systemui","com.mitv.tvhome","com.follow.clash.fake","com.github.kr328.clash","top.yogiczy.mytv.tv.fake","com.xiaodianshi.tv.yst.fake"})check(!LauncherIdentity.allowedBaseline(p));
  System.out.println("LauncherIdentityTest passed");
 }
 private static void check(boolean v){if(!v)throw new AssertionError();}
}
