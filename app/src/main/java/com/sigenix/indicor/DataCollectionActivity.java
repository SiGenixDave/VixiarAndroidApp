package com.sigenix.indicor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class DataCollectionActivity extends Activity
{
    // TAG is used for informational messages
    private final static String TAG = PatInfoActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BLE = 1;
    private static LineGraphSeries mPPGSeries;
    private static LineGraphSeries mPressureSeries;
    private static Double PPGLastX = 0.0;
    private static Double PressureLastX = 0.0;

    TextView mStatusText;

    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static VixiarHandheldBLEService mVixiarHHBLEService;
    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {

        /**
         * This is called when the VixiarHandheldBLEService is connected
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            Log.i(TAG, "onServiceConnected");
            mVixiarHHBLEService = ((VixiarHandheldBLEService.LocalBinder) service).getService();
            mServiceConnected = true;
            mVixiarHHBLEService.initialize();
            mStatusText.setText("Scanning");
            mVixiarHHBLEService.scan();
        }

        /**
         * This is called when the VixiarHandheldBLEService is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Log.i(TAG, "onServiceDisconnected");
            mVixiarHHBLEService = null;
        }
    };
    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            switch (action)
            {
                case VixiarHandheldBLEService.ACTION_BLESCAN_CALLBACK:
                    Log.i(TAG, "BLE Scan Callback");
                    mStatusText.setText("Found device");
                    mVixiarHHBLEService.connect();
                    break;

                case VixiarHandheldBLEService.ACTION_CONNECTED:
                    /* This if statement is needed because we sometimes get a GATT_CONNECTED */
                    /* action when sending Capsense notifications */
                    mStatusText.setText("Connected");
                    mVixiarHHBLEService.discoverServices();
                    break;

                case VixiarHandheldBLEService.ACTION_DISCONNECTED:
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    break;

                case VixiarHandheldBLEService.ACTION_SERVICES_DISCOVERED:
                    mStatusText.setText("Services Discovered");
                    mVixiarHHBLEService.writeRTDataNotification(true);
                    Log.d(TAG, "Services Discovered");
                    break;

                case VixiarHandheldBLEService.ACTION_RTDATA_RECEIVED:
                    // This is called after a notify or a read completes
                    mStatusText.setText("Receiving Data");
                    Log.d(TAG, "Receiving Data");
                    ArrayList<Integer> al = mVixiarHHBLEService.GetPressureData();
                    for (int i = 0; i < al.size(); i++)
                    {
                        mPressureSeries.appendData(new DataPoint(PressureLastX, al.get(i)), true, 500);
                        PressureLastX += 0.02;
                    }

                    al = mVixiarHHBLEService.GetPPGData();
                    for (int i = 0; i < al.size(); i++)
                    {
                        mPPGSeries.appendData(new DataPoint(PPGLastX, al.get(i)), true, 500);
                        PPGLastX += 0.02;
                    }
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_data_collection);
        mStatusText = (TextView) findViewById(R.id.statusText);
        startBluetoothService();

        GraphView PPGgraph = (GraphView) findViewById(R.id.PPGgraph);
        GraphView pressureGraph = (GraphView) findViewById(R.id.pressuregraph);

        mPPGSeries = new LineGraphSeries<>();
        PPGgraph.addSeries(mPPGSeries);
        PPGgraph.getViewport().setXAxisBoundsManual(true);
        PPGgraph.getViewport().setMinX(0);
        PPGgraph.getViewport().setMaxX(10);
        mPPGSeries.setColor(Color.BLACK);
        mPPGSeries.setThickness(2);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitle("PPG");
        //PPGgraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        PPGgraph.setTitleTextSize(45f);
        PPGgraph.getViewport().setDrawBorder(true);

        mPressureSeries = new LineGraphSeries<>();
        pressureGraph.addSeries(mPressureSeries);
        pressureGraph.getViewport().setXAxisBoundsManual(true);
        pressureGraph.getViewport().setMinX(0);
        pressureGraph.getViewport().setMaxX(10);
        mPressureSeries.setColor(Color.BLACK);
        mPressureSeries.setThickness(2);
        pressureGraph.getGridLabelRenderer().setVerticalAxisTitle("Pressure");
        pressureGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        pressureGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        pressureGraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        pressureGraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        pressureGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        pressureGraph.setTitleTextSize(45f);
        pressureGraph.getViewport().setDrawBorder(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerIntents();
        startBluetoothService();
    }

    private void registerIntents()
    {
        // Register the broadcast receiver. This specified the messages the main activity looks for from the PSoCCapSenseLedService
        final IntentFilter filter = new IntentFilter();
        filter.addAction(VixiarHandheldBLEService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(VixiarHandheldBLEService.ACTION_CONNECTED);
        filter.addAction(VixiarHandheldBLEService.ACTION_DISCONNECTED);
        filter.addAction(VixiarHandheldBLEService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(VixiarHandheldBLEService.ACTION_RTDATA_RECEIVED);
        registerReceiver(mBleUpdateReceiver, filter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // UnRegister the broadcast receiver.
        unregisterIntents();
        stopBluetoothService();
    }

    private void startBluetoothService()
    {
        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, VixiarHandheldBLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "BLE Service Started");
    }

    private void unregisterIntents()
    {
        unregisterReceiver(mBleUpdateReceiver);
    }

    private void stopBluetoothService()
    {
        unbindService(mServiceConnection);
    }
}


/*
    private final Handler mHandler = new Handler();
    private Runnable mTimer1;
    private LineGraphSeries<DataPoint> mSeries1;
    private double graph2LastXValue = 0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charting);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        mSeries1 = new LineGraphSeries<>();
        graph.addSeries(mSeries1);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);
        mSeries1.setColor(Color.BLACK);
        mSeries1.setThickness(2);
        graph.getGridLabelRenderer().setVerticalAxisTitle("PPG");
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        graph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        graph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graph.setTitleTextSize(45f);
        graph.getViewport().setDrawBorder(true);

    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer1 = new Runnable() {
            @Override
            public void run() {
                graph2LastXValue += 0.1;
                if (graph2LastXValue > 10) {
                    mSeries1.appendData(new DataPoint(graph2LastXValue, getRandom()), true, 100);
                } else {
                    mSeries1.appendData(new DataPoint(graph2LastXValue, getRandom()), false, 100);
                }
                mHandler.postDelayed(this, 100);
            }
        };
        mHandler.postDelayed(mTimer1, 100);

    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mTimer1);
        super.onPause();
    }


    double mLastRandom = 2;
    Random mRand = new Random();

    private double getRandom() {
        return mLastRandom += mRand.nextDouble() * 0.5 - 0.25;
    }
 */