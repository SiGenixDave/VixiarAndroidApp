package com.vixiar.indicor;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

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

    private Context mContext;

    public void initialize(Context c, IndicorDataInterface dataInterface)
    {
        mContext = c;
        mCallbackInterface = dataInterface;
    }

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            Log.i(TAG, "onServiceConnected");
            mVixiarHHBLEService = ((VixiarHandheldBLEService.LocalBinder) service).getService();
            mServiceConnected = true;
            mVixiarHHBLEService.initialize(IndicorConnection.this);
            mVixiarHHBLEService.ScanForIndicorHandhelds();
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
            AlertDialog dialog = alert.create();
            dialog.show();
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

    public void BLEScanCallback()
    {
        Log.d("IND", "Got a callback");
        mVixiarHHBLEService.ConnectToIndicor();
    }

    public void BLEConnected()
    {
        Log.d("IND", "Connected");

    }

    public void BLEDisconnected()
    {

    }

    public void BLEServicesDiscovered()
    {

    }

    public void BLEDataReceived()
    {

    }
}
