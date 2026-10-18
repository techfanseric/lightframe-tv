package local.simple.tvoverlay;

/** Resolves a fixed app or the last successfully used video app. No launching or settings writes. */
final class StartupChoice {
    static int resolve(String policy, String lastApp, int recent, String[] ids) {
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(policy)) return i;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(lastApp)) return i;
        return recent >= 0 && recent < ids.length ? recent : 0;
    }
}
