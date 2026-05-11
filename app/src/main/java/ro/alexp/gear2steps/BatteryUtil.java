package ro.alexp.gear2steps;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryUtil
{
    public static BatteryInfo read(Context context)
    {
        BatteryInfo info = new BatteryInfo();

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (batteryStatus == null)
        {
            return info;
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        int temperatureRaw = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);

        int percent = -1;

        if (level >= 0 && scale > 0)
        {
            percent = Math.round((level * 100.0f) / scale);
        }

        boolean charging = false;

        if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
        {
            charging = true;
        }

        float temperatureC = -1;

        if (temperatureRaw >= 0)
        {
            temperatureC = temperatureRaw / 10.0f;
        }

        info.level = percent;
        info.charging = charging;
        info.status = status;
        info.plugged = plugged;
        info.voltageMv = voltageMv;
        info.temperatureRaw = temperatureRaw;
        info.temperatureC = temperatureC;

        return info;
    }
}
