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
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

public class PatInfoActivity extends Activity
{
    // this is a request code that is used if BLE isn't turned on...
    // it tells the response handlet to start the data collection intent if the user enables ble
    private static final int REQUEST_START_CONNECTION_BLE = 1;
    
    // TAG is used for informational messages
    private final static String TAG = PatInfoActivity.class.getSimpleName();
    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private EditText txtPatientID;
    private EditText txtDOB;

    private NumberPicker npDOBMonth;
    private NumberPicker npDOBDay;
    private NumberPicker npDOBYear;

    private String[] monthString;
    private String[] genderString;

    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pat_info);
        initializeControls();


        // FULL SCREEN (add if FS is desired)
        /*
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        */

        // verify that this device supports bluetooth and it's turned on
        /*
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
        */
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

    private void initializeControls()
    {
        txtPatientID = (EditText) findViewById(R.id.txtPatientID);
        txtPatientID.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtPatientID.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                }
                else
                {
                    txtPatientID.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                }
            }
        });

        txtDOB = (EditText) findViewById(R.id.txtDOB);
        txtDOB.setShowSoftInputOnFocus(false);
        txtDOB.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtDOB.setTextColor(getResources().getColor(R.color.colorPatientEntryHighlightedValue));
                    npDOBDay.setVisibility(View.VISIBLE);
                    npDOBMonth.setVisibility(View.VISIBLE);
                    npDOBYear.setVisibility(View.VISIBLE);

                }
                else
                {
                    txtDOB.setTextColor(getResources().getColor(R.color.colorPatientEntryNormalValue));
                    npDOBDay.setVisibility(View.GONE);
                    npDOBMonth.setVisibility(View.GONE);
                    npDOBYear.setVisibility(View.GONE);
                }
            }
        });

        monthString = getResources().getStringArray(R.array.months_array);

        NumberPicker.OnValueChangeListener dobChangeListener = new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                int nMonth = npDOBMonth.getValue();
                int nDay = npDOBDay.getValue();
                int nYear = npDOBYear.getValue();
                txtDOB.setText(monthString[nMonth] + " " + String.valueOf(nDay) + " " + String.valueOf(nYear));
            }
        };

        npDOBDay = (NumberPicker) findViewById(R.id.npDOBDay);
        npDOBDay.setVisibility(View.GONE);
        npDOBDay.setMinValue(1);
        npDOBDay.setMaxValue(31);
        npDOBDay.setOnValueChangedListener(dobChangeListener);

        npDOBMonth = (NumberPicker) findViewById(R.id.npDOBMonth);
        npDOBMonth.setVisibility(View.GONE);
        npDOBMonth.setMinValue(0);
        npDOBMonth.setMaxValue(monthString.length-1);
        npDOBMonth.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return monthString[value];
            }
        });
        npDOBMonth.setOnValueChangedListener(dobChangeListener);

        npDOBYear = (NumberPicker) findViewById(R.id.npDOBYear);
        npDOBYear.setVisibility(View.GONE);
        npDOBYear.setMinValue(1930);
        npDOBYear.setMaxValue(2017);
        npDOBYear.setValue(1960);
        npDOBYear.setOnValueChangedListener(dobChangeListener);
    }
}

