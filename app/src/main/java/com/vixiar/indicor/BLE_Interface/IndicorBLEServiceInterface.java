package com.vixiar.indicor.BLE_Interface;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.Data.RealtimeData;
import com.vixiar.indicor.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by gyurk on 11/6/2017.
 */

public class IndicorBLEServiceInterface
{
    // make this a singleton class
    private static IndicorBLEServiceInterface ourInstance = new IndicorBLEServiceInterface();

    public static IndicorBLEServiceInterface getInstance()
    {
        return ourInstance;
    }

    private final static String TAG = "IND";
    private IndicorBLEServiceInterfaceCallbacks mCallbackInterface;
    private MyBLEMessageReceiver myBLEMessageReceiver;

    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static IndicorBLEService mVixiarHHBLEService;

    private AlertDialog connectionDialog;
    private Context mContext;

    private Handler handler = new Handler();
    private final Runnable runnable = new Runnable()
    {
        public void run()
        {
            ScanTimeout();
        }
    };

    private ArrayList<ScanResult> mScanList = new ArrayList<ScanResult>()
    {
    };

    private final int SCAN_TIME_MS = 5000;

    public void initialize(Context c, IndicorBLEServiceInterfaceCallbacks dataInterface)
    {
        mContext = c;
        mCallbackInterface = dataInterface;
    }

