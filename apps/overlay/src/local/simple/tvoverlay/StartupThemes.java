package local.simple.tvoverlay;

/** Presentation only. These choices never bypass network readiness checks. */
final class StartupThemes {
    static final String DEFAULT="trace";
    private static final String[] IDS={"trace","still","off"};
    private static final String[] NAMES={"流光","静态","关闭"};
    static String normalize(String id){for(String allowed:IDS)if(allowed.equals(id))return allowed;return DEFAULT;}
    static int index(String id){String normalized=normalize(id);for(int i=0;i<IDS.length;i++)if(IDS[i].equals(normalized))return i;return 0;}
    static String idAt(int index){return index>=0&&index<IDS.length?IDS[index]:DEFAULT;}
    static String name(String id){return NAMES[index(id)];}
    static String[] names(){return NAMES.clone();}
}
