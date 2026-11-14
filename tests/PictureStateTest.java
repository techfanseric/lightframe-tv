package local.simple.tvoverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The picture is decided from the usage events of the television. */
public final class PictureStateTest {
    private static final String TVPLAYER = "com.xiaomi.mitv.tvplayer";
    private static final String TUBE = "io.gh.reisxd.tizentube.cobalt";
    private static final String HOME = "local.simple.tvhome.dock";

    public static void main(String[] args) {
        Set<String> candidates = new HashSet<>(Arrays.asList(TVPLAYER, TUBE, HOME));

        // The source player came to the front and never left: the picture
        // is the attached device.
        PictureState.Picture source = PictureState.picture(Arrays.asList(
                new PictureState.Event(TVPLAYER, TVPLAYER + ".ExternalSourceActivity", true, 100)),
                candidates);
        require(source != null && TVPLAYER.equals(source.packageName), "source player is the picture");

        // The source player left and an application came to the front: the
        // picture is the application.
        PictureState.Picture app = PictureState.picture(Arrays.asList(
                new PictureState.Event(TVPLAYER, TVPLAYER + ".ExternalSourceActivity", true, 100),
                new PictureState.Event(TVPLAYER, TVPLAYER + ".ExternalSourceActivity", false, 110),
                new PictureState.Event(TUBE, TUBE + ".MainActivity", true, 120)),
                candidates);
        require(app != null && TUBE.equals(app.packageName), "the application replaces the source");

        // The launcher's own window sits on top of the picture and must not
        // hide it.
        PictureState.Picture behind = PictureState.picture(Arrays.asList(
                new PictureState.Event(TUBE, TUBE + ".MainActivity", true, 100),
                new PictureState.Event(HOME, HOME + ".MainActivity", true, 110),
                new PictureState.Event(HOME, HOME + ".MainActivity", false, 120)),
                candidates);
        require(behind != null && TUBE.equals(behind.packageName), "the launcher does not hide the picture");

        // An application that shows two activities at once keeps the picture
        // until the one that came last leaves.
        PictureState.Picture two = PictureState.picture(Arrays.asList(
                new PictureState.Event(TUBE, TUBE + ".MainActivity", true, 100),
                new PictureState.Event(TUBE, TUBE + ".DialogActivity", true, 110),
                new PictureState.Event(TUBE, TUBE + ".DialogActivity", false, 120)),
                candidates);
        require(two != null && TUBE.equals(two.packageName), "one application keeps the picture");

        // The picture that came to the front last is the one that is shown.
        PictureState.Picture last = PictureState.picture(Arrays.asList(
                new PictureState.Event(TUBE, TUBE + ".MainActivity", true, 100),
                new PictureState.Event(TVPLAYER, TVPLAYER + ".ExternalSourceActivity", true, 120)),
                candidates);
        require(last != null && TVPLAYER.equals(last.packageName), "the last one in front wins");
        require(last.time == 120, "the moment is kept");

        // Nothing is known, and nothing is claimed.
        require(PictureState.picture(new ArrayList<PictureState.Event>(), candidates) == null,
                "no events mean no picture");
        require(PictureState.picture(null, candidates) == null, "no events mean no picture");

        // An event for another application is not a candidate and is left
        // out of the decision.
        PictureState.Picture other = PictureState.picture(Arrays.asList(
                new PictureState.Event("com.other.app", "com.other.app.Main", true, 100)),
                candidates);
        require(other == null, "another application is not the picture");

        System.out.println("PictureStateTest OK");
    }

    private static void require(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
    }
}
