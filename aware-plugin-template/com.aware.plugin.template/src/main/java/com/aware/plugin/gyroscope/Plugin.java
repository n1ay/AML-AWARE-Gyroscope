package com.aware.plugin.gyroscope;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Gyroscope;
import com.aware.Screen;
import com.aware.providers.Gyroscope_Provider;
import com.aware.utils.Aware_Plugin;

public class Plugin extends Aware_Plugin {

    @Override
    public void onCreate() {
        super.onCreate();

        //This allows plugin data to be synced on demand from broadcast Aware#ACTION_AWARE_SYNC_DATA
        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::"+getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Allow callback to other applications when data is stored in provider
     */
    private static AWARESensorObserver awareSensor;
    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }
    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    public interface AWARESensorObserver {
        void onDataChanged(ContentValues data);
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

            Aware.startGyroscope(this);
            Gyroscope.setSensorObserver(new Gyroscope.AWARESensorObserver() {
                @Override
                public void onGyroscopeChanged(ContentValues data) {
                    data.put("label", "gyroscope");
                    sendBroadcast(new Intent("GYROSCOPE_DATA").putExtra("data", data));

                    ContentValues gyroscope_data = new ContentValues();
                    gyroscope_data.put(Provider.Table_Gyroscope_Plugin_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    gyroscope_data.put(Provider.Table_Gyroscope_Plugin_Data.TIMESTAMP, (long) data.get(Provider.Table_Gyroscope_Plugin_Data.TIMESTAMP));
                    gyroscope_data.put(Provider.Table_Gyroscope_Plugin_Data.X_ROTATION, (float) data.get(Provider.Table_Gyroscope_Plugin_Data.X_ROTATION));
                    gyroscope_data.put(Provider.Table_Gyroscope_Plugin_Data.Y_ROTATION, (float) data.get(Provider.Table_Gyroscope_Plugin_Data.Y_ROTATION));
                    gyroscope_data.put(Provider.Table_Gyroscope_Plugin_Data.Z_ROTATION, (float) data.get(Provider.Table_Gyroscope_Plugin_Data.Z_ROTATION));

                    getContentResolver().insert(Provider.Table_Gyroscope_Plugin_Data.CONTENT_URI, gyroscope_data);
                    Log.d("Gyroscope Plugin[DEBUG]", "Gyroscope: " + data);
                }
            });

            Aware.startScreen(this);
            Screen.setSensorObserver(new Screen.AWARESensorObserver() {
                @Override
                public void onScreenOn() {

                }

                @Override
                public void onScreenOff() {

                }

                @Override
                public void onScreenLocked() {

                }

                @Override
                public void onScreenUnlocked() {

                }
            });

            //Enable our plugin's sync-adapter to upload the data to the server if part of a study
            if (Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE).length() >= 0 && !Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
                ContentResolver.addPeriodicSync(
                        Aware.getAWAREAccount(this),
                        Provider.getAuthority(this),
                        Bundle.EMPTY,
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
                );
            }

            //Initialise AWARE instance in plugin
            Aware.startAWARE(this);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Turn off the sync-adapter if part of a study
        if (Aware.isStudy(this) && (getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone))) {
            ContentResolver.removePeriodicSync(
                    Aware.getAWAREAccount(this),
                    Provider.getAuthority(this),
                    Bundle.EMPTY
            );
        }

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}
