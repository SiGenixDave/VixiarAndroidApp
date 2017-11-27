package com.vixiar.indicor.BLEInterface;

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

import com.vixiar.indicor.Activities.GenericTimer;
import com.vixiar.indicor.Activities.TimerCallback;
import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by gyurk on 11/6/2017.
 */

public class IndicorBLEServiceInterface implements TimerCallback
{
    // make this a singleton class
    private static IndicorBLEServiceInterface ourInstance = new IndicorBLEServiceInterface();
    public static IndicorBLEServiceInterface getInstance()
    {
        return ourInstance;
    }

    private final static String TAG = IndicorBLEServiceInterface.class.getSimpleName();

    private IndicorBLEServiceInterfaceCallbacks m_CallbackInterface;
    private MyBLEMessageReceiver m_BLEMessageReceiver;

    // Variables to manage BLE connection
    private static boolean m_ConnectState;
    private static boolean m_ServiceConnected;
    private static IndicorBLEService m_VixiarHHBLEService;
    private static boolean m_bFirstBarreryReadRequest;

    private AlertDialog m_connectionDialog;
    private Context m_Context;
    private Handler m_handler = new Handler();
    private final Runnable m_ScanTimeoutRunnable = new Runnable()
    {
        public void run()
        {
            ScanTimeout();
        }
    };
    private int m_batteryLevel;

    private ArrayList<ScanResult> m_ScanList = new ArrayList<ScanResult>()
    {
    };

    // be careful this ID doesn't overlap others
    private final static int BATTERY_READ_TIMER_ID = 3;
    private final static int BATTERY_READ_TIME_MS = 30000;
    private GenericTimer m_updateBatteryTimer = new GenericTimer(BATTERY_READ_TIMER_ID);

    private final int SCAN_TIME_MS = 5000;

    public void initialize(Context c, IndicorBLEServiceInterfaceCallbacks dataInterface)
    {
        m_Context = c;
        m_CallbackInterface = dataInterface;
    }

    // list of errors
    public static final int ERROR_NO_DEVICES_FOUND = 1;
    public static final int ERROR_NO_PAIRED_DEVICES_FOUND = 2;
    public static final int ERROR_WRITING_DESCRIPTOR = 3;

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and Initialize the service.
     */
    private final ServiceConnection m_ServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            Log.i(TAG, "onServiceConnected");
            m_VixiarHHBLEService = ((IndicorBLEService.LocalBinder) service).getService();
            m_ServiceConnected = true;
            m_VixiarHHBLEService.Initialize();

            // delete everything from the scan list
            m_ScanList.clear();

            DisplayConnectingDialog();

            // start scanning
            m_VixiarHHBLEService.ScanForIndicorHandhelds();

