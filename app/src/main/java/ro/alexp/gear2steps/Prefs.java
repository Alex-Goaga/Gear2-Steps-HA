package ro.alexp.gear2steps;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs
{
    public static final String PREFS = "gear2_steps_prefs";

    public static final String KEY_URL = "url";
    public static final String KEY_DATE = "date";
    public static final String KEY_BASELINE = "baseline";
    public static final String KEY_LAST_RAW = "last_raw";
    public static final String KEY_LAST_STEPS = "last_steps";
    public static final String KEY_LAST_UPLOAD_MS = "last_upload_ms";
    public static final String KEY_LAST_STATUS = "last_status";
    public static final String KEY_SENSOR_OK = "sensor_ok";
    public static final String KEY_SERVICE_RUNNING = "service_running";
    public static final String KEY_LAST_BATTERY_LEVEL = "last_battery_level";
    public static final String KEY_LAST_BATTERY_CHARGING = "last_battery_charging";
    public static final String KEY_LAST_BATTERY_STATUS = "last_battery_status";
    public static final String KEY_LAST_BATTERY_PLUGGED = "last_battery_plugged";
    public static final String KEY_LAST_BATTERY_VOLTAGE_MV = "last_battery_voltage_mv";
    public static final String KEY_LAST_BATTERY_TEMPERATURE_C = "last_battery_temperature_c";
    public static final String KEY_LAST_BATTERY_TEMPERATURE_RAW = "last_battery_temperature_raw";

    public static final String KEY_LAST_ATTEMPT_TIME = "last_attempt_time";
    public static final String KEY_LAST_SUCCESS_TIME = "last_success_time";
    public static final String KEY_LAST_ERROR_TIME = "last_error_time";
    public static final String KEY_LAST_REASON = "last_reason";
    public static final String KEY_LAST_HTTP_CODE = "last_http_code";

    public static SharedPreferences get(Context context)
    {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getWebhookUrl(Context context)
    {
        SharedPreferences prefs = get(context);
        String url = prefs.getString(KEY_URL, "");

        if (url == null || url.trim().length() == 0)
        {
            return AppConfig.DEFAULT_WEBHOOK_URL;
        }

        return url;
    }
}
