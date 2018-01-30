package com.vixiar.indicor.BLEInterface;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import com.vixiar.indicor.Activities.GenericTimer;
import com.vixiar.indicor.Activities.TimerCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * Service for managing the BLE data connection with the GATT database.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
// This is required to allow us to use the lollipop and later APIs
public class IndicorBLEService extends Service implements TimerCallback
{
    private final static String TAG = IndicorBLEService.class.getSimpleName();

    // the ID used to filter messages from the service to the handler class
    final static String MESSAGE_ID = "vixiarBLEService";
    final static String SCAN_RESULT = "scanResult";
    final static String CONNECTED = "connected";
    final static String DISCONNECTED = "disconnected";
    final static String SERVICES_DISCOVERED = "services_discovered";
    final static String AUTHENTICATION_ERROR = "authentication_error";
    final static String CONNECTION_ERROR = "connection_error";
    final static String NOTIFICATION_WRITTEN = "notification_written";
    final static String RT_DATA_RECEIVED = "rt_data";
    final static String BATTERY_LEVEL_RECEIVED = "batt_level";
    final static String REVISION_INFO_RECEIVED = "revision_info";
    final static String EXTERNAL_CONNECTION_INFO_RECEIVED = "external_connection_info";
    final static String REALTIME_TIMEOUT_MSG = "ble_timeout";

    // UUIDs for the service and characteristics that the Vixiar service uses
    private final static String RT_DATA_CHARACTERISTIC_UUID = "7991CF92-D18B-40EB-AFBE-4EECB596C677";
    private final static String BATTERY_LEVEL_CHARACTERISTIC_UUID = "590D5C82-2999-4C12-9D75-A3BC343FBCA5";
    private final static String REVISION_INFO_CHARACTERISTIC_UUID = "95D09D70-E371-40B7-931F-EF46B143E4D6";
    private final static String EXTERNAL_CONNECTIONS_CHARACTERISTIC_UUID = "B4A5C92C-30A6-4DCB-BB8B-02D3CC4AD835";
    private final static String PPG_DRIVE_CHARACTERISTIC_UUID = "D45680E8-5B7C-41D6-8B98-5D08346AD7C4";

    private final static String CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private final static String VIXIAR_REALTIME_SERVICE_UUID = "83638348-96D8-455A-8451-0630BCD02558";

    // Bluetooth objects that we need to interact with
    private static BluetoothManager m_BluetoothManager;
    private static BluetoothAdapter m_BluetoothAdapter;
    private static BluetoothLeScanner m_LEScanner;
    private static BluetoothGatt m_BluetoothGatt;

    // Bluetooth characteristics that we need to read/write
    private static BluetoothGattCharacteristic m_RTDataCharacteristic;
    private static BluetoothGattCharacteristic m_BatteryLevelCharacteristic;
    private static BluetoothGattCharacteristic m_RevisionInfoCharacteristic;
    private static BluetoothGattCharacteristic m_ExternalConnectionsCharacteristic;
    private static BluetoothGattCharacteristic m_PPGDriveCharacteristic;

    private static BluetoothGattDescriptor m_RTNotificationCCCD;
    private static BluetoothGattDescriptor m_ExternalConnectionNotificationCCCD;

    private final IBinder m_Binder = new LocalBinder();

    private static GenericTimer m_realTimeDataTimeoutTimer;
    private final int TIMEOUT_TIMER_ID = 0;
    private final int RT_DATA_TIMEOUT_MS = 2000;

    private boolean m_bConnectedToIndicor = false;

    // ----------------------------- BLE Callbacks -----------------------------------------------
    // this is the callback used for older devices
    private BluetoothAdapter.LeScanCallback m_LeScanCallback =
            new BluetoothAdapter.LeScanCallback()
            {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
                {
                    //TODO: this probably should be handled for older devices
                    //Log.i(TAG, "LEScan Callback");
                }
            };

