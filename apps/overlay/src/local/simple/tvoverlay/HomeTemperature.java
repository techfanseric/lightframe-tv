package local.simple.tvoverlay;

/** HA climate attributes use the instance unit unless the entity explicitly supplies its own. */
final class HomeTemperature {
    static double celsius(double value,String unit){
        if("°F".equals(unit))return (value-32)*5/9;
        if("°C".equals(unit))return value;
        throw new IllegalArgumentException("Unknown temperature unit");
    }
    static double nativeTarget(double value,String unit,double min,double max,double step){
        if(!Double.isFinite(value)||value<5||value>40)throw new IllegalArgumentException("Invalid target");
        double nativeValue="°F".equals(unit)?value*9/5+32:"°C".equals(unit)?value:Double.NaN;
        if(!Double.isFinite(nativeValue)||!Double.isFinite(min)||!Double.isFinite(max)||min>max)throw new IllegalArgumentException("Invalid temperature scale");
        if(Double.isFinite(step)&&step>0)nativeValue=min+Math.rint((nativeValue-min)/step)*step;
        if(nativeValue<min-0.001||nativeValue>max+0.001)throw new IllegalArgumentException("Target outside supported range");
        return Math.rint(nativeValue*1000)/1000;
    }
}
