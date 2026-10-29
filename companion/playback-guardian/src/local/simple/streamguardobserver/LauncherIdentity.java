package local.simple.streamguardobserver;

/** Exact identities only: route cleanup to the same launcher that owns the blocking card. */
final class LauncherIdentity {
 static final String HOME="local.simple.tvhome",OVERLAY="local.simple.tvoverlay";
 static final String DOCK_HOME="local.simple.tvhome.dock",DOCK_OVERLAY="local.simple.tvoverlay.dock";
 static boolean overlay(CharSequence p){return OVERLAY.contentEquals(p==null?"":p)||DOCK_OVERLAY.contentEquals(p==null?"":p);}
 static boolean launcher(CharSequence p){return overlay(p)||HOME.contentEquals(p==null?"":p)||DOCK_HOME.contentEquals(p==null?"":p);}
 static String homeForOverlay(String p){return OVERLAY.equals(p)?HOME:DOCK_OVERLAY.equals(p)?DOCK_HOME:null;}
 static boolean samePair(String overlay,CharSequence p){String home=homeForOverlay(overlay);return home!=null&&(overlay.contentEquals(p==null?"":p)||home.contentEquals(p==null?"":p));}
 // These known apps may sit underneath the launcher's non-focusable error card.
 // This permits taking an initial baseline only; a request still retains that
 // exact baseline and its own launcher pair throughout cleanup.
 static boolean allowedBaseline(String p){return launcher(p)||"com.netflix.ninja".equals(p)||"io.gh.reisxd.tizentube.cobalt".equals(p)||"com.xiaodianshi.tv.yst".equals(p)||"top.yogiczy.mytv.tv".equals(p)||"com.follow.clash".equals(p);}
}
