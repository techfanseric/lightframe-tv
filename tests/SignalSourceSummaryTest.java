package local.simple.tvoverlay;

import java.util.ArrayList;
import java.util.List;

/** Run with a plain JDK. Validates the signal-source label rules. */
public final class SignalSourceSummaryTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // A vendor label names its port; a device name does not.
        check(SignalInputs.portFromLabel("HDMI 1")==1);
        check(SignalInputs.portFromLabel("HDMI 2")==2);
        check(SignalInputs.portFromLabel("HDMI 3")==3);
        check(SignalInputs.portFromLabel("HDMI4")==4);
        check(SignalInputs.portFromLabel("HDMI 2.1")==2);
        check(SignalInputs.portFromLabel("Apple TV")==-1);
        check(SignalInputs.portFromLabel("Composite")==-1);
        check(SignalInputs.portFromLabel("Tuner(DTV)")==-1);
        check(SignalInputs.portFromLabel(null)==-1);
        check(SignalInputs.portFromLabel("")==-1);

        // The summary names only attached ports and marks the current one.
        List<SignalInputs.Port> ports=new ArrayList<>();
        ports.add(new SignalInputs.Port(1,"HDMI 1","",false,false));
        ports.add(new SignalInputs.Port(2,"HDMI 2","Apple TV",true,true));
        ports.add(new SignalInputs.Port(3,"HDMI 3","",false,false));
        check(SignalInputs.summary(ports).equals("HDMI 2 当前"));

        // Several attached ports are listed together.
        ports.add(new SignalInputs.Port(3,"HDMI 3","",true,false));
        check(SignalInputs.summary(ports).equals("HDMI 2 当前 HDMI 3"));

        // Nothing attached reads as unconnected rather than as an error.
        List<SignalInputs.Port> empty=new ArrayList<>();
        empty.add(new SignalInputs.Port(1,"HDMI 1","",false,false));
        check(SignalInputs.summary(empty).equals("未接入"));
        check(SignalInputs.summary(new ArrayList<SignalInputs.Port>()).equals("未接入"));
        check(SignalInputs.summary(null).equals("未接入"));

        // The entry is named after the device, and falls back to the port.
        check(ports.get(1).title().equals("Apple TV"));
        check(empty.get(0).title().equals("HDMI 1"));

        // The vendor source id of HDMI 1 is 23.
        check(SignalInputs.SOURCE_OFFSET==22);
        check(23-SignalInputs.SOURCE_OFFSET==1);
        check(24-SignalInputs.SOURCE_OFFSET==2);
        check(25-SignalInputs.SOURCE_OFFSET==3);

        System.out.println("SignalSourceSummaryTest passed");
    }
}