    // list of errors
    public static final int ERROR_NO_DEVICES_FOUND = 1;
    public static final int ERROR_NO_PAIRED_DEVICES_FOUND = 2;
    public static final int ERROR_WRITING_DESCRIPTOR = 3;

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
            mVixiarHHBLEService = ((IndicorBLEService.LocalBinder) service).getService();
            mServiceConnected = true;
            mVixiarHHBLEService.Initialize();

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
        // Start the BLE Service
        Log.i(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(mContext, IndicorBLEService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "BLE Service Started");

        // create a receiver to receive messages from the service
        myBLEMessageReceiver = new MyBLEMessageReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IndicorBLEService.MESSAGE_ID);
        mContext.registerReceiver(myBLEMessageReceiver, intentFilter);
    }

    private class MyBLEMessageReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            Log.i(TAG, "onReceive");
            if (arg1.hasExtra(IndicorBLEService.SCAN_RESULT))
            {
                BLEScanCallback((ScanResult) arg1.getParcelableExtra(IndicorBLEService.SCAN_RESULT));
            }
            else if (arg1.hasExtra(IndicorBLEService.CONNECTED))
            {
                BLEConnected();
            }
            else if (arg1.hasExtra(IndicorBLEService.DISCONNECTED))
            {
                BLEDisconnected();
            }
            else if (arg1.hasExtra(IndicorBLEService.SERVICES_DISCOVERED))
            {
                BLEServicesDiscovered();
            }
            else if (arg1.hasExtra(IndicorBLEService.RT_DATA_RECEIVED))
            {
                PatientInfo.getInstance().GetRealtimeData().AppendNewSample(arg1.getByteArrayExtra(IndicorBLEService.RT_DATA_RECEIVED));
                mCallbackInterface.iNotify();
            }
            else if (arg1.hasExtra(IndicorBLEService.ERROR_WRITING_DESCRIPTOR))
            {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                alertDialogBuilder.setMessage("A handheld device was detected, however it is not paired with this tablet.  See Instructions for Use for how to pair the handheld with this device.");
                alertDialogBuilder.setTitle("No handheld paired");
                alertDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1)
                            {
                                connectionDialog.cancel();
                                // notify the activity of the problem
                                mCallbackInterface.iError(ERROR_WRITING_DESCRIPTOR);
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    }

    public void BLEScanCallback(ScanResult result)
    {
        Log.i("IND", "Got a callback");
        mScanList.add(result);
    }

    public void BLEConnected()
    {
        Log.i("IND", "iConnected");
        mVixiarHHBLEService.DiscoverIndicorServices();
        mCallbackInterface.iConnected();
    }

    public void BLEDisconnected()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setMessage("The handheld device has become disconnected.");
        alertDialogBuilder.setTitle("Disconnected");
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        connectionDialog.cancel();
                        // notify the activity of the problem
                        mCallbackInterface.iDisconnected();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void BLEServicesDiscovered()
    {
        if (connectionDialog != null)
        {
            connectionDialog.cancel();
        }

        // once the services are discovered, turn on the realtime data notification
        mVixiarHHBLEService.WriteRTDataNotification(true);
    }

    private void DisplayConnectingDialog()
    {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertLayout = inflater.inflate(R.layout.connection_dialog, null);

        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle("Connecting");
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);
        connectionDialog = alert.create();
        connectionDialog.show();
    }

    private void ScanTimeout()
    {
        mVixiarHHBLEService.StopScanning();

        // TODO: add logic to figure out which device to connect to based on which one is paired, strongest signal, etc.
        BluetoothDevice device = GetLargestSignalDevice();

        // see if there are any devices
        if (device == null)
        {
            // TODO: this is temporary...need to finish this
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
            alertDialogBuilder.setMessage("No Indicor handhelds were detected");
            alertDialogBuilder.setTitle("No handheld found");
            alertDialogBuilder.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1)
                        {
                            connectionDialog.cancel();
                            // notify the activity of the problem
                            mCallbackInterface.iError(ERROR_NO_DEVICES_FOUND);
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else
        {
            // see if this device is paired
            if (device.getBondState() == BluetoothDevice.BOND_BONDED)
            {
                // if it's bonded, connect to it
                mVixiarHHBLEService.ConnectToSpecificIndicor(device);
            } else
            {
                // TODO: this is temporary...need to finish this
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                alertDialogBuilder.setMessage("A handheld device was detected, however it is not paired with this tablet.  See Instructions for Use for how to pair the handheld with this device.");
                alertDialogBuilder.setTitle("No handheld paired");
                alertDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1)
                            {
                                connectionDialog.cancel();
                                // notify the activity of the problem
                                mCallbackInterface.iError(ERROR_NO_PAIRED_DEVICES_FOUND);
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    }

    private BluetoothDevice GetLargestSignalDevice()
    {

        Map<String, BluetoothDevice> uniqueDevices = new HashMap<>();
        Map<String, VixiarDeviceParams> uniqueDeviceParams = new HashMap<>();

        // Create a map of all of the unique device Ids detected while receiving advertising
        // packets
        for (ScanResult b : mScanList)
        {
            String deviceAddress = b.getDevice().getAddress();
            if (!uniqueDevices.containsKey(deviceAddress))
            {
                uniqueDevices.put(deviceAddress, b.getDevice());
                uniqueDeviceParams.put(deviceAddress, new VixiarDeviceParams());
            }
        }

        // now parse all of the scans and accumulate the RSSI for each device
        for (ScanResult b : mScanList)
        {
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
        for (String key : uniqueDeviceParams.keySet())
        {
            VixiarDeviceParams v = uniqueDeviceParams.get(key);
            int rssiAvg = v.Average();
            if (rssiAvg >= maxRSSIAvg)
            {
                devIdMaxId = key;
                maxRSSIAvg = rssiAvg;
            }
        }

        if (devIdMaxId != "")
        {
            return uniqueDevices.get(devIdMaxId);
        }

        return null;
    }

    private class VixiarDeviceParams
    {

        private int mTotalRssi;
        private int mNumAdvertisements;

        public VixiarDeviceParams()
        {
            mTotalRssi = 0;
            mNumAdvertisements = 0;
        }

        public void Accumulate(int rssi)
        {
            mTotalRssi += rssi;
            mNumAdvertisements++;
        }

        public int Average()
        {
            return mTotalRssi / mNumAdvertisements;
        }
    }
}
