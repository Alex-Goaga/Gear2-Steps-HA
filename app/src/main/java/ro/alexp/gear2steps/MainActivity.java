package ro.alexp.gear2steps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity
{
    private TextView statusText;
    private Handler handler;

    private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            refreshStatus();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestBodySensorPermission();

        handler = new Handler();

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(12, 12, 12, 12);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Gear2 Steps HA");
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        Button startButton = new Button(this);
        startButton.setText("Start service");
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, StepService.class);
                intent.putExtra("reason", "ui_start");
                startService(intent);
                refreshStatus();
            }
        });
        layout.addView(startButton);

        Button testButton = new Button(this);
        testButton.setText("Test server");
        testButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent("ro.alexp.gear2steps.TEST_SERVER");
                sendBroadcast(intent);
                refreshStatus();
            }
        });
        layout.addView(testButton);

        Button sendButton = new Button(this);
        sendButton.setText("Trimite acum");
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent("ro.alexp.gear2steps.SEND_NOW");
                sendBroadcast(intent);
                refreshStatus();
            }
        });
        layout.addView(sendButton);

        Button resetButton = new Button(this);
        resetButton.setText("Reset azi");
        resetButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                confirmResetToday();
            }
        });
        layout.addView(resetButton);

        statusText = new TextView(this);
        statusText.setTextSize(12);
        statusText.setPadding(0, 10, 0, 0);
        layout.addView(statusText);

        scrollView.addView(layout);
        setContentView(scrollView);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        startService(new Intent(this, StepService.class));
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void requestBodySensorPermission()
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 10);
            }
        }
    }

    private void confirmResetToday()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset pasi azi?");
        builder.setMessage("Sigur vrei sa resetezi pasii de azi la 0?");

        builder.setPositiveButton("Da", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Intent intent = new Intent("ro.alexp.gear2steps.RESET_TODAY");
                sendBroadcast(intent);
                refreshStatus();
            }
        });

        builder.setNegativeButton("Nu", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void refreshStatus()
    {
        SharedPreferences prefs = Prefs.get(this);

        String url = Prefs.getWebhookUrl(this);
        String date = prefs.getString(Prefs.KEY_DATE, "-");
        int steps = prefs.getInt(Prefs.KEY_LAST_STEPS, 0);
        int raw = prefs.getInt(Prefs.KEY_LAST_RAW, 0);
        int baseline = prefs.getInt(Prefs.KEY_BASELINE, -1);
        boolean sensorOk = prefs.getBoolean(Prefs.KEY_SENSOR_OK, false);
        boolean serviceRunning = prefs.getBoolean(Prefs.KEY_SERVICE_RUNNING, false);
        String lastStatus = prefs.getString(Prefs.KEY_LAST_STATUS, "-");
        int batteryLevel = prefs.getInt(Prefs.KEY_LAST_BATTERY_LEVEL, -1);
        boolean batteryCharging = prefs.getBoolean(Prefs.KEY_LAST_BATTERY_CHARGING, false);
        int batteryVoltageMv = prefs.getInt(Prefs.KEY_LAST_BATTERY_VOLTAGE_MV, -1);
        float batteryTemperatureC = prefs.getFloat(Prefs.KEY_LAST_BATTERY_TEMPERATURE_C, -1);
        String lastAttemptTime = prefs.getString(Prefs.KEY_LAST_ATTEMPT_TIME, "-");
        String lastSuccessTime = prefs.getString(Prefs.KEY_LAST_SUCCESS_TIME, "-");
        String lastErrorTime = prefs.getString(Prefs.KEY_LAST_ERROR_TIME, "-");
        String lastReason = prefs.getString(Prefs.KEY_LAST_REASON, "-");
        int lastHttpCode = prefs.getInt(Prefs.KEY_LAST_HTTP_CODE, 0);

        String text = "Service: " + serviceRunning + "\n"
                + "Senzor: " + sensorOk + "\n"
                + "Data pasi: " + date + "\n"
                + "Pasi azi: " + steps + "\n"
                + "Baterie: " + batteryLevel + "%\n"
                + "La incarcat: " + batteryCharging + "\n"
                + "Temp baterie: " + batteryTemperatureC + " C\n"
                + "Voltaj baterie: " + batteryVoltageMv + " mV\n"
                + "Ultima incercare: " + lastAttemptTime + "\n"
                + "Ultimul OK: " + lastSuccessTime + "\n"
                + "Ultima eroare: " + lastErrorTime + "\n"
                + "Motiv: " + lastReason + "\n"
                + "HTTP: " + lastHttpCode + "\n"
                + "Raw: " + raw + "\n"
                + "Baseline: " + baseline + "\n"
                + "URL: " + url + "\n\n"
                + lastStatus;

        statusText.setText(text);
    }
}
