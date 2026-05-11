package ro.alexp.gear2steps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class StepService extends Service implements SensorEventListener
{
    private static final String TAG = "Gear2StepsHA";
    private static final long UPLOAD_INTERVAL_MS = AppConfig.UPLOAD_INTERVAL_MS;

    private SensorManager sensorManager;
    private Sensor stepCounter;
    private Handler handler;
    private boolean uploadRunning = false;

    private Runnable periodicUpload = new Runnable()
    {
        @Override
        public void run()
        {
            sendNow("periodic");
            handler.postDelayed(this, UPLOAD_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();

        SharedPreferences prefs = Prefs.get(this);
        prefs.edit()
                .putBoolean(Prefs.KEY_SERVICE_RUNNING, true)
                .putString(Prefs.KEY_LAST_STATUS, "Service pornit: " + DateUtil.now())
                .apply();

        handler = new Handler();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null)
        {
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        if (stepCounter != null)
        {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
            prefs.edit()
                    .putBoolean(Prefs.KEY_SENSOR_OK, true)
                    .putString(Prefs.KEY_LAST_STATUS, "Senzor step counter gasit")
                    .apply();
        }
        else
        {
            prefs.edit()
                    .putBoolean(Prefs.KEY_SENSOR_OK, false)
                    .putString(Prefs.KEY_LAST_STATUS, "Nu gasesc TYPE_STEP_COUNTER")
                    .apply();
        }

        handler.postDelayed(periodicUpload, 15000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        String reason = "start";

        if (intent != null)
        {
            String extraReason = intent.getStringExtra("reason");

            if (extraReason != null && extraReason.trim().length() > 0)
            {
                reason = extraReason;
            }
        }

        sendNow(reason);
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (sensorManager != null)
        {
            sensorManager.unregisterListener(this);
        }

        if (handler != null)
        {
            handler.removeCallbacks(periodicUpload);
        }

        Prefs.get(this).edit()
                .putBoolean(Prefs.KEY_SERVICE_RUNNING, false)
                .putString(Prefs.KEY_LAST_STATUS, "Service oprit: " + DateUtil.now())
                .apply();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER)
        {
            int raw = Math.round(event.values[0]);
            updateSteps(raw);

            SharedPreferences prefs = Prefs.get(this);
            long lastUpload = prefs.getLong(Prefs.KEY_LAST_UPLOAD_MS, 0);
            long now = System.currentTimeMillis();

            if ((now - lastUpload) > UPLOAD_INTERVAL_MS)
            {
                sendNow("sensor_interval");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }

    private void updateSteps(int raw)
    {
        SharedPreferences prefs = Prefs.get(this);
        String today = DateUtil.today();
        String savedDate = prefs.getString(Prefs.KEY_DATE, "");
        int baseline = prefs.getInt(Prefs.KEY_BASELINE, -1);

        if (!today.equals(savedDate))
        {
            baseline = raw;
            savedDate = today;
        }

        if (baseline < 0)
        {
            baseline = raw;
        }

        if (raw < baseline)
        {
            // Senzorul TYPE_STEP_COUNTER s-a resetat, de obicei dupa reboot.
            baseline = raw;
        }

        int stepsToday = raw - baseline;

        if (stepsToday < 0)
        {
            stepsToday = 0;
        }

        prefs.edit()
                .putString(Prefs.KEY_DATE, savedDate)
                .putInt(Prefs.KEY_BASELINE, baseline)
                .putInt(Prefs.KEY_LAST_RAW, raw)
                .putInt(Prefs.KEY_LAST_STEPS, stepsToday)
                .apply();
    }

    private void checkDayRollover()
    {
        SharedPreferences prefs = Prefs.get(this);
        String today = DateUtil.today();
        String savedDate = prefs.getString(Prefs.KEY_DATE, "");

        if (!today.equals(savedDate))
        {
            int raw = prefs.getInt(Prefs.KEY_LAST_RAW, 0);

            prefs.edit()
                    .putString(Prefs.KEY_DATE, today)
                    .putInt(Prefs.KEY_BASELINE, raw)
                    .putInt(Prefs.KEY_LAST_STEPS, 0)
                    .putString(Prefs.KEY_LAST_STATUS, "Zi noua detectata: " + today + ", baseline=" + raw)
                    .apply();
        }
    }

    public void sendNow(final String reason)
    {
        if (uploadRunning)
        {
            return;
        }

        checkDayRollover();

        final SharedPreferences prefs = Prefs.get(this);
        final String url = Prefs.getWebhookUrl(this);
        final int steps = prefs.getInt(Prefs.KEY_LAST_STEPS, 0);
        final int raw = prefs.getInt(Prefs.KEY_LAST_RAW, 0);
        final String date = prefs.getString(Prefs.KEY_DATE, DateUtil.today());
        final BatteryInfo batteryInfo = BatteryUtil.read(this);
        final String attemptTime = DateUtil.now();

        prefs.edit()
                .putString(Prefs.KEY_LAST_ATTEMPT_TIME, attemptTime)
                .putString(Prefs.KEY_LAST_REASON, reason)
                .putInt(Prefs.KEY_LAST_BATTERY_LEVEL, batteryInfo.level)
                .putBoolean(Prefs.KEY_LAST_BATTERY_CHARGING, batteryInfo.charging)
                .putInt(Prefs.KEY_LAST_BATTERY_STATUS, batteryInfo.status)
                .putInt(Prefs.KEY_LAST_BATTERY_PLUGGED, batteryInfo.plugged)
                .putInt(Prefs.KEY_LAST_BATTERY_VOLTAGE_MV, batteryInfo.voltageMv)
                .putFloat(Prefs.KEY_LAST_BATTERY_TEMPERATURE_C, batteryInfo.temperatureC)
                .putInt(Prefs.KEY_LAST_BATTERY_TEMPERATURE_RAW, batteryInfo.temperatureRaw)
                .apply();

        if (url == null || url.trim().length() == 0)
        {
            prefs.edit()
                    .putString(Prefs.KEY_LAST_STATUS, "URL Home Assistant lipsa")
                    .apply();
            return;
        }

        uploadRunning = true;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String json = "{"
                            + "\"steps\":" + steps + ","
                            + "\"raw\":" + raw + ","
                            + "\"battery_level\":" + batteryInfo.level + ","
                            + "\"battery_charging\":" + batteryInfo.charging + ","
                            + "\"battery_status\":" + batteryInfo.status + ","
                            + "\"battery_plugged\":" + batteryInfo.plugged + ","
                            + "\"battery_voltage_mv\":" + batteryInfo.voltageMv + ","
                            + "\"battery_temperature_c\":" + batteryInfo.temperatureC + ","
                            + "\"battery_temperature_raw\":" + batteryInfo.temperatureRaw + ","
                            + "\"date\":\"" + date + "\","
                            + "\"source\":\"Samsung Gear 2\","
                            + "\"reason\":\"" + reason + "\","
                            + "\"updated\":\"" + DateUtil.now() + "\""
                            + "}";

                    int code = HttpSender.postJson(url, json);
                    String successTime = DateUtil.now();

                    prefs.edit()
                            .putLong(Prefs.KEY_LAST_UPLOAD_MS, System.currentTimeMillis())
                            .putString(Prefs.KEY_LAST_SUCCESS_TIME, successTime)
                            .putInt(Prefs.KEY_LAST_HTTP_CODE, code)
                            .putString(Prefs.KEY_LAST_STATUS, "Upload OK HTTP " + code + ": " + steps + " pasi, baterie " + batteryInfo.level + "%, ora " + DateUtil.time())
                            .apply();

                    Log.d(TAG, "Upload OK HTTP " + code + " reason=" + reason + " steps=" + steps + " battery=" + batteryInfo.level + " temp=" + batteryInfo.temperatureC + " voltage=" + batteryInfo.voltageMv);
                }
                catch (Exception e)
                {
                    String errorTime = DateUtil.now();

                    prefs.edit()
                            .putString(Prefs.KEY_LAST_ERROR_TIME, errorTime)
                            .putString(Prefs.KEY_LAST_STATUS, "Upload eroare " + DateUtil.time() + ": " + e.getMessage())
                            .apply();

                    Log.e(TAG, "Upload error reason=" + reason, e);
                }

                uploadRunning = false;
            }
        }).start();
    }
}
