package local.simple.tvoverlay;

public final class BootHomePolicyTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,10100,10200));
        check(BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,9900,10200));
        // onNewIntent and explicit settings always cancel, even immediately.
        check(!BootHomePolicy.preservePendingBoot(false,false,true,true,9,9,10000,10100,10200));
        check(!BootHomePolicy.preservePendingBoot(true,true,true,true,9,9,10000,10100,10200));
        check(!BootHomePolicy.preservePendingBoot(true,false,false,true,9,9,10000,10100,10200));
        check(!BootHomePolicy.preservePendingBoot(true,false,true,false,9,9,10000,10100,10200));
        check(!BootHomePolicy.preservePendingBoot(true,false,true,true,8,9,10000,10100,10200));
        check(!BootHomePolicy.preservePendingBoot(true,false,true,true,-1,-1,10000,10100,10200));
        // Ordinary HOME-triggered boot does not get broadcast/bootstrap exemption.
        check(!BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,-1,10100,10200));
        // A delayed first HOME must not cancel an otherwise valid cold boot.
        check(BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,12000,12001));
        check(BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,60000,60001));
        check(BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,8000,10200));
        check(!BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,135001,135001));
        check(!BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,135000,9999,135000));
        check(!BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,10300,10200));
        check(!BootHomePolicy.preservePendingBoot(true,false,true,true,9,9,10000,9900,9999));
        // BOOT while asleep does not create a request. SCREEN_ON or sticky
        // recovery later starts one and must admit this boot's first HOME.
        check(BootHomePolicy.preservePendingBoot(true,false,true,true,10,10,20000,23500,24000));
        check(!BootHomePolicy.preservePendingBoot(false,false,true,true,10,10,20000,23500,24000));
        System.out.println("BootHomePolicyTest passed (18 checks)");
    }
}
