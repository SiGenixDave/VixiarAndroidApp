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
import com.vixiar.indicor.CustomDialog.CustomAlertDialog;
import com.vixiar.indicor.CustomDialog.CustomDialogInterface;
import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by gyurk on 11/6/2017.
 */

public class IndicorBLEServiceInterface implements TimerCallback, CustomDialogInterface
{
    // TODO: (1) Implement a state machine to do all the connection stuff, read initial values, and start notifications

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
    private static boolean m_bFirstBatteryReadRequest;

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

    // dialog ids handled here
    private final int DLG_ID_AUTHENTICATION_ERROR = 0;
    private final int DLG_ID_NO_PAIRED_DEVICE = 1;
    private final int DLG_ID_NO_HANDHELDS = 2;

    public void initialize(Context c, IndicorBLEServiceInterfaceCallbacks dataInterface)
    {
        m_Context = c;
        m_CallbackInterface = dataInterface;
    }

    // list of errors
    public static final int ERROR_NO_DEVICES_FOUND = 1;
    public static final int ERROR_NO_PAIRED_DEVICES_FOUND = 2;
    public static final int AUTHENTICATION_ERROR = 3;

    // Offsets to data in characteristics
    private final static int BATTERY_LEVEL_PCT_INDEX = 0;
    private final static int BATTERY_LEVEL_MV_INDEX = 1;

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and Initialize the service.
     */
    private final ServiceConnection m_ServiceConnection = new ServiceConnection()
    {
        @Override
        public void onBindingDied(ComponentName name)
        {
            Log.i(TAG, "onBindingDied");
        }

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
            else if (arg1.hasExtra(IndicorBLEService.REVISION_INFO_RECEIVED))
            {

            }
            else if (arg1.hasExtra(IndicorBLEService.EXTERNAL_CONNECTION_INFO_RECEIVED))
            {

            }
            if (arg1.hasExtra(IndicorBLEService.RT_DATA_RECEIVED))
            {
                // TODO: Need to implement some sort of timeout here that would be able to detect the loss of connection to the handheld faster than the BLE timeout of 20 seconds

                PatientInfo.getInstance().getRealtimeData().AppendNewSample(arg1.getByteArrayExtra(IndicorBLEService.RT_DATA_RECEIVED));
                m_CallbackInterface.iRealtimeDataNotification();

                // if this is the first notification received, we need to read the battery level
                // it has to be done this way because you can't chain gatt reads or writes...you have to
                // wait till one finishes to start another one
                if (m_bFirstBatteryReadRequest)
                {
                    m_bFirstBatteryReadRequest = false;
                    m_batteryLevel = -1;
                    m_VixiarHHBLEService.ReadBatteryLevel();
                    // start the timer to update the battery level
                    m_updateBatteryTimer.Start(IndicorBLEServiceInterface.getInstance(), BATTERY_READ_TIME_MS, false);
                }
            }
            else if (arg1.hasExtra(IndicorBLEService.AUTHENTICATION_ERROR))
            {
                CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                        m_Context.getString(R.string.dlg_title_authentication_error),
                        m_Context.getString(R.string.dlg_msg_authentication_error),
                        "Ok",
                        null,
                        m_Context, DLG_ID_AUTHENTICATION_ERROR, IndicorBLEServiceInterface.this);
            }
            else if (arg1.hasExtra(IndicorBLEService.BATTERY_LEVEL_RECEIVED))
            {
                byte x[] = arg1.getByteArrayExtra(IndicorBLEService.BATTERY_LEVEL_RECEIVED);
                //m_batteryLevel x[]
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
        Log.i(TAG, "iConnected");
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
        //m_VixiarHHBLEService.SubscribeToRealtimeDataNotification(true);
        m_VixiarHHBLEService.ReadBatteryLevel();
        m_bFirstBatteryReadRequest = true;
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
            CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                    m_Context.getString(R.string.dlg_title_no_handhelds),
                    m_Context.getString(R.string.dlg_msg_no_handhelds),
                    "Ok",
                    null,
                    m_Context, DLG_ID_NO_HANDHELDS, IndicorBLEServiceInterface.this);
        }
        else
        {
            // see if this device is paired
            if (device.getBondState() == BluetoothDevice.BOND_BONDED)
            {
                // if it's bonded, connect to it
                m_VixiarHHBLEService.ConnectToSpecificIndicor(device);
            }
            else
            {
                CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                        m_Context.getString(R.string.dlg_title_no_paired_devices),
                        m_Context.getString(R.string.dlg_msg_no_paired_devices),
                        "Ok",
                        null,
                        m_Context, DLG_ID_NO_PAIRED_DEVICE, this);
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

    @Override
    public void onClickPositiveButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
            case DLG_ID_AUTHENTICATION_ERROR:
                m_connectionDialog.cancel();
                m_CallbackInterface.iError(AUTHENTICATION_ERROR);
                break;

            case DLG_ID_NO_PAIRED_DEVICE:
                m_connectionDialog.cancel();
                m_CallbackInterface.iError(ERROR_NO_PAIRED_DEVICES_FOUND);
                break;

            case DLG_ID_NO_HANDHELDS:
                m_connectionDialog.cancel();
                m_CallbackInterface.iError(ERROR_NO_DEVICES_FOUND);
                break;
        }
    }

    @Override
    public void onClickNegativeButton(DialogInterface dialog, int dialogID)
    {

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
