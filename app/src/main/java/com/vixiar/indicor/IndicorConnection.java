package com.vixiar.indicor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by gyurk on 11/6/2017.
 */

public class IndicorConnection implements IndicorBLEServiceInterface
{
    // make this a singleton class
    private static IndicorConnection ourInstance = new IndicorConnection();

    public static IndicorConnection getInstance()
    {
        return ourInstance;
    }

    private final static String TAG = "IND";

    private IndicorDataInterface mCallbackInterface;

    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static VixiarHandheldBLEService mVixiarHHBLEService;

    private AlertDialog dialog;
    private Context mContext;

    Handler handler = new Handler();
    final Runnable runnable = new Runnable()
    {
        public void run()
        {
            ScanTimeout();
        }
    };

    private ArrayList<ScanResult> mScanList = new ArrayList<ScanResult>(){};

    private final int SCAN_TIME_MS = 5000;

    public void initialize(Context c, IndicorDataInterface dataInterface)
    {
        mContext = c;
        mCallbackInterface = dataInterface;
    }

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and Initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            Log.i(TAG, "onServiceConnected");
            mVixiarHHBLEService = ((VixiarHandheldBLEService.LocalBinder) service).getService();
            mServiceConnected = true;
            mVixiarHHBLEService.Initialize(IndicorConnection.this);

            // delete everythign from the scan list
            mScanList.clear();

            DisplayConnectingDialog();

            // start scanning
            mVixiarHHBLEService.ScanForIndicorHandhelds();

            // start the timer to wait for scan results
            handler.postDelayed(runnable, SCAN_TIME_MS);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Log.i(TAG, "onServiceDisconnected");
            mVixiarHHBLEService = null;
        }
    };

    public void ConnectToIndicor()
    {
        StartHandheldBLEService();
    }

    private void StartHandheldBLEService()
    {
        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(mContext, VixiarHandheldBLEService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "BLE Service Started");
    }

    public void iBLEScanCallback(ScanResult result)
    {
        Log.d("IND", "Got a callback");
        mScanList.add(result);
    }

    public void iBLEConnected()
    {
        Log.d("IND", "iConnected");
        mVixiarHHBLEService.DiscoverIndicorServices();
        mCallbackInterface.iConnected();
    }

    public void iBLEDisconnected()
    {

    }

    public void iBLEServicesDiscovered()
    {
        dialog.cancel();
        mVixiarHHBLEService.WriteRTDataNotification(true);
    }

    public void iBLEDataReceived(byte[] data)
    {
        if (mCallbackInterface != null)
        {
            mCallbackInterface.iNotify(data);
        }
    }

    private void DisplayConnectingDialog()
    {
        LayoutInflater inflater = new LayoutInflater(mContext)
        {
            @Override
            public LayoutInflater cloneInContext(Context context)
            {
                return null;
            }
        };
        View alertLayout = inflater.inflate(R.layout.connection_dialog, null);

        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle("Connecting");
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);
        dialog = alert.create();
        dialog.show();
    }

    private void ScanTimeout()
    {
        mVixiarHHBLEService.StopScanning();
        BluetoothDevice device = GetLargestSignalDevice();
        mVixiarHHBLEService.ConnectToSpecificIndicor(device);
    }

    private BluetoothDevice GetLargestSignalDevice() {

        Map<String, BluetoothDevice> uniqueDevices = new HashMap<>();
        Map<String, VixiarDeviceParams> uniqueDeviceParams = new HashMap<>();

        // Create a map of all of the unique device Ids detected while receiving advertising
        // packets
        for (ScanResult b: mScanList) {
            String deviceAddress = b.getDevice().getAddress();
            if (!uniqueDevices.containsKey(deviceAddress)) {
                uniqueDevices.put(deviceAddress, b.getDevice());
                uniqueDeviceParams.put(deviceAddress, new VixiarDeviceParams());
            }
        }

        // now parse all of the scans and accumulate the RSSI for each device
        for (ScanResult b: mScanList) {
            // get the address
            String deviceAddress = b.getDevice().getAddress();
            // get the object
            VixiarDeviceParams v = uniqueDeviceParams.get(deviceAddress);
            // accumulate the RSSI for that particular device
            v.Accumulate(b.getRssi());
        }

        // Determine who has the largest average RSSI
        int maxRSSIAvg = Integer.MIN_VALUE;
        String devIdMaxId = "";
        for(String key: uniqueDeviceParams.keySet()) {
            VixiarDeviceParams v = uniqueDeviceParams.get(key);
            int rssiAvg = v.Average();
            if (rssiAvg  >= maxRSSIAvg) {
                devIdMaxId = key;
                maxRSSIAvg = rssiAvg;
            }
        }

        if (devIdMaxId != "") {
            return uniqueDevices.get(devIdMaxId);
        }

        return null;
    }


    private class VixiarDeviceParams {

        private int mTotalRssi;
        private int mNumAdvertisements;

        public VixiarDeviceParams() {
            mTotalRssi = 0;
            mNumAdvertisements = 0;
        }

        public void Accumulate(int rssi) {
            mTotalRssi += rssi;
            mNumAdvertisements++;
        }

        public int Average() {
            return mTotalRssi/mNumAdvertisements;
        }
    }
}
