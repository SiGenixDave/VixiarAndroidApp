package com.sigenix.indicor;

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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class PatInfoActivity extends Activity
{
    // this is a request code that is used if BLE isn't turned on...
    // it tells the response handlet to start the data collection intent if the user enables ble
    private static final int REQUEST_START_CONNECTION_BLE = 1;
    
    // TAG is used for informational messages
    private final static String TAG = PatInfoActivity.class.getSimpleName();
    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private EditText subjectId;
    private EditText subjectInfo;
    private EditText systolicBloodPressure;
    private EditText diastolicBloodPressure;
    private EditText heightfeet;
    private EditText heightinches;
    private EditText weight;
    private EditText birthdateMonth;
    private EditText birthdateDay;
    private EditText birthdateYear;

    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // FULL SCREEN (add if FS is desired)
        /*
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        */
        setContentView(R.layout.activity_pat_info);
        InitPatientInfoViews();

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
                        }
                    });
            builder.show();
        } else
        {
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
                } else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to communicate with handheld devices.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(DialogInterface dialog)
                        {
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    private void InitPatientInfoViews()
    {

        // Used to change border color when text is entered by user
        subjectId.addTextChangedListener(new GenericTextWatcher(subjectId));
        subjectInfo.addTextChangedListener(new GenericTextWatcher(subjectInfo));
        systolicBloodPressure.addTextChangedListener(new GenericTextWatcher(systolicBloodPressure));
        diastolicBloodPressure.addTextChangedListener(new GenericTextWatcher(diastolicBloodPressure));
        heightfeet.addTextChangedListener(new GenericTextWatcher(heightfeet));
        heightinches.addTextChangedListener(new GenericTextWatcher(heightinches));
        weight.addTextChangedListener(new GenericTextWatcher(weight));
        birthdateMonth.addTextChangedListener(new GenericTextWatcher(birthdateMonth));
        birthdateDay.addTextChangedListener(new GenericTextWatcher(birthdateDay));
        birthdateYear.addTextChangedListener(new GenericTextWatcher(birthdateYear));
    }

    // User clicks on Training image
    public void imageTrainingClick(View view)
    {
        Intent intent = new Intent(this, TrainingActivity.class);
        startActivity(intent);
    }

    // User clicks on "next" image
    public void imageNextClick(View view)
    {
        Intent intent = new Intent(this, DataCollectionActivity.class);
        if (IsBLEEnabled())
        {
            startActivity(intent);
        }
    }

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
        } else
        {
            return false;
        }
    }

    // this get's called after the user either accepts or denys turning ble on
    // it they accept, the data collection activity starts
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_START_CONNECTION_BLE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Intent intent = new Intent(this, DataCollectionActivity.class);
                startActivity(intent);
            }
        }
    }

    // This class is used to change the rectangle border color when text is added
    private class GenericTextWatcher implements TextWatcher
    {
        private View view;

        private GenericTextWatcher(View view)
        {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
        {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
        {
        }

        public void afterTextChanged(Editable editable)
        {
            GradientDrawable myGrad = (GradientDrawable) view.getBackground();
            myGrad.setStroke(2, Color.BLUE);
        }
    }
}

