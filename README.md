# Gear2 Steps HA

Because apparently I did not get the memo that my 2014-ish Samsung Gear 2 it is supposed to be dead.

**Gear2 Steps HA** is a tiny Android Wear app for the **Samsung Gear 2 / Gear 2 321f running the Android Wear port**. It reads the watch step counter and battery information, then sends everything to **Home Assistant** through a webhook.

This project exists because I refuse to let old hardware die just because a company decided the software lifecycle is over. If you are reading this: please support **Right to Repair**, open bootloaders, community firmware, and keeping perfectly good devices useful instead of turning them into electronic sadness.

Old tech is not trash. Sometimes it just needs one more APK and a little stubbornness.

---

## What it does

The app can send the following data to Home Assistant:

- steps today
- raw step counter value
- battery percentage
- charging state
- battery status
- charging source / plugged state
- battery voltage in mV
- battery temperature in °C
- update reason, for example:
  - `periodic`
  - `manual`
  - `server_test`
  - `power_connected`
  - `power_disconnected`
- timestamp of the update

It can also:

- run in the background as a small Android service
- start again after boot
- send immediately when the watch is put on the charger
- send immediately when the watch is removed from the charger
- show service/sensor/upload status inside the app
- test the Home Assistant server from a button
- ask for confirmation before resetting today's steps

---

## Tested environment

This project was made for:

- Samsung Gear 2 / Gear 2 321f
- Android Wear port from XDA
- Android 6.0.1 / SDK 23
- ARM / `armeabi-v7a`
- ADB over Bluetooth through the Wear OS / Android Wear phone app
- Home Assistant webhook endpoint
- Cloudflare Tunnel or local Home Assistant URL

The original Android Wear port thread is here:
I love these guys :
```text
https://xdaforums.com/t/porting-android-to-gear-2.2992953/
```

---

## Big warning, because reality exists

This is not an official Samsung, Google, ASUS, or Home Assistant app.

It is a small community-style app for a weird but beautiful situation: a Samsung Gear 2 running an Android Wear port and still being useful today.

Bluetooth tethered internet on this port can sometimes get stuck. The watch may still show a connected network, DNS may still resolve, but HTTPS requests may time out. If that happens, restart the watch or toggle Bluetooth/Wear debugging on the phone.

If you see this in logcat:

```text
SocketTimeoutException
failed to connect to your-domain.example.com port 443
```

it is usually the watch Bluetooth tethered internet being moody, not Home Assistant.

---

## Project structure

Important files:

```text
app/src/main/java/ro/alexp/gear2steps/AppConfig.java
app/src/main/java/ro/alexp/gear2steps/MainActivity.java
app/src/main/java/ro/alexp/gear2steps/StepService.java
app/src/main/java/ro/alexp/gear2steps/PowerReceiver.java
app/src/main/AndroidManifest.xml
```

The main configuration is inside:

```text
AppConfig.java
```

---

## Configure the Home Assistant webhook URL

Open:

```text
app/src/main/java/ro/alexp/gear2steps/AppConfig.java
```

Example:

```java
package ro.alexp.gear2steps;

public class AppConfig
{
    public static final String DEFAULT_WEBHOOK_URL = "https://your-home-assistant-domain.example.com/api/webhook/gear2_steps";

    public static final long UPLOAD_INTERVAL_MS = 5 * 60 * 1000;
}
```

For local Home Assistant:

```java
public static final String DEFAULT_WEBHOOK_URL = "http://192.168.1.50:8123/api/webhook/gear2_steps";
```

For Cloudflare Tunnel / public access:

```java
public static final String DEFAULT_WEBHOOK_URL = "https://your-domain.example.com/api/webhook/gear2_steps";
```

Recommended: do not use a simple webhook ID like `gear2_steps` if your Home Assistant is exposed online. Use something long and random:

```text
gear2_steps_7d9f2c1a6b934fa8
```

Then your URL becomes:

```text
https://your-domain.example.com/api/webhook/gear2_steps_7d9f2c1a6b934fa8
```

Security through obscurity is not a fortress, but it is still better than naming your webhook `please_hack_me`.

---

## Build steps

### 1. Open the project

