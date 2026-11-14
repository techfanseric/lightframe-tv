package local.simple.tvoverlay;

/** Run with a plain JDK. Validates the colour of a dock entry. */
public final class DockColorsTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // The source tile carries a light grey of its own. The value is a
        // negative number, which must not be read as "no colour given".
        int source=DockColors.SOURCE_TILE;
        check(source<0);
        // The tile is a clear grey, not a pale one that would swallow the
        // white mark.
        int red=(source>>16)&0xff,green=(source>>8)&0xff,blue=source&0xff;
        check(red<0xd0&&green<0xd0&&blue<0xd0);
        check(red>0xa0&&green>0xa0&&blue>0xa0);
        check(DockColors.tileColor(true,source,"Apple TV · HDMI 2")==source);

        // The other entries keep the colour of their kind.
        check(DockColors.tileColor(false,0,"YouTube")==0xfff5f4f1);
        check(DockColors.tileColor(false,0,"Netflix")==0xff17171b);
        check(DockColors.tileColor(false,0,"哔哩哔哩")==0xffed5188);
        check(DockColors.tileColor(false,0,"IPTV")==0xff1b6773);
        // An entry with a colour of its own keeps it even when the value is
        // zero or negative.
        check(DockColors.tileColor(true,0,"IPTV")==0);
        check(DockColors.tileColor(true,0xff000000,"IPTV")==0xff000000);

        System.out.println("DockColorsTest passed");
    }
}
