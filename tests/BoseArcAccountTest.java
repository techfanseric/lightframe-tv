package local.simple.tvoverlay;

/** Run with a plain JDK. Validates which speaker channel follows the picture. */
public final class BoseArcAccountTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // The speaker lists the television's ARC input as "TV" and its own
        // socket as "HDMI_n"; the picture decides which one plays.
        check(BoseArc.target(false,"TV","HDMI_1").equals("TV"));
        check(BoseArc.target(true,"TV","HDMI_1").equals("HDMI_1"));
        check(BoseArc.target(false,"TV","HDMI_2").equals("TV"));

        // A missing account never becomes a request to the speaker.
        check(BoseArc.target(false,null,"HDMI_1").equals(""));
        check(BoseArc.target(true,"TV",null).equals(""));
        // The account of the other mode is not used, so it cannot blank the target.
        check(BoseArc.target(false,"TV","").equals("TV"));
        check(BoseArc.target(true,"TV","").equals(""));

        // The ARC channel is the television's own input by default.
        check(BoseArc.DEFAULT_ACCOUNT.equals("TV"));
        check(BoseArc.DEFAULT_HDMI_ACCOUNT.equals("HDMI_1"));

        System.out.println("BoseArcAccountTest passed");
    }
}
