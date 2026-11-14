package local.simple.tvoverlay;

/** Run with a plain JDK. Validates the boot target and its cycle. */
public final class StartupChoiceSourceTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        String[] ids=AppCatalog.IDS;

        // A source is a boot target of its own.
        check(StartupChoice.resolve(StartupChoice.SOURCE_ID,"",0,ids)==StartupChoice.SOURCE);
        check(StartupChoice.resolve("last",StartupChoice.SOURCE_ID,0,ids)==StartupChoice.SOURCE);
        check(StartupChoice.isSource(StartupChoice.SOURCE_ID));
        check(!StartupChoice.isSource("youtube"));
        check(!StartupChoice.isSource("last"));

        // An application keeps its own behaviour.
        check(StartupChoice.resolve("netflix","",0,ids)==2);
        check(StartupChoice.resolve("last","bilibili",0,ids)==1);
        check(StartupChoice.resolve("last","",3,ids)==3);
        check(StartupChoice.resolve("unknown","",7,ids)==0);

        // The cycle walks the applications and ends on the source.
        check(StartupChoice.next("last",ids).equals(ids[0]));
        check(StartupChoice.next(ids[0],ids).equals(ids[1]));
        check(StartupChoice.next(ids[ids.length-2],ids).equals(ids[ids.length-1]));
        check(StartupChoice.next(ids[ids.length-1],ids).equals(StartupChoice.SOURCE_ID));
        // The source is followed by the television being left alone, and
        // then the cycle starts over.
        check(StartupChoice.next(StartupChoice.SOURCE_ID,ids).equals(StartupChoice.OFF_ID));
        check(StartupChoice.next(StartupChoice.OFF_ID,ids).equals(StartupChoice.LAST_ID));
        // An unknown value returns to the start of the cycle.
        check(StartupChoice.next("nonsense",ids).equals("last"));

        // The takeover switch and the boot target share one tile.
        check(StartupChoice.isOff(StartupChoice.OFF_ID));
        check(!StartupChoice.isOff(StartupChoice.LAST_ID));
        check(!StartupChoice.isOff(StartupChoice.SOURCE_ID));
        check(StartupChoice.effective(StartupChoice.LAST_ID,false).equals(StartupChoice.OFF_ID));
        check(StartupChoice.effective(StartupChoice.LAST_ID,true).equals(StartupChoice.LAST_ID));
        check(StartupChoice.effective(StartupChoice.OFF_ID,true).equals(StartupChoice.OFF_ID));
        // A target that says "off" opens nothing.
        check(StartupChoice.resolve(StartupChoice.OFF_ID,"",0,ids)==-1);
        check(StartupChoice.resolve(StartupChoice.OFF_ID,"bilibili",1,ids)==-1);

        // The index of an application, or -1 when it is not one.
        check(StartupChoice.index("youtube",ids)==0);
        check(StartupChoice.index("iptv",ids)==3);
        check(StartupChoice.index(StartupChoice.SOURCE_ID,ids)==-1);
        check(StartupChoice.index("last",ids)==-1);

        // The attached source sits directly behind the video entry.
        check(StartupChoice.sourceSlot(new int[]{0,1,3},0)==1);
        check(StartupChoice.sourceSlot(new int[]{1,3},0)==1);
        check(StartupChoice.sourceSlot(new int[]{1,3},0)==1);
        // With no applications at all the source leads the row.
        check(StartupChoice.sourceSlot(new int[0],0)==0);
        // The video entry is not in the row, so the source follows the first
        // entry that is.
        check(StartupChoice.sourceSlot(new int[]{2,1},0)==1);
        check(StartupChoice.sourceSlot(new int[]{1,2},0)==1);

        System.out.println("StartupChoiceSourceTest passed");
    }
}