Open the project folder in **Android Studio**:

```text
File > Open > Gear2StepsHA
```

### 2. Let Gradle sync

If Android Studio asks to install missing SDK/build tools, allow it.

This project uses:

```text
minSdk 23
targetSdk 23
compileSdk 35
```

The watch is old, but Android Studio is not, so yes, we do the usual Gradle dance.

### 3. Build APK

In Android Studio:

```text
Build > Build APK(s)
```

The APK will be generated here:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Connect to the watch with ADB

The Gear 2 Android Wear port usually connects through the phone.

### On the watch

Enable:

```text
Developer options > ADB debugging
Developer options > Debug over Bluetooth
```

### On the phone

In the Wear OS / Android Wear app, enable:

```text
Debugging over Bluetooth
```

### On the PC

First connect to the phone through ADB. Example:

```bat
adb connect xxxxxxx:yyyyy
```

Then create the ADB bridge to the watch:

```bat
adb forward tcp:4444 localabstract:/adb-hub
adb connect 127.0.0.1:4444
```

Check devices:

```bat
adb devices -l
```

Expected watch line:

```text
127.0.0.1:4444    device product:b2 model:Samsung_Gear_2 device:b2
```

Sometimes it appears as:

```text
localhost:4444
```

Use exactly the serial shown by `adb devices -l`.

---

## Install the APK

Example using `127.0.0.1:4444`:

```bat
adb -s 127.0.0.1:4444 install -r "app/build/outputs/apk/debug/app-debug.apk"
```

Grant body sensor permission:

```bat
adb -s 127.0.0.1:4444 shell pm grant ro.alexp.gear2steps android.permission.BODY_SENSORS
```

Start the service:

```bat
adb -s 127.0.0.1:4444 shell am startservice -n ro.alexp.gear2steps/.StepService
```

Open the app UI:

```bat
adb -s 127.0.0.1:4444 shell am start -n ro.alexp.gear2steps/.MainActivity
```

---

## Useful ADB commands

Send now:

```bat
adb -s 127.0.0.1:4444 shell am broadcast -a ro.alexp.gear2steps.SEND_NOW
```

Test server:

```bat
adb -s 127.0.0.1:4444 shell am broadcast -a ro.alexp.gear2steps.TEST_SERVER
```

Reset today's steps:

```bat
adb -s 127.0.0.1:4444 shell am broadcast -a ro.alexp.gear2steps.RESET_TODAY
```

Simulate charger connected:

```bat
adb -s 127.0.0.1:4444 shell am broadcast -a ro.alexp.gear2steps.TEST_POWER_CONNECTED
```

Simulate charger disconnected:

```bat
adb -s 127.0.0.1:4444 shell am broadcast -a ro.alexp.gear2steps.TEST_POWER_DISCONNECTED
```

Clear logcat:

```bat
adb -s 127.0.0.1:4444 logcat -c
```

Watch logs:

```bat
adb -s 127.0.0.1:4444 logcat | findstr /i "Gear2StepsHA"
```

Expected success:

```text
Upload OK HTTP 200 reason=server_test steps=123 battery=86 temp=31.2 voltage=4252
```

---

## Home Assistant setup

There are two ways to create the required entities:

1. create helpers from the UI
2. define them in YAML

Use whichever makes you happier. Home Assistant already gives us enough existential configuration choices.

---

## Option A: create helpers from the UI

Go to:

```text
Settings > Devices & services > Helpers
```

Create these helpers:

### Number helper: steps

```text
Name: Gear 2 Steps Today
Entity ID: input_number.gear2_steps_today
Min: 0
Max: 100000
Step: 1
Unit: steps
```

### Number helper: battery level

```text
Name: Gear 2 Battery Level
Entity ID: input_number.gear2_battery_level
Min: 0
Max: 100
Step: 1
Unit: %
```

### Toggle helper: charging

```text
Name: Gear 2 Charging
Entity ID: input_boolean.gear2_battery_charging
```

### Number helper: battery temperature

```text
Name: Gear 2 Battery Temperature
Entity ID: input_number.gear2_battery_temperature
Min: 0
Max: 80
Step: 0.1
Unit: °C
```

### Number helper: battery voltage

