package com.sigenix.indicor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity  {

    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static VixiarHandheldBLEService vixiarHandheldBLEService;

    private static final int REQUEST_ENABLE_BLE = 1;

    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the VixiarHandheldBLEService is connected
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            vixiarHandheldBLEService = ((VixiarHandheldBLEService.LocalBinder) service).getService();
            mServiceConnected = true;
            vixiarHandheldBLEService.initialize();
        }

        /**
         * This is called when the VixiarHandheldBLEService is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            vixiarHandheldBLEService = null;
        }
    };

    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FULL SCREEN (add if FS is desired)
        /*
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        */
        setContentView(R.layout.activity_main);

        InitPatientInfoViews();
        BLESupport.getInstance().Init(this);
    }

    //This method required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to communicate with the handheld when the app is in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver. This specified the messages the main activity looks for from the PSoCCapSenseLedService
        final IntentFilter filter = new IntentFilter();
        filter.addAction(VixiarHandheldBLEService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(VixiarHandheldBLEService.ACTION_CONNECTED);
        filter.addAction(VixiarHandheldBLEService.ACTION_DISCONNECTED);
        filter.addAction(VixiarHandheldBLEService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(VixiarHandheldBLEService.ACTION_DATA_RECEIVED);
        registerReceiver(mBleUpdateReceiver, filter);

    }


    private void InitPatientInfoViews() {

        subjectId = (EditText)findViewById(R.id.edTxtSubjectId);
        subjectInfo = (EditText)findViewById(R.id.edTxtSubjectInfo);
        systolicBloodPressure = (EditText)findViewById(R.id.edTxtSystolicBloodPressure);
        diastolicBloodPressure = (EditText)findViewById(R.id.edTxtDiastolicBloodPressure);
        heightfeet = (EditText)findViewById(R.id.edTxtHeightFeet);
        heightinches = (EditText)findViewById(R.id.edTxtHeightInches);
        weight = (EditText)findViewById(R.id.edTxtWeight);
        birthdateMonth = (EditText)findViewById(R.id.edTxtBirthdateMonth);
        birthdateDay = (EditText)findViewById(R.id.edTxtBirthdateDay);
        birthdateYear = (EditText)findViewById(R.id.edTxtBirthdateYear);

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
    public void imageTrainingClick(View view) {

        Intent intent = new Intent(this, TrainingActivity.class);
        startActivity(intent);

    }

    // User clicks on "next" image
    public void imageNextClick(View view) {
        // verify that this device supports bluetooth and it's turned on

        if (!BLESupport.getInstance().IsBLEAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("BLE not supported");
            builder.setMessage("This device does not support Bluetooth Low Energy which is required to communicate to the handheld.");
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            Toast.makeText(getApplicationContext(),"Yes is clicked",Toast.LENGTH_LONG).show();
                        }
                    });
            builder.show();
        }
        else
        {
            if (!BLESupport.getInstance().IsBLEEnabled()){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("BLE not enabled");
                builder.setMessage("Please enable Bluetooth communications and try again.");
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                Toast.makeText(getApplicationContext(),"Yes is clicked",Toast.LENGTH_LONG).show();
                            }
                        });
                builder.show();
            }
        }
    }

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


    // This class is used to change the rectangle border color when text is added
    private class GenericTextWatcher implements TextWatcher {

        private View view;
        private GenericTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        public void afterTextChanged(Editable editable) {

            GradientDrawable myGrad = (GradientDrawable)view.getBackground();
            myGrad.setStroke(2, Color.BLUE);

        }
    }

    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case PSoCCapSenseLedService.ACTION_BLESCAN_CALLBACK:
                    // Disable the search button and enable the connect button
                    search_button.setEnabled(false);
                    connect_button.setEnabled(true);
                    break;

                case PSoCCapSenseLedService.ACTION_CONNECTED:
                    /* This if statement is needed because we sometimes get a GATT_CONNECTED */
                    /* action when sending Capsense notifications */
                    if (!mConnectState) {
                        // Dsable the connect button, enable the discover services and disconnect buttons
                        connect_button.setEnabled(false);
                        discover_button.setEnabled(true);
                        disconnect_button.setEnabled(true);
                        mConnectState = true;
                        Log.d(TAG, "Connected to Device");
                    }
                    break;
                case PSoCCapSenseLedService.ACTION_DISCONNECTED:
                    // Disable the disconnect, discover svc, discover char button, and enable the search button
                    disconnect_button.setEnabled(false);
                    discover_button.setEnabled(false);
                    search_button.setEnabled(true);
                    // Turn off and disable the LED and CapSense switches
                    led_switch.setChecked(false);
                    led_switch.setEnabled(false);
                    cap_switch.setChecked(false);
                    cap_switch.setEnabled(false);
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    break;
                case PSoCCapSenseLedService.ACTION_SERVICES_DISCOVERED:
                    // Disable the discover services button
                    discover_button.setEnabled(false);
                    // Enable the LED and CapSense switches
                    led_switch.setEnabled(true);
                    cap_switch.setEnabled(true);
                    Log.d(TAG, "Services Discovered");
                    break;
                case PSoCCapSenseLedService.ACTION_DATA_RECEIVED:
                    // This is called after a notify or a read completes
                    // Check LED switch Setting
                    if(mPSoCCapSenseLedService.getLedSwitchState()){
                        led_switch.setChecked(true);
                    } else {
                        led_switch.setChecked(false);
                    }
                    // Get CapSense Slider Value
                    String CapSensePos = mPSoCCapSenseLedService.getCapSenseValue();
                    if (CapSensePos.equals("-1")) {  // No Touch returns 0xFFFF which is -1
                        if(!CapSenseNotifyState) { // Notifications are off
                            mCapsenseValue.setText(R.string.NotifyOff);
                        } else { // Notifications are on but there is no finger on the slider
                            mCapsenseValue.setText(R.string.NoTouch);
                        }
                    } else { // Valid CapSense value is returned
                        mCapsenseValue.setText(CapSensePos);
                    }
                default:
                    break;
            }
        }
    };

}

