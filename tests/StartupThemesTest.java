package local.simple.tvoverlay;
public final class StartupThemesTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        String[] expected={"trace","still","off"};check(StartupThemes.names().length==3);
        for(int i=0;i<expected.length;i++){check(StartupThemes.idAt(i).equals(expected[i]));check(StartupThemes.index(expected[i])==i);}
        // Old artwork choices migrate to the new default; explicit off remains off.
        for(String old:new String[]{null,"psyduck","snoopy","unknown","../other"})check(StartupThemes.normalize(old).equals("trace"));
        check(StartupThemes.normalize("off").equals("off"));check(StartupThemes.idAt(-1).equals("trace"));
        String[] labels=StartupThemes.names();labels[0]="mutated";check(!StartupThemes.name("trace").equals("mutated"));
        System.out.println("StartupThemesTest passed");
    }
}
