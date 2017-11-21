package com.vixiar.indicor.Activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.vixiar.indicor.BuildConfig;
import com.vixiar.indicor.R;

public class MainActivity extends Activity
{
    private final String TAG = this.getClass().getSimpleName();

    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // this is a request code that is used if BLE isn't turned on...
    // it tells the response handlet to start the data collection intent if the user enables ble
    private static final int REQUEST_START_CONNECTION_BLE = 1;

    Handler handler = new Handler();
    final Runnable r = new Runnable()
    {
        public void run()
        {
            CheckBLE();
        }
    };

    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup the top bar
        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, "");
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.main_screen_title));

        // put the version number into the bottom status bar
        String nameAndVersion = getString(R.string.main_screen_title) +
                " V" + BuildConfig.VERSION_NAME;

        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView versionText = (TextView) findViewById(R.id.textViewVersion);
        versionText.setText(nameAndVersion);
        versionText.setTypeface(robotoTypeface);

        TextView v = (TextView) findViewById(R.id.websiteLbl);
        v.setTypeface(robotoTypeface);
        v.setText(R.string.vixiar_web);

        v = (TextView) findViewById(R.id.startTestLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.settingsLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.manualLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.trainingLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.tagLineLbl);
        v.setTypeface(robotoTypeface);

        // FULL SCREEN (add if FS is desired)
        /*
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        */
        // wait a couple seconds then check the BLE stuff
        handler.postDelayed(r, 2000);
    }

    //This method required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSION_REQUEST_COARSE_LOCATION:
            {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                }
                else
                {
                    final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to communicate with handheld devices.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(DialogInterface dialog)
                        {
                            finish();
                            System.exit(0);
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)


    // quick stuff to check that BLE is supported and turned on
    private BluetoothAdapter GetAdapter()
    {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();
    }

    public boolean IsBLEAvailable()
    {
        return GetAdapter() != null;
    }

    public boolean IsBLEEnabled()
    {
        BluetoothAdapter adapter = GetAdapter();
        if (adapter != null)
        {
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!adapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_START_CONNECTION_BLE);
            }
            return adapter.isEnabled();
        }
        else
        {
            return false;
        }
    }

    // this get's called after the user either accepts or denys turning ble on
    // it they didn't turn it on, the app quits
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_START_CONNECTION_BLE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Log.i(TAG, "BLE was enabled");
                // This section required for Android 6.0 (Marshmallow)
                // Make sure location access is on or BLE won't scan
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    // Android M Permission check 
                    if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("This app requires location access in order to function properly.");
                        builder.setMessage("Please grant location access so this app can communicate with handheld devices.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener()
                        {
                            public void onDismiss(DialogInterface dialog)
                            {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                            }
                        });
                        builder.show();
                    }
                } //End of section for Android 6.0 (Marshmallow)
            }
            else
            {
                Log.i(TAG, "BLE was not enabled");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("BLE not enabled");
                builder.setMessage("BLE must be enabled.  BLE is required to communicate to the handheld.");
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog,
                                                int which)
                            {
                                finish();
                                System.exit(0);
                            }
                        });
                builder.show();
            }
        }
    }

    public void onImageStartClick(View view)
    {
        Intent intent = new Intent(this, PatInfoActivity.class);
        startActivity(intent);
    }

    private void CheckBLE()
    {
        // TODO: check that location services are enabled on the device
        // verify that this device supports bluetooth and it's turned on
        if (!IsBLEAvailable())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("BLE not supported");
            builder.setMessage("This device does not support Bluetooth Low Energy which is required to communicate to the handheld.");
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog,
                                            int which)
                        {
                            finish();
                            System.exit(0);
                        }
                    });
            builder.show();
        }
        else
        {
            // this call will check if BLE is enabled, if it isn't the user will be given the chance to
            // enable it.  If they don't the app will quit
            // if it is enabled, we need to make sure location access is enabled

            // This section required for Android 6.0 (Marshmallow)
            // Make sure location access is on or BLE won't scan
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (IsBLEEnabled())
                {
                    // Android M Permission check 
                    if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("This app requires location access in order to function properly.");
                        builder.setMessage("Please grant location access so this app can communicate with handheld devices.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener()
                        {
                            public void onDismiss(DialogInterface dialog)
                            {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                            }
                        });
                        builder.show();
                    }
                }
            } //End of section for Android 6.0 (Marshmallow)
        }
    }
}
