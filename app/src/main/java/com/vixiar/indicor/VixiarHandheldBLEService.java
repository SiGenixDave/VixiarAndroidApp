/*
Copyright (c) 2016, Cypress Semiconductor Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.



For more information on Cypress BLE products visit:
http://www.cypress.com/products/bluetooth-low-energy-ble
 */

package com.vixiar.indicor;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing the BLE data connection with the GATT database.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
// This is required to allow us to use the lollipop and later ScanForIndicorHandhelds APIs
public class VixiarHandheldBLEService extends Service
{
    private IndicorBLEServiceInterface mCallbackInterface;

    private final static String TAG = VixiarHandheldBLEService.class.getSimpleName();

    // UUIDs for the service and characteristics that the Vixiar service uses
    public final static String RTDataCharacteristicUUID = "7991CF92-D18B-40EB-AFBE-4EECB596C677";
    public final static String batteryLevelDataCharacteristicUUID = "590D5C82-2999-4C12-9D75-A3BC343FBCA5";

    private final static String CCCDUUID = "00002902-0000-1000-8000-00805f9b34fb";

    private final static String vixiarRealTimeServiceUUID = "83638348-96D8-455A-8451-0630BCD02558";

    // Bluetooth objects that we need to interact with
    private static BluetoothManager mBluetoothManager;
    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothLeScanner mLEScanner;
    private static BluetoothDevice mLeDevice;
    private static BluetoothGatt mBluetoothGatt;

    // Bluetooth characteristics that we need to read/write
    private static BluetoothGattCharacteristic mRTDataCharacteristic;
    private static BluetoothGattDescriptor mRTNotificationCCCD;

    private final IBinder mBinder = new LocalBinder();

    // realtime data
    private static ArrayList<Integer> mPressureData = new ArrayList<Integer>();
    private static ArrayList<Integer> mPPGData = new ArrayList<Integer>();
    private static Integer mBatteryLevel;

    private final ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            mLeDevice = result.getDevice();
            mLEScanner.stopScan(mScanCallback); // Stop scanning after the first device is found
            if (mCallbackInterface != null)
            {
                mCallbackInterface.BLEScanCallback();
            }
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                Log.i(TAG, "Connected to GATT server.");
                if (mCallbackInterface != null)
                {
                    mCallbackInterface.BLEConnected();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.i(TAG, "Disconnected from GATT server.");
                if (mCallbackInterface != null)
                {
                    mCallbackInterface.BLEDisconnected();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            // Get just the service that we are looking for
            BluetoothGattService mService = gatt.getService(UUID.fromString(vixiarRealTimeServiceUUID));

            // Get characteristics from our desired service
            mRTDataCharacteristic = mService.getCharacteristic(UUID.fromString(RTDataCharacteristicUUID));

            // Get the descriptors
            mRTNotificationCCCD = mRTDataCharacteristic.getDescriptor(UUID.fromString(CCCDUUID));

            // Broadcast that service/characteristic/descriptor discovery is done
            if (mCallbackInterface != null)
            {
                mCallbackInterface.BLEServicesDiscovered();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                // Verify that the read was the battery level
                String uuid = characteristic.getUuid().toString();
                // In this case, the only read the app does is the battery level.
                // If the application had additional characteristics to read we could
                // use a switch statement here to operate on each one separately.
                if (uuid.equals(batteryLevelDataCharacteristicUUID))
                {
                    final byte[] data = characteristic.getValue();
                    // Set the LED switch state variable based on the characteristic value ttat was read
                    mBatteryLevel = (data[0] & 0xff);
                }
                // Notify the main activity that new data is available
                if (mCallbackInterface != null)
                {
                    mCallbackInterface.BLEDataReceived();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic)
        {

            String uuid = characteristic.getUuid().toString();

            // In this case, the notifications the apps gets are the PPG and pressure data.
            // If the application had additional notifications we could
            // use a switch statement here to operate on each one separately.
            String temp = uuid.toUpperCase();

            if (uuid.toUpperCase().equals(RTDataCharacteristicUUID))
            {
                mPPGData.clear();
                mPressureData.clear();
                for (int i = 1; i < characteristic.getValue().length; i+=4)
                {
                    int result = (int)characteristic.getValue()[i+1]&0xff;
                    result += 256*((int)characteristic.getValue()[i]&0xff);
                    mPressureData.add(result);

                    result = (int)characteristic.getValue()[i+3]&0xff;
                    result += 256*((int)characteristic.getValue()[i+2]&0xff);
                    mPPGData.add(result);
                }
                Log.i(TAG, "Got a PPG packet");
                if (mCallbackInterface != null)
                {
                    mCallbackInterface.BLEDataReceived();
                }
            }
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback()
            {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
                {
                    mLeDevice = device;
                    //noinspection deprecation
                    mBluetoothAdapter.stopLeScan(mLeScanCallback); // Stop scanning after the first device is found
                    if (mCallbackInterface != null)
                    {
                        mCallbackInterface.BLEScanCallback();
                    }
                }
            };

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // The BLE close method is called when we unbind the service to free up the resources.
        close();
        return super.onUnbind(intent);
    }

    public boolean initialize(IndicorBLEServiceInterface i)
    {
        mCallbackInterface = i;

        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null)
        {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null)
            {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)
        {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void ScanForIndicorHandhelds()
    {
        /* Scan for devices and look for the one with the service that we want */
        UUID capsenseLedService = UUID.fromString(vixiarRealTimeServiceUUID);
        UUID[] capsenseLedServiceArray = {capsenseLedService};

        // Use old ScanForIndicorHandhelds method for versions older than lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            //noinspection deprecation
            mBluetoothAdapter.startLeScan(capsenseLedServiceArray, mLeScanCallback);
        } else
        { // New BLE scanning introduced in LOLLIPOP
            ScanSettings settings;
            List<ScanFilter> filters;
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            // We will ScanForIndicorHandhelds just for the CAR's UUID
            ParcelUuid PUuid = new ParcelUuid(capsenseLedService);
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(PUuid).build();
            filters.add(filter);
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    public boolean ConnectToIndicor()
    {
        if (mBluetoothAdapter == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null)
        {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        // We want to directly ConnectToIndicor to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = mLeDevice.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        return true;
    }

    public void DiscoverIndicorServices()
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.discoverServices();
    }

    public void DisconnectFromIndicor()
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close()
    {
        if (mBluetoothGatt == null)
        {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void writeRTDataNotification(boolean value)
    {
        // Set notifications locally in the CCCD
        mBluetoothGatt.setCharacteristicNotification(mRTDataCharacteristic, value);

        // Write Notification value to the device
        Log.i(TAG, "Realtime Notification " + value);
        mRTNotificationCCCD.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(mRTNotificationCCCD);
    }

    private void broadcastUpdate(final String action)
    {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public ArrayList<Integer> GetPPGData()
    {
        return mPPGData;
    }

    public ArrayList<Integer> GetPressureData()
    {
        return mPressureData;
    }

    public class LocalBinder extends Binder
    {
        VixiarHandheldBLEService getService()
        {
            return VixiarHandheldBLEService.this;
        }
    }
}