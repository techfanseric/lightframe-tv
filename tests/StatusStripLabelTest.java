package local.simple.tvoverlay;

/** Run with a plain JDK. Validates the status strip's label contract. */
public final class StatusStripLabelTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        // A direct connection is the expected case and carries no label.
        check(ProxyMode.statusLabel(true,"HK 01"," · 42ms").isEmpty());
        check(ProxyMode.statusLabel(true,"暂不可用","").isEmpty());

        // The proxy mode keeps naming its line and its delay.
        check(ProxyMode.statusLabel(false,"HK 01"," · 42ms").equals("Clash · HK 01 · 42ms"));
        check(ProxyMode.statusLabel(false,"暂不可用","").equals("Clash · 暂不可用"));
        check(ProxyMode.statusLabel(false,"速度最优"," · 120ms").equals("Clash · 速度最优 · 120ms"));

        // No Chinese text may survive in the direct case.
        String direct=ProxyMode.statusLabel(true,"速度最优"," · 42ms");
        for(int i=0;i<direct.length();i++){
            char c=direct.charAt(i);
            check(c<0x2E80||c>0x9FFF);
        }

        System.out.println("StatusStripLabelTest passed");
    }
}
