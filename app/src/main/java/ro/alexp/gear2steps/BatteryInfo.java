package ro.alexp.gear2steps;

public class BatteryInfo
{
    public int level;
    public boolean charging;
    public int status;
    public int plugged;
    public int voltageMv;
    public int temperatureRaw;
    public float temperatureC;

    public BatteryInfo()
    {
        level = -1;
        charging = false;
        status = -1;
        plugged = 0;
        voltageMv = -1;
        temperatureRaw = -1;
        temperatureC = -1;
    }
}