            // start the timer to wait for scan results
            m_handler.postDelayed(m_ScanTimeoutRunnable, SCAN_TIME_MS);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Log.i(TAG, "onServiceDisconnected");
            m_VixiarHHBLEService = null;
        }
    };

    public void ConnectToIndicor()
    {
        // Start the BLE Service
        Log.i(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(m_Context, IndicorBLEService.class);
        m_Context.bindService(gattServiceIntent, m_ServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "BLE Service Started");

        // create a receiver to receive messages from the service
        m_BLEMessageReceiver = new MyBLEMessageReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IndicorBLEService.MESSAGE_ID);
        m_Context.registerReceiver(m_BLEMessageReceiver, intentFilter);
    }

    public int GetLastReadBatteryLevel()
    {
        return m_batteryLevel;
    }

    private class MyBLEMessageReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            //Log.i(TAG, "onReceive");
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
                // TODO: Need to implement some sort of timeout here that would be able to detect the loss of connection to the handheld faster than the BLE timeout of 20 seconds

                PatientInfo.getInstance().GetRealtimeData().AppendNewSample(arg1.getByteArrayExtra(IndicorBLEService.RT_DATA_RECEIVED));
                m_CallbackInterface.iNotify();

                // if this is the first notification received, we need to read the battery level
                // it has to be done this way because you can't chain gatt reads or writes...you have to
                // wait till one finishes to start another one
                if (m_bFirstBarreryReadRequest)
                {
                    m_bFirstBarreryReadRequest = false;
                    m_batteryLevel = -1;
                    m_VixiarHHBLEService.ReadBatteryLevel();
                    // start the timer to update the battery level
                    m_updateBatteryTimer.Start(IndicorBLEServiceInterface.getInstance(), BATTERY_READ_TIME_MS, false);
                }
            }
            else if (arg1.hasExtra(IndicorBLEService.ERROR_WRITING_DESCRIPTOR))
            {
                //TODO: Need to switch this to use the custom dialog
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(m_Context);
                alertDialogBuilder.setMessage("A handheld device was detected, however it is not paired with this tablet.  See Instructions for Use for how to pair the handheld with this device.");
                alertDialogBuilder.setTitle("No handheld paired");
                alertDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1)
                            {
                                m_connectionDialog.cancel();
                                // notify the activity of the problem
                                m_CallbackInterface.iError(ERROR_WRITING_DESCRIPTOR);
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
            else if (arg1.hasExtra(IndicorBLEService.BATTERY_LEVEL_RECEIVED))
            {
                m_batteryLevel = arg1.getIntExtra(IndicorBLEService.BATTERY_LEVEL_RECEIVED, 0);
                m_CallbackInterface.iBatteryLevelRead(m_batteryLevel);
            }
        }
    }

    private void BLEScanCallback(ScanResult result)
    {
        //Log.i("IND", "Got a callback");
        m_ScanList.add(result);
    }

    private void BLEConnected()
    {
        Log.i("IND", "iConnected");
        m_VixiarHHBLEService.DiscoverIndicorServices();
        m_CallbackInterface.iConnected();
    }

    private void BLEDisconnected()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(m_Context);
        alertDialogBuilder.setMessage("The handheld device has become disconnected.");
        alertDialogBuilder.setTitle("Disconnected");
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        m_connectionDialog.cancel();
                        // notify the activity of the problem
                        m_CallbackInterface.iDisconnected();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void BLEServicesDiscovered()
    {
        if (m_connectionDialog != null)
        {
            m_connectionDialog.cancel();
        }

        // once the services are discovered, turn on the real time data notification
        // in the notification callback, we'll start reading the battery level and other
        // characteristics
        m_VixiarHHBLEService.WriteRTDataNotification(true);
        m_bFirstBarreryReadRequest = true;
    }

    @Override
    public void TimerExpired(int id)
    {
        if (id == BATTERY_READ_TIMER_ID)
        {
            m_VixiarHHBLEService.ReadBatteryLevel();
        }
    }

    private void DisplayConnectingDialog()
    {
        LayoutInflater inflater = (LayoutInflater) m_Context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertLayout = inflater.inflate(R.layout.connection_dialog, null);

        AlertDialog.Builder alert = new AlertDialog.Builder(m_Context);
        alert.setTitle("Connecting");
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);
        m_connectionDialog = alert.create();
        m_connectionDialog.show();
    }

    private void ScanTimeout()
    {
        m_VixiarHHBLEService.StopScanning();

        // TODO: add logic to figure out which device to connect to based on which one is paired, strongest signal, etc.
        BluetoothDevice device = GetLargestSignalDevice();

        // see if there are any devices
        if (device == null)
        {
            // TODO: this is temporary...need to use the custom dialog class
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(m_Context);
            alertDialogBuilder.setMessage("No Indicor handhelds were detected");
            alertDialogBuilder.setTitle("No handheld found");
            alertDialogBuilder.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1)
                        {
                            m_connectionDialog.cancel();
                            // notify the activity of the problem
                            m_CallbackInterface.iError(ERROR_NO_DEVICES_FOUND);
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
                m_VixiarHHBLEService.ConnectToSpecificIndicor(device);
            } else
            {
                // TODO: this is temporary...need to use the custom dialog class
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(m_Context);
                alertDialogBuilder.setMessage("A handheld device was detected, however it is not paired with this tablet.  See Instructions for Use for how to pair the handheld with this device.");
                alertDialogBuilder.setTitle("No handheld paired");
                alertDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1)
                            {
                                m_connectionDialog.cancel();
                                // notify the activity of the problem
                                m_CallbackInterface.iError(ERROR_NO_PAIRED_DEVICES_FOUND);
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
        for (ScanResult b : m_ScanList)
        {
            String deviceAddress = b.getDevice().getAddress();
            if (!uniqueDevices.containsKey(deviceAddress))
            {
                uniqueDevices.put(deviceAddress, b.getDevice());
                uniqueDeviceParams.put(deviceAddress, new VixiarDeviceParams());
            }
        }

        // now parse all of the scans and accumulate the RSSI for each device
        for (ScanResult b : m_ScanList)
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
