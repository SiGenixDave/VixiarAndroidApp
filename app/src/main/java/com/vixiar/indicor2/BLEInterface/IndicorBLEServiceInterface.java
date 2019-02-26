package com.vixiar.indicor2.BLEInterface;

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

import com.vixiar.indicor2.Activities.GenericTimer;
import com.vixiar.indicor2.Activities.TimerCallback;
import com.vixiar.indicor2.Application.NavigatorApplication;
import com.vixiar.indicor2.CustomDialog.CustomAlertDialog;
import com.vixiar.indicor2.CustomDialog.CustomDialogInterface;
import com.vixiar.indicor2.Data.PatientInfo;
import com.vixiar.indicor2.Data.RealtimeDataMarker;
import com.vixiar.indicor2.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by gyurk on 11/6/2017.
 */

public class IndicorBLEServiceInterface implements TimerCallback, CustomDialogInterface
{
    // make this a singleton class
    private static final IndicorBLEServiceInterface ourInstance = new IndicorBLEServiceInterface();

    public static IndicorBLEServiceInterface getInstance()
    {
        return ourInstance;
    }

    private final static String TAG = IndicorBLEServiceInterface.class.getSimpleName();

    private IndicorBLEServiceInterfaceCallbacks m_CallbackInterface;
    private MyBLEMessageReceiver m_BLEMessageReceiver;

    // Variables to manage BLE connection
    private static boolean m_bServiceConnected;
    private static IndicorBLEService m_VixiarHHBLEService;

    private AlertDialog m_connectionDialog;
    private Context m_ActivityContext;
    private final Handler m_handler = new Handler();
    private final Runnable m_ScanTimeoutRunnable = new Runnable()
    {
        public void run()
        {
            ConnectionStateMachine(Connection_Event.EVT_SCAN_TIMEOUT);
        }
    };
    private int m_batteryLevel;
    private int m_LEDLevel;

    private int m_expectedRTDataSequnceNumber;
    private boolean m_bFirstSequenceNumber;

    private String m_handheldFirmwareRevision;

    private final ArrayList<ScanResult> m_ScanList = new ArrayList<ScanResult>()
    {
    };

    // be careful this ID doesn't overlap others
    private final static int BATTERY_READ_TIMER_ID = 3;
    private final static int CONNECTION_TIMEOUT_TIMER_ID = 10;
    private final static int BATTERY_READ_TIME_MS = 30000;
    private GenericTimer m_updateBatteryTimer = new GenericTimer(BATTERY_READ_TIMER_ID);
    private GenericTimer m_connectionTimeoutTimer = new GenericTimer(CONNECTION_TIMEOUT_TIMER_ID);

    private final int SCAN_TIME_MS = 5000;
    private final int CONNECTION_TIMEOUT_MS = SCAN_TIME_MS + 10000;

    private BluetoothDevice m_connectedDevice;

    // the low battery level msg will popup if battery is at or below this percentage
    private final int BATTERY_LEVEL_LOW_FOR_TEST = 20;

    // dialog ids handled here
    private final int DLG_ID_AUTHENTICATION_ERROR = 0;
    private final int DLG_ID_NO_PAIRED_DEVICE = 1;
    private final int DLG_ID_NO_HANDHELDS = 2;
    private final int DLG_ID_CONNECTION_ERROR = 3;
    private final int DLG_ID_SEQUENCE_ERROR = 4;
    private final int DLG_ID_LOW_BATTERY = 5;

    private enum IndicorConnection_State
    {
        STATE_NOT_CONNECTED,
        STATE_SCANNING,
        STATE_WAITING_TO_CONNECT,
        STATE_SERVICES_READ,
        STATE_REQUESTED_REVISION,
        STATE_REQUESTED_BATTERY,
        STATE_REQUESTED_RT_NOTIFICATION,
        STATE_REQUESTED_CONNECTION_NOTIFICATION,
        STATE_OPERATIONAL
    }

    private IndicorConnection_State m_IndicorConnectionState;

