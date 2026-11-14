package local.simple.tvoverlay;

/**
 * Run with a plain JDK.
 *
 * The documents are the ones the speaker actually returned, taken from the
 * target speaker during development.
 */
public final class BoseArcStateTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // The speaker is idle. It names no account and is on neither channel.
        String idle="<?xml version=\"1.0\" encoding=\"UTF-8\" ?><nowPlaying deviceID=\"883F4A312708\" source=\"STANDBY\"><ContentItem source=\"STANDBY\" isPresetable=\"false\" /></nowPlaying>";
        check(BoseArcState.account(idle).equals(""));
        check(BoseArcState.account(idle).equals(BoseArcState.IDLE));

        // A product source names its account.
        String tv="<?xml version=\"1.0\" encoding=\"UTF-8\" ?><nowPlaying deviceID=\"883F4A312708\" source=\"PRODUCT\" sourceAccount=\"TV\"><ContentItem source=\"PRODUCT\" sourceAccount=\"TV\" isPresetable=\"false\" /><art artImageStatus=\"SHOW_DEFAULT_IMAGE\" /><playStatus>PLAY_STATE</playStatus></nowPlaying>";
        check(BoseArcState.account(tv).equals("TV"));
        String hdmi="<?xml version=\"1.0\" encoding=\"UTF-8\" ?><nowPlaying deviceID=\"883F4A312708\" source=\"PRODUCT\" sourceAccount=\"HDMI_1\"><ContentItem source=\"PRODUCT\" sourceAccount=\"HDMI_1\" isPresetable=\"false\" /><art artImageStatus=\"SHOW_DEFAULT_IMAGE\" /><playStatus>PLAY_STATE</playStatus></nowPlaying>";
        check(BoseArcState.account(hdmi).equals("HDMI_1"));

        // Nothing at all is treated as no channel.
        check(BoseArcState.account(null).equals(""));
        check(BoseArcState.account("").equals(""));
        check(BoseArcState.account("not xml").equals(""));

        // The speaker is satisfied only when it plays the wanted account. An
        // idle speaker is not, so it is asked to take a channel.
        check(BoseArcState.satisfied("TV","TV"));
        check(BoseArcState.satisfied("HDMI_1","HDMI_1"));
        check(!BoseArcState.satisfied("TV","HDMI_1"));
        check(!BoseArcState.satisfied("HDMI_1","TV"));
        check(!BoseArcState.satisfied("","TV"));
        check(!BoseArcState.satisfied("","HDMI_1"));
        // A wanted channel that is not configured asks for nothing.
        check(BoseArcState.satisfied("TV",""));
        check(BoseArcState.satisfied("",""));
        check(BoseArcState.satisfied(null,null));

        System.out.println("BoseArcStateTest passed");
    }
}
