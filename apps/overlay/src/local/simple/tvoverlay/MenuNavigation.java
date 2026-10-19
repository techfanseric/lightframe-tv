package local.simple.tvoverlay;

/** Rows are ordered top to bottom; the caller explicitly chooses the application row initially. */
final class MenuNavigation {
    static int wrap(int index,int delta,int count){if(count<1)throw new IllegalArgumentException("Empty row");return Math.floorMod(index+delta,count);}
    static int adjacentColumn(int column,int fromCount,int toCount){
        if(fromCount<1||toCount<1)throw new IllegalArgumentException("Empty row");
        return Math.min(toCount-1,(int)(((long)column*2+1)*toCount/(fromCount*2L)));
    }
    static int quickRows(int controls,int maxRows){return Math.min(Math.max(0,maxRows),(Math.max(0,controls)+3)/4);}
    static int quickCapacity(int heightDp){return 4*Math.min(3,Math.max(0,(heightDp-220)/64));}
}
