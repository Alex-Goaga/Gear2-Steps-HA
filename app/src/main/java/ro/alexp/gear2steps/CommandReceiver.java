package ro.alexp.gear2steps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class CommandReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null || intent.getAction() == null)
        {
            return;
        }

        String action = intent.getAction();
        SharedPreferences prefs = Prefs.get(context);
        String reason = "start";
        boolean shouldStartService = true;

        if ("ro.alexp.gear2steps.SET_URL".equals(action))
        {
            String url = intent.getStringExtra("url");

            if (url != null && url.length() > 0)
            {
                prefs.edit()
                        .putString(Prefs.KEY_URL, url)
                        .putString(Prefs.KEY_LAST_STATUS, "URL salvat: " + url)
                        .apply();
            }

            reason = "set_url";
        }

        if ("ro.alexp.gear2steps.RESET_TODAY".equals(action))
        {
            int raw = prefs.getInt(Prefs.KEY_LAST_RAW, 0);

            prefs.edit()
                    .putString(Prefs.KEY_DATE, DateUtil.today())
                    .putInt(Prefs.KEY_BASELINE, raw)
                    .putInt(Prefs.KEY_LAST_STEPS, 0)
                    .putString(Prefs.KEY_LAST_STATUS, "Reset azi facut la raw=" + raw + ", ora " + DateUtil.time())
                    .apply();

            reason = "reset_today";
        }

        if ("ro.alexp.gear2steps.SEED_TODAY".equals(action))
        {
            int steps = intent.getIntExtra("steps", 0);
            int raw = prefs.getInt(Prefs.KEY_LAST_RAW, 0);
            int baseline = raw - steps;

            if (baseline < 0)
            {
                baseline = 0;
            }

            prefs.edit()
                    .putString(Prefs.KEY_DATE, DateUtil.today())
                    .putInt(Prefs.KEY_BASELINE, baseline)
                    .putInt(Prefs.KEY_LAST_STEPS, steps)
                    .putString(Prefs.KEY_LAST_STATUS, "Seed azi: " + steps + " pasi, baseline=" + baseline)
                    .apply();

            reason = "seed_today";
        }

        if ("ro.alexp.gear2steps.SEND_NOW".equals(action))
        {
            prefs.edit()
                    .putString(Prefs.KEY_LAST_STATUS, "Trimitere manuala ceruta: " + DateUtil.time())
                    .apply();

            reason = "manual";
        }

        if ("ro.alexp.gear2steps.TEST_SERVER".equals(action))
        {
            prefs.edit()
                    .putString(Prefs.KEY_LAST_STATUS, "Test server cerut: " + DateUtil.time())
                    .apply();

            reason = "server_test";
        }

        if (shouldStartService)
        {
            Intent serviceIntent = new Intent(context, StepService.class);
            serviceIntent.putExtra("reason", reason);
            context.startService(serviceIntent);
        }
    }
}