```text
Name: Gear 2 Battery Voltage
Entity ID: input_number.gear2_battery_voltage
Min: 3000
Max: 4500
Step: 1
Unit: mV
```

Optional text helpers:

```text
input_text.gear2_last_reason
input_text.gear2_last_seen
```

---

## Option B: YAML helpers

Add to `configuration.yaml` or your split helper YAML file:

```yaml
input_number:
  gear2_steps_today:
    name: Gear 2 Steps Today
    min: 0
    max: 100000
    step: 1
    mode: box
    icon: mdi:shoe-print

  gear2_battery_level:
    name: Gear 2 Battery Level
    min: 0
    max: 100
    step: 1
    mode: box
    icon: mdi:battery

  gear2_battery_temperature:
    name: Gear 2 Battery Temperature
    min: 0
    max: 80
    step: 0.1
    mode: box
    unit_of_measurement: "°C"
    icon: mdi:thermometer

  gear2_battery_voltage:
    name: Gear 2 Battery Voltage
    min: 3000
    max: 4500
    step: 1
    mode: box
    unit_of_measurement: "mV"
    icon: mdi:sine-wave

input_boolean:
  gear2_battery_charging:
    name: Gear 2 Charging
    icon: mdi:battery-charging

input_text:
  gear2_last_reason:
    name: Gear 2 Last Reason
    max: 100

  gear2_last_seen:
    name: Gear 2 Last Seen
    max: 100
```

Restart Home Assistant or reload helpers, depending on your setup.

---

## Home Assistant automation

### If you edit automations from the UI YAML editor

Do **not** include `automation:` at the top.

Paste this directly into the automation YAML editor:

```yaml
alias: Gear 2 - receive steps and battery
description: Receives Samsung Gear 2 data through webhook
triggers:
  - trigger: webhook
    webhook_id: gear2_steps
    allowed_methods:
      - POST
    local_only: false

conditions: []

actions:
  - action: input_number.set_value
    target:
      entity_id: input_number.gear2_steps_today
    data:
      value: "{{ trigger.json.steps | int(0) }}"

  - choose:
      - conditions:
          - condition: template
            value_template: "{{ trigger.json.battery_level is defined and trigger.json.battery_level | int(-1) >= 0 }}"
        sequence:
          - action: input_number.set_value
            target:
              entity_id: input_number.gear2_battery_level
            data:
              value: "{{ trigger.json.battery_level | int(0) }}"

  - choose:
      - conditions:
          - condition: template
            value_template: "{{ trigger.json.battery_temperature_c is defined and trigger.json.battery_temperature_c | float(-1) >= 0 }}"
        sequence:
          - action: input_number.set_value
            target:
              entity_id: input_number.gear2_battery_temperature
            data:
              value: "{{ trigger.json.battery_temperature_c | float(0) }}"

  - choose:
      - conditions:
          - condition: template
            value_template: "{{ trigger.json.battery_voltage_mv is defined and trigger.json.battery_voltage_mv | int(-1) > 0 }}"
        sequence:
          - action: input_number.set_value
            target:
              entity_id: input_number.gear2_battery_voltage
            data:
              value: "{{ trigger.json.battery_voltage_mv | int(0) }}"

  - choose:
      - conditions:
          - condition: template
            value_template: "{{ trigger.json.battery_charging == true }}"
        sequence:
          - action: input_boolean.turn_on
            target:
              entity_id: input_boolean.gear2_battery_charging
    default:
      - action: input_boolean.turn_off
        target:
          entity_id: input_boolean.gear2_battery_charging

  - choose:
      - conditions:
          - condition: template
            value_template: "{{ trigger.json.reason is defined }}"
        sequence:
          - action: input_text.set_value
            target:
              entity_id: input_text.gear2_last_reason
            data:
              value: "{{ trigger.json.reason }}"

  - action: input_text.set_value
    target:
      entity_id: input_text.gear2_last_seen
    data:
      value: "{{ now().strftime('%Y-%m-%d %H:%M:%S') }}"

mode: restart
```

### If you use `automations.yaml`

Use the same content as above, but as a list item:

