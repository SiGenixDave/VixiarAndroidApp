package com.sigenix.indicor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

public class DataCollectionActivity extends AppCompatActivity
{
    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BLE = 1;

    EditText mStatusText;

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
                    mVixiarHHBLEService.writePPGDataNotification(true);
                    Log.d(TAG, "Services Discovered");
                    break;

                case VixiarHandheldBLEService.ACTION_DATA_RECEIVED:
                    // This is called after a notify or a read completes
                    // Check LED switch Setting
                    mStatusText.setText("Data Received");
                    Log.d(TAG, "Data Received");
                    /*
                    //if (mPSoCCapSenseLedService.getLedSwitchState())
                    {
                        //led_switch.setChecked(true);
                    //} else
                    {
                        //led_switch.setChecked(false);
                    }
                    // Get CapSense Slider Value
                    //String CapSensePos = mPSoCCapSenseLedService.getCapSenseValue();
                    //if (CapSensePos.equals("-1"))
                    {  // No Touch returns 0xFFFF which is -1
                        //if (!CapSenseNotifyState)
                        { // Notifications are off
                        //    mCapsenseValue.setText(R.string.NotifyOff);
                        //} else
                        { // Notifications are on but there is no finger on the slider
                        //    mCapsenseValue.setText(R.string.NoTouch);
                        }
                    //} else
                    { // Valid CapSense value is returned
                     //   mCapsenseValue.setText(CapSensePos);
                    }
                    */

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
        mStatusText = (EditText) findViewById(R.id.statusText);
        startBluetoothService();
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
        filter.addAction(VixiarHandheldBLEService.ACTION_DATA_RECEIVED);
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
