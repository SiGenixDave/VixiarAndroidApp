package com.vixiar.indicor.Upload_Interface;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.dropbox.core.DbxException;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by gsevinc on 11/28/2017.
 */

public class UploadServiceInterface
{
    // make this a singleton class
    private static final UploadServiceInterface ourInstance = new UploadServiceInterface();

    public static UploadServiceInterface getInstance()
    {
        return ourInstance;
    }

    private final static String TAG = "IND";
    // private MyBLEMessageReceiver myBLEMessageReceiver;

    private static UploadService mVixiarUploadService;

    private AlertDialog connectionDialog;

    private Handler handler = new Handler();
    private final Runnable runnable = new Runnable()
    {
        public void run()
        {
            //ScanTimeout();
        }
    };

    public void initialize(Context c)
    {
        Context mContext = c;

        // Start the Upload Service
        Log.i(TAG, "Starting Upload Service");
        Intent gattServiceIntent = new Intent(mContext, UploadService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "Upload Started");

        // create a receiver to receive messages from the service
        //myBLEMessageReceiver = new MyBLEMessageReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UploadService.MESSAGE_ID);
        //mContext.registerReceiver(myBLEMessageReceiver, intentFilter);
    }

    /**
     * This manages the lifecycle of the Upload service.
     * When the service starts we get the service object and Initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            Log.i(TAG, "Starting Upload Service");
            mVixiarUploadService = ((UploadService.LocalBinder) service).getService();
            boolean mServiceConnected = true;
            mVixiarUploadService.Initialize();
            Log.i(TAG, "Upload Service Started");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Log.i(TAG, "onServiceDisconnected");
            mVixiarUploadService = null;
        }
    };

    public void PauseUpload()
    {
        mVixiarUploadService.Pause();
    }

    public void ResumeUpload()
    {
        mVixiarUploadService.Resume();
    }
}