```yaml
- alias: Gear 2 - receive steps and battery
  description: Receives Samsung Gear 2 data through webhook
  triggers:
    - trigger: webhook
      webhook_id: gear2_steps
      allowed_methods:
        - POST
      local_only: false
  conditions: []
  actions:
    # paste the same actions here
  mode: restart
```

---

## Template sensors

Optional, but nicer for dashboards:

```yaml
template:
  - sensor:
      - name: Gear 2 Steps Today
        unique_id: gear2_steps_today_sensor
        state: "{{ states('input_number.gear2_steps_today') | int(0) }}"
        unit_of_measurement: "steps"
        icon: mdi:shoe-print

      - name: Gear 2 Battery
        unique_id: gear2_battery_sensor
        state: "{{ states('input_number.gear2_battery_level') | int(0) }}"
        unit_of_measurement: "%"
        device_class: battery

      - name: Gear 2 Battery Temperature
        unique_id: gear2_battery_temperature_sensor
        state: "{{ states('input_number.gear2_battery_temperature') | float(0) }}"
        unit_of_measurement: "°C"
        device_class: temperature

      - name: Gear 2 Battery Voltage
        unique_id: gear2_battery_voltage_sensor
        state: "{{ states('input_number.gear2_battery_voltage') | int(0) }}"
        unit_of_measurement: "mV"
        device_class: voltage

  - binary_sensor:
      - name: Gear 2 Charging
        unique_id: gear2_charging_binary_sensor
        state: "{{ is_state('input_boolean.gear2_battery_charging', 'on') }}"
        device_class: battery_charging
```

---

## Example Home Assistant card ideas

Show these entities on a dashboard:

```text
sensor.gear_2_steps_today
sensor.gear_2_battery
binary_sensor.gear_2_charging
sensor.gear_2_battery_temperature
sensor.gear_2_battery_voltage
input_text.gear2_last_reason
input_text.gear2_last_seen
```

Good automation ideas:

- notify when battery is below 20%
- notify when battery reaches 100% while charging
- notify if the watch has not reported for 30 minutes
- track daily steps goal
- use charging/discharging state as a tiny presence/lifestyle signal

---

## Troubleshooting

### App installs but no data arrives

Check logs:

```bat
adb -s 127.0.0.1:4444 logcat | findstr /i "Gear2StepsHA"
```

If you see:

```text
Upload OK HTTP 200
```

Home Assistant received it.

If you see:

```text
SocketTimeoutException
failed to connect
```

restart the watch or reset Bluetooth tethering.

### ADB says more than one device/emulator

Use `-s` with the exact serial:

```bat
adb devices -l
adb -s 127.0.0.1:4444 shell am broadcast -a ro.alexp.gear2steps.TEST_SERVER
```

If the watch appears as `localhost:4444`, use that instead:

```bat
adb -s localhost:4444 shell am broadcast -a ro.alexp.gear2steps.TEST_SERVER
```

### `127.0.0.1:4444` offline

Reset the bridge:

```bat
adb disconnect 127.0.0.1:4444
adb forward --remove-all
adb forward tcp:4444 localabstract:/adb-hub
adb connect 127.0.0.1:4444
```

### Windows command note

On Windows CMD, clear the screen with:

```bat
cls
```

Not:

```bat
clear
```

Ask me how I know.

---

## Why this exists

This project is a small reminder that useful hardware should not become garbage just because official support ended.

The Samsung Gear 2 has:

- a display
- battery reporting
- step counter
- heart rate hardware
- vibration
- Bluetooth
- infrared
- audio
- enough sensors to still be useful

So why throw it away?

If a device can still boot, sense, send data, and help with a smart home, then it deserves a second life (and I change the battery every ~1.5-2 years . It's like 7-8 $ )

Support **Right to Repair**.
Support community firmware.
Support people who keep old devices alive with duct tape, ADB, coffee, and questionable but functional ideas.

Long live weird hardware.

---

## License

Use it, modify it, improve it, break it, fix it again.

A permissive open-source license such as MIT is recommended if you publish this project.

---

## Credits

- Android Wear porting community
- XDA developers and modders keeping old hardware alive
- Home Assistant community
- Everyone who refuses to let repairable technology become e-waste

