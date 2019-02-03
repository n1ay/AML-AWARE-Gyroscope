package com.aware.plugin.gyroscope;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aware.providers.Gyroscope_Provider;
import com.aware.utils.IContextCard;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ContextCard implements IContextCard {

    private static final int MAX_RECORDS = 5 * 3600;
    private static final double EPSILON = 0.05;

    //Constructor used to instantiate this card
    public ContextCard() {
    }

    private TextView awareStreamTextView = null;
    private GraphView graph = null;

    @Override
    public View getContextCard(Context context) {
        //Load card layout
        View card = LayoutInflater.from(context).inflate(R.layout.card, null);
        awareStreamTextView = card.findViewById(R.id.aware_stream_text_view);

        //Register the broadcast receiver that will update the UI from the background service (Plugin)
        IntentFilter gyroscopeDataFilter = new IntentFilter("GYROSCOPE_DATA");
        IntentFilter updateGraphFilter = new IntentFilter("UPDATE_GRAPH");
        context.registerReceiver(gyroscopeObserver, gyroscopeDataFilter);
        context.registerReceiver(graphObserver, updateGraphFilter);

        graph = card.findViewById(R.id.graph);
        initializeGraph(graph, context);

        //Return the card to AWARE/apps
        return card;
    }

    private void initializeGraph(GraphView graph, final Context context) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(numberFormat, numberFormat));

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setHorizontalLabels(new String[] {"now", "20 min ago", "40 min ago", "1 h ago"});
        staticLabelsFormatter.setVerticalLabels(new String[] {"zero", "low", "med", "high"});
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
        graph.setTitle("Gyroscope activity in the last hour");
        graph.setTitleTextSize(64);
    }

    //This broadcast receiver is auto-unregistered because it's not static.
    private GyroscopeObserver gyroscopeObserver = new GyroscopeObserver();
    private GraphObserver graphObserver = new GraphObserver();

    public class GyroscopeObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //do nothing
        }
    }

    public class GraphObserver extends BroadcastReceiver {

        public void updateView(Context context) {
            Cursor gyroscope_data = context.getContentResolver().query(Provider.Table_Gyroscope_Plugin_Data.CONTENT_URI, null, null, null, "timestamp DESC");
            LinkedList<DataPoint> dataPointsList = new LinkedList<>();

            gyroscope_data.moveToFirst();
            long now = new Date().getTime();
            long recentActivityTimestamp = 0;
            int index = 0;
            String message = "You were not active in the last hour.";
            do {
                long timestamp = gyroscope_data.getLong(gyroscope_data.getColumnIndex(Gyroscope_Provider.Gyroscope_Data.TIMESTAMP));
                double x = gyroscope_data.getDouble(gyroscope_data.getColumnIndex(Gyroscope_Provider.Gyroscope_Data.VALUES_0));
                double y = gyroscope_data.getDouble(gyroscope_data.getColumnIndex(Gyroscope_Provider.Gyroscope_Data.VALUES_1));
                double z = gyroscope_data.getDouble(gyroscope_data.getColumnIndex(Gyroscope_Provider.Gyroscope_Data.VALUES_2));
                double speed = Math.sqrt(x * x + y * y + z * z);
                if (speed < EPSILON) {
                    speed = 0;
                } else if (recentActivityTimestamp == 0) {
                    recentActivityTimestamp = timestamp;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(now - recentActivityTimestamp);
                    if (minutes == 0) {
                        message = "You were active few seconds ago.";
                    } else if (minutes > 0) {
                        message = "You were active " + minutes + " minute(s) ago.";
                    }
                    awareStreamTextView.setText(message);
                }

                dataPointsList.addLast(new DataPoint(index, speed));
                index++;
            } while (gyroscope_data.moveToNext() && index < MAX_RECORDS);

            while (index < MAX_RECORDS) {
                dataPointsList.addLast(new DataPoint(index, 0));
                index++;
            }

            DataPoint[] dataPointsArray = new DataPoint[dataPointsList.size()];
            for (int i = 0; i < dataPointsList.size(); i++) {
                    dataPointsArray[i] = dataPointsList.get(i);
                }

            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPointsArray);
            graph.removeAllSeries();
            graph.addSeries(series);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            updateView(context);
        }
    }
}
