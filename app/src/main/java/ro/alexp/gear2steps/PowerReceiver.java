package ro.alexp.gear2steps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class PowerReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null || intent.getAction() == null)
        {
            return;
        }

        String action = intent.getAction();
        String reason = "power_changed";

        if (Intent.ACTION_POWER_CONNECTED.equals(action) || "ro.alexp.gear2steps.TEST_POWER_CONNECTED".equals(action))
        {
            reason = "power_connected";
        }

        if (Intent.ACTION_POWER_DISCONNECTED.equals(action) || "ro.alexp.gear2steps.TEST_POWER_DISCONNECTED".equals(action))
        {
            reason = "power_disconnected";
        }

        BatteryInfo batteryInfo = BatteryUtil.read(context);
        SharedPreferences prefs = Prefs.get(context);

        prefs.edit()
                .putInt(Prefs.KEY_LAST_BATTERY_LEVEL, batteryInfo.level)
                .putBoolean(Prefs.KEY_LAST_BATTERY_CHARGING, batteryInfo.charging)
                .putInt(Prefs.KEY_LAST_BATTERY_STATUS, batteryInfo.status)
                .putInt(Prefs.KEY_LAST_BATTERY_PLUGGED, batteryInfo.plugged)
                .putInt(Prefs.KEY_LAST_BATTERY_VOLTAGE_MV, batteryInfo.voltageMv)
                .putFloat(Prefs.KEY_LAST_BATTERY_TEMPERATURE_C, batteryInfo.temperatureC)
                .putInt(Prefs.KEY_LAST_BATTERY_TEMPERATURE_RAW, batteryInfo.temperatureRaw)
                .putString(Prefs.KEY_LAST_STATUS, "Schimbare incarcare detectata: " + reason + ", baterie " + batteryInfo.level + "%")
                .apply();

        Intent serviceIntent = new Intent(context, StepService.class);
        serviceIntent.putExtra("reason", reason);
        context.startService(serviceIntent);
    }
}
