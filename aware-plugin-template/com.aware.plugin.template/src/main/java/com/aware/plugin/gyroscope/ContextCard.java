package com.aware.plugin.gyroscope;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aware.utils.IContextCard;

public class ContextCard implements IContextCard {

    //Constructor used to instantiate this card
    public ContextCard() {
    }

    private TextView awareStreamTextView = null;

    @Override
    public View getContextCard(Context context) {
        //Load card layout
        View card = LayoutInflater.from(context).inflate(R.layout.card, null);
        awareStreamTextView = card.findViewById(R.id.aware_stream_text_view);

        //Register the broadcast receiver that will update the UI from the background service (Plugin)
        IntentFilter filter = new IntentFilter("GYROSCOPE_DATA");
        context.registerReceiver(gyroscopeObserver, filter);

        //Return the card to AWARE/apps
        return card;
    }

    //This broadcast receiver is auto-unregistered because it's not static.
    private GyroscopeObserver gyroscopeObserver = new GyroscopeObserver();
    public class GyroscopeObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase("GYROSCOPE_DATA")) {
                ContentValues data = intent.getParcelableExtra("data");
                String text = "label=" + data.get("label") +
                        "\ndevice_id=" + data.get("device_id") +
                        "\ntimestamp=" + data.get("timestamp") +
                        "\ndouble_values_0 (X_Rot)=" + data.get("double_values_0") +
                        "\ndouble_values_1 (Y_Rot)=" + data.get("double_values_1") +
                        "\ndouble_values_2 (Z_Rot)=" + data.get("double_values_2");
                awareStreamTextView.setText(text);
            }
        }
    }
}
