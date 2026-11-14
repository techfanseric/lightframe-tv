package local.simple.tvoverlay;

import java.util.ArrayList;
import java.util.List;

/** Run with a plain JDK. Validates which applications the row offers. */
public final class AppVisibilityTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // Netflix is hidden until the owner asks for it again.
        check(AppVisibility.hiddenByDefault("netflix"));
        check(!AppVisibility.hiddenByDefault("youtube"));
        check(!AppVisibility.hiddenByDefault("bilibili"));
        check(!AppVisibility.hiddenByDefault("iptv"));
        check(!AppVisibility.hiddenByDefault("nonsense"));

        // An empty list of shown applications keeps the hidden one hidden.
        check(!shown("").contains("netflix"));
        check(shown("").contains("youtube"));
        check(shown("").contains("bilibili"));
        check(shown("").contains("iptv"));

        // The owner can bring the entry back.
        check(shown("netflix").contains("netflix"));

        // A stray comma or an unknown name changes nothing.
        check(!shown(",").contains("netflix"));
        check(shown("nonsense").contains("youtube"));
        check(!shown("nonsense").contains("netflix"));

        System.out.println("AppVisibilityTest passed");
    }

    /** The applications the row would offer for a shown list. */
    private static String shown(String shownApps){
        List<String> ids=new ArrayList<>();
        for(String id:AppCatalog.IDS){
            if(!AppVisibility.hiddenByDefault(id)){ids.add(id);continue;}
            for(String s:shownApps.split(","))if(id.equals(s)){ids.add(id);break;}
        }
        return String.join(",",ids);
    }
}