    // this is the callback used for newer devices
    private final ScanCallback m_ScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            //Log.i(TAG, "Scan Callback");
            SendDataToConnectionClass(SCAN_RESULT, result);
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            super.onDescriptorWrite(gatt, descriptor, status);
            // if this comes back without a status of 0, the write didn't work, which may mean that the
            // handheld lost pairing
            Log.i(TAG, "onDescriptorWrite; status = " + status);
            if (status == GATT_INSUFFICIENT_AUTHENTICATION)
            {
                SendDataToConnectionClass(AUTHENTICATION_ERROR, null);
            }
            else
            {
                SendDataToConnectionClass(NOTIFICATION_WRITTEN, null);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            Log.i(TAG, "onConnectionStateChange; status = " + status + " newState = " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                Log.i(TAG, "onConnectionStateChange CONNECTED");
                m_bConnectedToIndicor = true;
                SendDataToConnectionClass(CONNECTED, null);
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                m_BluetoothGatt.close();
                m_BluetoothGatt = null;
                Log.i(TAG, "onConnectionStateChange DISCONNECTED");
                m_bConnectedToIndicor = false;
                SendDataToConnectionClass(DISCONNECTED, null);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            Log.i(TAG, "onServicesDiscovered status = " + status);

            // Get just the service that we are looking for
            BluetoothGattService mService = gatt.getService(UUID.fromString(VIXIAR_REALTIME_SERVICE_UUID));

            // Get characteristics from our desired service
            m_RTDataCharacteristic = mService.getCharacteristic(UUID.fromString(RT_DATA_CHARACTERISTIC_UUID));
            m_BatteryLevelCharacteristic = mService.getCharacteristic(UUID.fromString(BATTERY_LEVEL_CHARACTERISTIC_UUID));
            m_PPGDriveCharacteristic = mService.getCharacteristic(UUID.fromString(PPG_DRIVE_CHARACTERISTIC_UUID));
            m_ExternalConnectionsCharacteristic = mService.getCharacteristic(UUID.fromString(EXTERNAL_CONNECTIONS_CHARACTERISTIC_UUID));
            m_RevisionInfoCharacteristic = mService.getCharacteristic(UUID.fromString(REVISION_INFO_CHARACTERISTIC_UUID));

            // Get the descriptors
            m_RTNotificationCCCD = m_RTDataCharacteristic.getDescriptor(UUID.fromString(CCCD_UUID));
            m_ExternalConnectionNotificationCCCD = m_ExternalConnectionsCharacteristic.getDescriptor(UUID.fromString(CCCD_UUID));

            if (m_RTDataCharacteristic == null || m_BatteryLevelCharacteristic == null ||
                    m_PPGDriveCharacteristic == null || m_ExternalConnectionsCharacteristic == null ||
                    m_RevisionInfoCharacteristic == null || m_RTNotificationCCCD == null ||
                    m_ExternalConnectionNotificationCCCD == null)
            {
                Log.i(TAG, "characteristic came back as null");
            }
            else
            {
                // Broadcast that service/characteristic/descriptor discovery is done
                SendDataToConnectionClass(SERVICES_DISCOVERED, null);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            Log.i(TAG, "onCharacteristicRead status =" + status);
            if (status == GATT_SUCCESS)
            {
                // Verify that the read was the battery level
                String uuid = characteristic.getUuid().toString();
                // In this case, the only read the app does is the battery level.
                // If the application had additional characteristics to read we could
                // use a switch statement here to operate on each one separately.
                if (uuid.toUpperCase().equals(BATTERY_LEVEL_CHARACTERISTIC_UUID))
                {
                    SendDataToConnectionClass(BATTERY_LEVEL_RECEIVED, characteristic.getValue());
                }
                else if (uuid.toUpperCase().equals(EXTERNAL_CONNECTIONS_CHARACTERISTIC_UUID))
                {
                    SendDataToConnectionClass(EXTERNAL_CONNECTION_INFO_RECEIVED, characteristic.getValue());
                }
                else if (uuid.toUpperCase().equals(REVISION_INFO_CHARACTERISTIC_UUID))
                {
                    SendDataToConnectionClass(REVISION_INFO_RECEIVED, characteristic.getValue());
                }
            }
            else if (status == GATT_INSUFFICIENT_AUTHENTICATION)
            {
                SendDataToConnectionClass(AUTHENTICATION_ERROR, null);
            }
            else
            {
                SendDataToConnectionClass(CONNECTION_ERROR, null);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic)
        {
            //Log.i(TAG, "Characteristic notification");

            String uuid = characteristic.getUuid().toString();

            if (uuid.toUpperCase().equals(RT_DATA_CHARACTERISTIC_UUID))
            {
                m_realTimeDataTimeoutTimer.Reset();
                SendDataToConnectionClass(RT_DATA_RECEIVED, characteristic.getValue());
            }
            else if (uuid.toUpperCase().equals(EXTERNAL_CONNECTIONS_CHARACTERISTIC_UUID))
            {
                SendDataToConnectionClass(EXTERNAL_CONNECTION_INFO_RECEIVED, characteristic.getValue());
            }
        }
    };

    // --------------------------End of BLE callbacks -------------------------------------------

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.i(TAG, "onBind");
        return m_Binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void TimerExpired(int id)
    {
        if (id == TIMEOUT_TIMER_ID)
        {
            Log.i(TAG, "Timeout expired");
            DisconnectFromIndicor();
            SendDataToConnectionClass(REALTIME_TIMEOUT_MSG, null);
        }
    }

    public class LocalBinder extends Binder
    {
        IndicorBLEService getService()
        {
            return IndicorBLEService.this;
        }
    }


    // --------------------------- Public interface functions -------------------------------------------

    public boolean Initialize()
    {
        if (m_BluetoothManager == null)
        {
            m_BluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (m_BluetoothManager == null)
            {
                Log.e(TAG, "Unable to Initialize BluetoothManager.");
                return false;
            }
        }
        m_BluetoothAdapter = m_BluetoothManager.getAdapter();
        if (m_BluetoothAdapter == null)
        {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        if (m_realTimeDataTimeoutTimer == null)
        {
            m_realTimeDataTimeoutTimer = new GenericTimer(TIMEOUT_TIMER_ID);
        }
        return true;
    }

    public void ScanForIndicorHandhelds()
    {
        /* Scan for devices and look for the one with the service that we want */
        UUID handheldService = UUID.fromString(VIXIAR_REALTIME_SERVICE_UUID);
        UUID[] handheldServiceArray = {handheldService};

        // Use old ScanForIndicorHandhelds method for versions older than lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            //noinspection deprecation
            m_BluetoothAdapter.startLeScan(handheldServiceArray, m_LeScanCallback);
        }
        else
        { // New BLE scanning introduced in LOLLIPOP
            ScanSettings settings;
            List<ScanFilter> filters;
            m_LEScanner = m_BluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
            filters = new ArrayList<>();

            // scan just for the handheld's service UUID
            ParcelUuid PUuid = new ParcelUuid(handheldService);
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(PUuid).build();
            filters.add(filter);
            m_LEScanner.startScan(filters, settings, m_ScanCallback);
        }
    }

    public boolean ConnectToSpecificIndicor(BluetoothDevice device)
    {
        if (m_BluetoothAdapter == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (m_bConnectedToIndicor)
        {
            Log.d(TAG, "Trying to use an existing m_BluetoothGatt for connection.");
            return m_BluetoothGatt.connect();

        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        m_BluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        return true;
    }

    public void DiscoverIndicorServices()
    {
        if (m_BluetoothAdapter != null || m_BluetoothGatt != null)
        {
            m_BluetoothGatt.discoverServices();
        }
        else
        {
            Log.e(TAG, "BluetoothAdapter not initialized");
        }
    }

    public void DisconnectFromIndicor()
    {
        Log.i(TAG, "DisconnectFromIndicor m_bluetoothAdaptor = " + m_BluetoothAdapter + ", m_bluetoothGatt = " + m_BluetoothGatt);

        if (m_BluetoothAdapter != null && m_BluetoothGatt != null)
        {
            Log.i(TAG, "Killing m_BluetoothGat");
            m_BluetoothGatt.disconnect();
            m_RTDataCharacteristic = null;
            m_BatteryLevelCharacteristic = null;
            m_RevisionInfoCharacteristic = null;
            m_ExternalConnectionsCharacteristic = null;
            m_PPGDriveCharacteristic = null;
            m_RTNotificationCCCD = null;
            m_ExternalConnectionNotificationCCCD = null;
        }
        m_bConnectedToIndicor = false;
        if (m_realTimeDataTimeoutTimer != null)
        {
            m_realTimeDataTimeoutTimer.Cancel();
            Log.i(TAG, "Timeout timer cancelled");
        }
    }

    public void SubscribeToRealtimeDataNotification(boolean value)
    {
        if (m_RTDataCharacteristic != null && m_BluetoothGatt != null && m_RTNotificationCCCD != null)
        {
            Log.i(TAG, "Setting real time notification " + value);

            // Set notifications locally in the CCCD
            m_BluetoothGatt.setCharacteristicNotification(m_RTDataCharacteristic, value);

            // Write Notification value to the device
            m_RTNotificationCCCD.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            m_BluetoothGatt.writeDescriptor(m_RTNotificationCCCD);

            // start the timeout timer
            m_realTimeDataTimeoutTimer.Start(this, RT_DATA_TIMEOUT_MS, true);
        }
    }

    public void SubscribeToConnectionNotification(boolean value)
    {
        if (m_ExternalConnectionsCharacteristic != null && m_BluetoothGatt != null && m_ExternalConnectionNotificationCCCD != null)
        {
            Log.i(TAG, "Setting connection notification " + value);

            // Set notifications locally in the CCCD
            m_BluetoothGatt.setCharacteristicNotification(m_ExternalConnectionsCharacteristic, value);

            // Write Notification value to the device
            m_ExternalConnectionNotificationCCCD.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            m_BluetoothGatt.writeDescriptor(m_ExternalConnectionNotificationCCCD);
        }
    }

    public void ReadRevisionInformation()
    {
        if (m_RevisionInfoCharacteristic != null && m_BluetoothGatt != null)
        {
            Log.i(TAG, "Asking for revision information");
            if (m_BluetoothGatt != null)
            {
                m_BluetoothGatt.readCharacteristic(m_RevisionInfoCharacteristic);
            }
        }
        else
        {
            Log.i(TAG, "Failure reading revision information");
        }
    }

    public void ReadBatteryLevel()
    {
        if (m_BatteryLevelCharacteristic != null && m_BluetoothGatt != null)
        {
            Log.i(TAG, "Asking for battery level");
            if (m_BluetoothGatt != null)
            {
                m_BluetoothGatt.readCharacteristic(m_BatteryLevelCharacteristic);
            }
            else
            {
                Log.i(TAG, "Failure reading battery level");
            }
        }
    }

    public void StopScanning()
    {
        if (m_LEScanner != null)
        {
            m_LEScanner.stopScan(m_ScanCallback);
        }
    }

    public boolean AmConnectedToIndicor()
    {
        return m_bConnectedToIndicor;
    }

    // ------------------ Utility functions -----------------------------------------------------
    private void SendDataToConnectionClass(String name, Object data)
    {
        Intent intent = new Intent();
        intent.setAction(MESSAGE_ID);
        if (data instanceof Integer)
        {
            intent.putExtra(name, (Integer) data);
        }
        else if (data instanceof byte[])
        {
            intent.putExtra(name, (byte[]) data);
        }
        else if (data instanceof ScanResult)
        {
            intent.putExtra(name, (ScanResult) data);
        }
        else
        {
            intent.putExtra(name, "");
        }
        sendBroadcast(intent);
    }
}