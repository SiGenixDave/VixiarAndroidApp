package com.sigenix.indicor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class DataCollectionActivity extends AppCompatActivity
{
    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BLE = 1;

    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static VixiarHandheldBLEService mVixiarHHBLEService;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);
        startBluetooth();
    }

    public void startBluetooth()
    {
        // Find BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, VixiarHandheldBLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        Log.d(TAG, "Bluetooth is Enabled");
    }

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the VixiarHandheldBLEService is connected
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mVixiarHHBLEService = ((VixiarHandheldBLEService.LocalBinder) service).getService();
            mServiceConnected = true;
            mVixiarHHBLEService.initialize();
        }

        /**
         * This is called when the VixiarHandheldBLEService is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mVixiarHHBLEService = null;
        }
    };


}