    private enum Connection_Event
    {
        EVT_SERVICE_CONNECTED,
        EVT_SCAN_TIMEOUT,
        EVT_BLE_CONNECTED,
        EVT_SERVICES_READ,
        EVT_REVISION_READ,
        EVT_BATTERY_READ,
        EVT_NOTIFICATION_WRITTEN,
        EVT_DISCONNECTED,
        EVT_AUTHENTICATION_ERROR,
        EVT_CONNECTiON_ERROR
    }

    // list of errors
    public static final int ERROR_NO_DEVICES_FOUND = 1;
    public static final int ERROR_NO_PAIRED_DEVICES_FOUND = 2;
    public static final int ERROR_AUTHENTICATION = 3;
    public static final int ERROR_CONNECTION_ERROR = 4;
    public static final int ERROR_SEQUENCE_ERROR = 4;

    // Offsets to data in characteristics
    private final static int BATTERY_LEVEL_PCT_INDEX = 0;
    private final static int BATTERY_LEVEL_MV_INDEX = 1;
    private final static int LED_LEVEL_PCT_INDEX = 0;

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
            m_bServiceConnected = true;
            m_VixiarHHBLEService.Initialize();
            ConnectionStateMachine(Connection_Event.EVT_SERVICE_CONNECTED);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Log.i(TAG, "onServiceDisconnected");
            m_VixiarHHBLEService = null;
            m_bServiceConnected = false;
        }
    };

    public void initialize(Context c, IndicorBLEServiceInterfaceCallbacks dataInterface)
    {
        m_ActivityContext = c;
        m_CallbackInterface = dataInterface;
    }

    public void ConnectToIndicor()
    {
        if (m_bServiceConnected)
        {
            Log.i(TAG, "Service already connected");

            // if the service is still connected to the handheld, everything is fine
            if (m_VixiarHHBLEService.AmConnectedToIndicor())
            {
                Log.i(TAG, "Service thinks indicor is conneted");
                m_CallbackInterface.iFullyConnected();
            }
            // otherwise, start the connection process
            else
            {
                Log.i(TAG, "Service thinks indicor is not connected");
                m_IndicorConnectionState = IndicorConnection_State.STATE_NOT_CONNECTED;
                ConnectionStateMachine(Connection_Event.EVT_SERVICE_CONNECTED);
            }
        }
        else
        {
            Log.i(TAG, "Service is not connected");

            // Start the BLE Service
            Intent gattServiceIntent = new Intent(NavigatorApplication.getAppContext(), IndicorBLEService.class);
            NavigatorApplication.getAppContext().bindService(gattServiceIntent, m_ServiceConnection, BIND_AUTO_CREATE);

            // create a receiver to receive messages from the service
            m_BLEMessageReceiver = new MyBLEMessageReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(IndicorBLEService.MESSAGE_ID);
            NavigatorApplication.getAppContext().registerReceiver(m_BLEMessageReceiver, intentFilter);

            m_IndicorConnectionState = IndicorConnection_State.STATE_NOT_CONNECTED;
        }
    }

    public void DisconnectFromService()
    {
        m_VixiarHHBLEService = null;
        m_bServiceConnected = false;

        // unbind the service, and unregister the intent receiver
        NavigatorApplication.getAppContext().unbindService(m_ServiceConnection);
        NavigatorApplication.getAppContext().unregisterReceiver(m_BLEMessageReceiver);
        m_IndicorConnectionState = IndicorConnection_State.STATE_NOT_CONNECTED;

        // stop the battery read timer if it's running
        m_updateBatteryTimer.Cancel();
        m_connectionTimeoutTimer.Cancel();
    }

    public void DisconnectFromIndicor()
    {
        if (m_VixiarHHBLEService != null)
        {
            m_updateBatteryTimer.Cancel();
            m_connectionTimeoutTimer.Cancel();
            m_VixiarHHBLEService.DisconnectFromIndicor();
        }
    }

    public void GetLEDPWMValue()
    {
        m_VixiarHHBLEService.ReadLEDPWMLevel();
    }

    public void SetLEDPWMValue(int value)
    {
        m_VixiarHHBLEService.WriteLEDPWMLevel(value);
    }


    public int GetLastReadBatteryLevel()
    {
        return m_batteryLevel;
    }

    private void DisplayConnectingDialog()
    {
        LayoutInflater inflater = (LayoutInflater) m_ActivityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertLayout = inflater.inflate(R.layout.connection_dialog, null);

        AlertDialog.Builder alert = new AlertDialog.Builder(m_ActivityContext);
        alert.setTitle("Connecting");
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);
        m_connectionDialog = alert.create();
        m_connectionDialog.show();
    }

    private void ConnectionStateMachine(Connection_Event event)
    {
        switch (m_IndicorConnectionState)
        {
            case STATE_NOT_CONNECTED:
                if (event == Connection_Event.EVT_SERVICE_CONNECTED)
                {
                    // delete everything from the scan list
                    m_ScanList.clear();

                    DisplayConnectingDialog();

                    // start scanning
                    m_VixiarHHBLEService.ScanForIndicorHandhelds();

                    // start the timer to wait for scan results
                    m_handler.postDelayed(m_ScanTimeoutRunnable, SCAN_TIME_MS);

                    Log.i(TAG, "STATE_SCANNING");

                    m_IndicorConnectionState = IndicorConnection_State.STATE_SCANNING;

                    // start the connection timeout timer
                    m_connectionTimeoutTimer.Start(this, CONNECTION_TIMEOUT_MS, true);
                }
                break;

            case STATE_SCANNING:
                if (event == Connection_Event.EVT_SCAN_TIMEOUT)
                {
                    m_VixiarHHBLEService.StopScanning();

                    // TODO: add logic to figure out which device to connect to based on which one is paired, strongest signal, etc.
                    BluetoothDevice device = GetLargestSignalDevice();

                    // see if there are any devices
                    if (device == null)
                    {
                        m_connectionTimeoutTimer.Cancel();

                        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                                m_ActivityContext.getString(R.string.dlg_title_no_handhelds),
                                m_ActivityContext.getString(R.string.dlg_msg_no_handhelds),
                                "Try Again",
                                "End Test", m_ActivityContext,
                                DLG_ID_NO_HANDHELDS, IndicorBLEServiceInterface.this);

                    }
                    else
                    {
                        // see if this device is paired
                        if (device.getBondState() == BluetoothDevice.BOND_BONDED)
                        {
                            // if it's bonded, try to connect to it

                            // save the device we're trying to connect to
                            m_connectedDevice = device;

                            m_VixiarHHBLEService.ConnectToSpecificIndicor(device);
                            m_IndicorConnectionState = IndicorConnection_State.STATE_WAITING_TO_CONNECT;

                            Log.i(TAG, "STATE_WAITING_TO_CONNECT");
                        }
                        else
                        {
                            CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                                    m_ActivityContext.getString(R.string.dlg_title_no_paired_devices),
                                    m_ActivityContext.getString(R.string.dlg_msg_no_paired_devices),
                                    "Ok",
                                    null,
                                    m_ActivityContext, DLG_ID_NO_PAIRED_DEVICE, this);
                        }
                    }
                }
                break;

            case STATE_WAITING_TO_CONNECT:
                if (event == Connection_Event.EVT_BLE_CONNECTED)
                {
                    m_VixiarHHBLEService.DiscoverIndicorServices();
                    m_IndicorConnectionState = IndicorConnection_State.STATE_SERVICES_READ;

                    // save the device serial number in the patient info
                    PatientInfo.getInstance().set_handheldSerialNumber(m_connectedDevice.getName());

                    Log.i(TAG, "STATE_SERVICES_READ");
                }
                break;

            case STATE_SERVICES_READ:
                if (event == Connection_Event.EVT_SERVICES_READ)
                {
                    // get the battery level
                    m_VixiarHHBLEService.ReadRevisionInformation();
                    m_IndicorConnectionState = IndicorConnection_State.STATE_REQUESTED_REVISION;
                    Log.i(TAG, "STATE_REQUESTED_REVISION");
                }
                break;

            case STATE_REQUESTED_REVISION:
                if (event == Connection_Event.EVT_REVISION_READ)
                {
                    m_VixiarHHBLEService.ReadBatteryLevel();
                    m_IndicorConnectionState = IndicorConnection_State.STATE_REQUESTED_BATTERY;
                    Log.i(TAG, "STATE_REQUESTED_BATTERY");
                }
                break;

            case STATE_REQUESTED_BATTERY:
                if (event == Connection_Event.EVT_BATTERY_READ)
                {
                    // make sure the battery level is enough to start a test
                    if (GetLastReadBatteryLevel() <= BATTERY_LEVEL_LOW_FOR_TEST)
                    {
                        // stop the connection timeout timer
                        m_connectionTimeoutTimer.Cancel();

                        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                                m_ActivityContext.getString(R.string.dlg_title_low_battery),
                                m_ActivityContext.getString(R.string.dlg_msg_low_battery),
                                "Ok",
                                null,
                                m_ActivityContext, DLG_ID_LOW_BATTERY, this);
                    }
                    else
                    {
                        // start the timer to update the battery level
                        m_updateBatteryTimer.Start(IndicorBLEServiceInterface.getInstance(), BATTERY_READ_TIME_MS, false);

                        m_bFirstSequenceNumber = true;

                        m_VixiarHHBLEService.SubscribeToRealtimeDataNotification(true);
                        m_IndicorConnectionState = IndicorConnection_State.STATE_REQUESTED_RT_NOTIFICATION;
                        Log.i(TAG, "STATE_REQUESTED_RT_NOTIFICATION");
                    }
                }
                break;

            case STATE_REQUESTED_RT_NOTIFICATION:
                if (event == Connection_Event.EVT_NOTIFICATION_WRITTEN)
                {
                    m_VixiarHHBLEService.SubscribeToConnectionNotification(true);
                    m_IndicorConnectionState = IndicorConnection_State.STATE_REQUESTED_CONNECTION_NOTIFICATION;
                    Log.i(TAG, "STATE_REQUESTED_CONNECTION_NOTIFICATION");
                }
                break;

            case STATE_REQUESTED_CONNECTION_NOTIFICATION:
                if (event == Connection_Event.EVT_NOTIFICATION_WRITTEN)
                {
                    m_IndicorConnectionState = IndicorConnection_State.STATE_OPERATIONAL;

                    // remove the dialog showing the connection progress bar
                    if (m_connectionDialog != null)
                    {
                        m_connectionDialog.cancel();
                    }

                    // stop the connection timeout timer
                    m_connectionTimeoutTimer.Cancel();

                    // tell the activity the everything is good to go
                    m_CallbackInterface.iFullyConnected();
                    Log.i(TAG, "STATE_OPERATIONAL");
                }
                break;

            case STATE_OPERATIONAL:
                break;

        }
    }

    @Override
    public void TimerExpired(int id)
    {
        if (id == BATTERY_READ_TIMER_ID)
        {
            if (m_VixiarHHBLEService != null)
            {
                m_VixiarHHBLEService.ReadBatteryLevel();
            }
        }
        if (id == CONNECTION_TIMEOUT_TIMER_ID)
        {
            m_VixiarHHBLEService.DisconnectFromIndicor();

            CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                    m_ActivityContext.getString(R.string.dlg_title_connection_problem),
                    m_ActivityContext.getString(R.string.dlg_msg_connection_problem),
                    "Ok",
                    null,
                    m_ActivityContext, DLG_ID_CONNECTION_ERROR, IndicorBLEServiceInterface.this);

        }
    }

    @Override
    public void onClickPositiveButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
            case DLG_ID_AUTHENTICATION_ERROR:
                m_connectionDialog.cancel();
                m_CallbackInterface.iError(ERROR_AUTHENTICATION);
                break;

            case DLG_ID_NO_PAIRED_DEVICE:
                m_connectionDialog.cancel();
                m_CallbackInterface.iError(ERROR_NO_PAIRED_DEVICES_FOUND);
                break;

            case DLG_ID_NO_HANDHELDS:
                m_connectionDialog.cancel();
                m_CallbackInterface.iRestart();
                break;

            case DLG_ID_CONNECTION_ERROR:
                m_connectionDialog.cancel();
                m_CallbackInterface.iRestart();
                break;

            case DLG_ID_SEQUENCE_ERROR:
                m_connectionDialog.cancel();
                m_CallbackInterface.iError(ERROR_SEQUENCE_ERROR);
                m_VixiarHHBLEService.DisconnectFromIndicor();
                break;

            case DLG_ID_LOW_BATTERY:
                // re start the connection timeout timer
                m_connectionTimeoutTimer.Start(this, CONNECTION_TIMEOUT_MS, true);

                // start the timer to update the battery level
                m_updateBatteryTimer.Start(IndicorBLEServiceInterface.getInstance(), BATTERY_READ_TIME_MS, false);

                m_bFirstSequenceNumber = true;

                m_VixiarHHBLEService.SubscribeToRealtimeDataNotification(true);
                m_IndicorConnectionState = IndicorConnection_State.STATE_REQUESTED_RT_NOTIFICATION;
                m_expectedRTDataSequnceNumber = 0;
                Log.i(TAG, "STATE_REQUESTED_RT_NOTIFICATION");
                break;
        }
    }

    @Override
    public void onClickNegativeButton(DialogInterface dialog, int dialogID)
    {
        m_CallbackInterface.iError(dialogID);
    }

    private class MyBLEMessageReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            //Log.i(TAG, "onReceive");
            if (arg1.hasExtra(IndicorBLEService.SCAN_RESULT))
            {
                m_ScanList.add((ScanResult) arg1.getParcelableExtra(IndicorBLEService.SCAN_RESULT));
            }
            else if (arg1.hasExtra(IndicorBLEService.CONNECTED))
            {
                ConnectionStateMachine(Connection_Event.EVT_BLE_CONNECTED);
            }
            else if (arg1.hasExtra(IndicorBLEService.DISCONNECTED))
            {
                m_CallbackInterface.iDisconnected();
                ConnectionStateMachine(Connection_Event.EVT_DISCONNECTED);
            }
            else if (arg1.hasExtra(IndicorBLEService.SERVICES_DISCOVERED))
            {
                ConnectionStateMachine(Connection_Event.EVT_SERVICES_READ);
            }
            else if (arg1.hasExtra(IndicorBLEService.REVISION_INFO_RECEIVED))
            {
                byte x[] = arg1.getByteArrayExtra(IndicorBLEService.REVISION_INFO_RECEIVED);
                String m_handheldFirmwareRevision = x[1] + "." + x[2] + "." + x[3] + "." + x[4];
                PatientInfo.getInstance().set_firmwareRevision(m_handheldFirmwareRevision);
                ConnectionStateMachine(Connection_Event.EVT_REVISION_READ);
            }
            else if (arg1.hasExtra(IndicorBLEService.NOTIFICATION_WRITTEN))
            {
                ConnectionStateMachine(Connection_Event.EVT_NOTIFICATION_WRITTEN);
            }
            else if (arg1.hasExtra(IndicorBLEService.EXTERNAL_CONNECTION_INFO_RECEIVED))
            {

            }
            else if (arg1.hasExtra(IndicorBLEService.RT_DATA_RECEIVED))
            {
                // make sure the sequence number is right
                int receivedSequenceNumber = (arg1.getByteArrayExtra(IndicorBLEService.RT_DATA_RECEIVED)[0] & 0xFF);
                //Log.i(TAG, "Received RT data, seq number= " + receivedSequenceNumber);
                if (m_bFirstSequenceNumber)
                {
                    // if this is the first message, we don't care what the sequence number is, but we need to know what to expect next time
                    m_bFirstSequenceNumber = false;
                    m_expectedRTDataSequnceNumber = (receivedSequenceNumber + 1) % 256;
                    PatientInfo.getInstance().getRealtimeData().AppendNewSample(arg1.getByteArrayExtra(IndicorBLEService.RT_DATA_RECEIVED));
                    m_CallbackInterface.iRealtimeDataNotification();
                }
                else if (receivedSequenceNumber != m_expectedRTDataSequnceNumber)
                {
                    CleanupFromConnectionLoss();
                    // the sequence number is wrong
                    Log.e(TAG, "Sequence number in RT data is wrong, expected " + m_expectedRTDataSequnceNumber + " received " + receivedSequenceNumber);

                    m_VixiarHHBLEService.DisconnectFromIndicor();

                    // show a dialog
                    CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                            m_ActivityContext.getString(R.string.dlg_title_sequence_error),
                            m_ActivityContext.getString(R.string.dlg_msg_sequence_error),
                            "Ok",
                            null,
                            m_ActivityContext, DLG_ID_SEQUENCE_ERROR, IndicorBLEServiceInterface.this);

                }
                else
                {
                    // everything is ok

                    m_expectedRTDataSequnceNumber = (m_expectedRTDataSequnceNumber + 1) % 256;
                    PatientInfo.getInstance().getRealtimeData().AppendNewSample(arg1.getByteArrayExtra(IndicorBLEService.RT_DATA_RECEIVED));
                    m_CallbackInterface.iRealtimeDataNotification();
                }
            }
            else if (arg1.hasExtra(IndicorBLEService.AUTHENTICATION_ERROR))
            {
                CleanupFromConnectionLoss();
                ConnectionStateMachine(Connection_Event.EVT_AUTHENTICATION_ERROR);
                CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                        m_ActivityContext.getString(R.string.dlg_title_authentication_error),
                        m_ActivityContext.getString(R.string.dlg_msg_authentication_error),
                        "Ok",
                        null,
                        m_ActivityContext, DLG_ID_AUTHENTICATION_ERROR, IndicorBLEServiceInterface.this);
            }
            else if (arg1.hasExtra(IndicorBLEService.CONNECTION_ERROR))
            {
                CleanupFromConnectionLoss();
                ConnectionStateMachine(Connection_Event.EVT_CONNECTiON_ERROR);

                CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                        m_ActivityContext.getString(R.string.dlg_title_connection_problem),
                        m_ActivityContext.getString(R.string.dlg_msg_connection_problem),
                        "Try Again",
                        "End Test", m_ActivityContext,
                        DLG_ID_CONNECTION_ERROR, IndicorBLEServiceInterface.this);

            }
            else if (arg1.hasExtra(IndicorBLEService.REALTIME_TIMEOUT_MSG))
            {
                CleanupFromConnectionLoss();

                PatientInfo.getInstance().getRealtimeData().CreateMarker(RealtimeDataMarker.Marker_Type.MARKER_TEST_ERROR,
                        PatientInfo.getInstance().getRealtimeData().GetRawData().size() - 1);

                CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                        m_ActivityContext.getString(R.string.dlg_title_handheld_timeout),
                        m_ActivityContext.getString(R.string.dlg_msg_handheld_timeout),
                        "Try Again",
                        "End Test", m_ActivityContext,
                        DLG_ID_CONNECTION_ERROR, IndicorBLEServiceInterface.this);
            }
            else if (arg1.hasExtra(IndicorBLEService.BATTERY_LEVEL_RECEIVED))
            {
                byte x[] = arg1.getByteArrayExtra(IndicorBLEService.BATTERY_LEVEL_RECEIVED);
                m_batteryLevel = x[BATTERY_LEVEL_PCT_INDEX];
                m_CallbackInterface.iBatteryLevelRead(m_batteryLevel);
                Log.i(TAG, "Battery = " + m_batteryLevel);
                ConnectionStateMachine(Connection_Event.EVT_BATTERY_READ);
            }
            else if (arg1.hasExtra(IndicorBLEService.LED_PWM_RECEIVED))
            {
                byte x[] = arg1.getByteArrayExtra(IndicorBLEService.LED_PWM_RECEIVED);
                m_LEDLevel = x[LED_LEVEL_PCT_INDEX];
                m_CallbackInterface.iLEDLevelRead(m_LEDLevel);
                Log.i(TAG, "LED = " + m_LEDLevel);
                m_CallbackInterface.iLEDLevelRead(m_LEDLevel);
            }
        }
    }

    private void CleanupFromConnectionLoss()
    {
        if (m_connectionTimeoutTimer != null)
        {
            m_connectionTimeoutTimer.Cancel();
        }
        if (m_updateBatteryTimer != null)
        {
            m_updateBatteryTimer.Cancel();
        }
        m_CallbackInterface.iDisconnected();

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
}
