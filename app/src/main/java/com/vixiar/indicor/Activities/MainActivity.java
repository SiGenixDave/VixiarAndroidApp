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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.vixiar.indicor.Application.NavigatorApplication;
import com.vixiar.indicor.BLEInterface.IndicorBLEServiceInterface;
import com.vixiar.indicor.BuildConfig;
import com.vixiar.indicor.CustomDialog.CustomAlertDialog;
import com.vixiar.indicor.CustomDialog.CustomDialogInterface;
import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.R;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends Activity implements CustomDialogInterface
{
    private final String TAG = this.getClass().getSimpleName();

    // These are local request code so you know when the result callback happens, which request it was from
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_STORAGE = 2;

    // this is a request code that is used if BLE isn't turned on...
    // it tells the response m_deviceCheckHandler to start the data collection intent if the user enables ble
    private static final int REQUEST_START_CONNECTION_BLE = 1;

    // dialog numbers so the callback m_deviceCheckHandler knows what's going on
    private static final int DLG_ID_NO_BLE = 0;
    private static final int DLG_ID_LOCATION_SERVICES = 1;
    private static final int DLG_ID_BLE_NOT_ENABLED = 2;
    private static final int DLG_ID_LOCATION_ACCESS_PRE = 3;
    private static final int DLG_ID_LOCATION_ACCESS_NOT_ENABLED = 4;
    private static final int DLG_ID_BLE_NOT_ENABLED_PRE = 5;
    private static final int DLG_ID_STORAGE_NOT_ENABLED_PRE = 6;
    private static final int DLG_ID_STORAGE_NOT_ENABLED = 7;


    Handler m_deviceCheckHandler = new Handler();
    final Runnable m_deviceCheckRunnable = new Runnable()
    {
        public void run()
        {
            checkDeviceSetup();
        }
    };

    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SetSiteName();
        DisplaySiteName();

        // setup the top bar
        HeaderFooterControl.getInstance().SetTypefaces(this, this);
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, "");
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.main_screen_title));

        // put the version number into the bottom status bar
        String nameAndVersion = getString(R.string.main_screen_title) +
                " V" + BuildConfig.VERSION_NAME;

        Typeface robotoTypeface = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");

        TextView versionText = (TextView) findViewById(R.id.txtViewVersion);
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

        // dim the video button
        ImageView iv = findViewById(R.id.trainingVideoButton);
        iv.setAlpha((float) 0.3);
        TextView tv = findViewById(R.id.trainingLbl);
        tv.setAlpha((float) 0.3);

        // wait a couple seconds then check the device for the proper configuration and settings
        m_deviceCheckHandler.postDelayed(m_deviceCheckRunnable, 2000);

        // make sure we're disconnected from the device
        IndicorBLEServiceInterface.getInstance().DisconnectFromIndicor();
    }

    @Override
    public void onBackPressed()
    {
    }

    private void SetSiteName()
    {
        // get the site name from the settings
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences (NavigatorApplication.getAppContext ());
        String siteFolder = sp.getString ("study_location", "Vixiar_Internal-Testing");

        if (siteFolder.equals("Stony_Brook-Training"))
        {
            PatientInfo.getInstance().set_studyLocation("Stony Brook Training Set");
        }
        else if (siteFolder.equals("Stony_Brook-Validation"))
        {
            PatientInfo.getInstance().set_studyLocation("Stony Brook Validation Study");
        }
        else if (siteFolder.equals("JHU-Diuresis"))
        {
            PatientInfo.getInstance().set_studyLocation("JHU Diuresis Study");
        }
        else if (siteFolder.equals("JHU-Nephrology"))
        {
            PatientInfo.getInstance().set_studyLocation("JHU Nephrology Study");
        }
        else
        {
            PatientInfo.getInstance().set_studyLocation("Vixiar Testing");
        }
    }

    private void DisplaySiteName()
    {
        TextView tv = findViewById(R.id.txtStudyLocation);
        tv.setText(PatientInfo.getInstance().get_studyLocation());
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
                    Log.d(TAG, "Coarse location permission granted");
                    if (!doesAppHaveStoragePermission())
                    {
                        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                                getString(R.string.dlg_title_storage_access_pre),
                                getString(R.string.dlg_msg_storage_access_pre),
                                "Ok",
                                null,
                                this, DLG_ID_STORAGE_NOT_ENABLED_PRE, this);
                    }
                }
                else
                {
                    CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                            getString(R.string.dlg_title_location_access_no),
                            getString(R.string.dlg_msg_location_access_no),
                            "Ok",
                            null,
                            this, DLG_ID_LOCATION_ACCESS_NOT_ENABLED, this);
                }
            }

            case PERMISSION_REQUEST_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Storage location permission granted");
                }
                else
                {
                    CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                            getString(R.string.dlg_title_storage_access_no),
                            getString(R.string.dlg_msg_storage_access_no),
                            "Ok",
                            null,
                            this, DLG_ID_STORAGE_NOT_ENABLED, this);
                }
                break;
        }
    }


    // quick stuff to check that BLE is supported and turned on
    private BluetoothAdapter GetAdapter()
    {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();
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
                        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                                getString(R.string.dlg_title_location_access_pre),
                                getString(R.string.dlg_msg_location_access_pre),
                                "Ok",
                                null,
                                this, DLG_ID_LOCATION_ACCESS_PRE, this);
                    }
                } //End of section for Android 6.0 (Marshmallow)
            }
            else
            {
                Log.i(TAG, "BLE was not enabled");
                CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                        getString(R.string.dlg_title_ble_not_enabled),
                        getString(R.string.dlg_msg_ble_not_enabled),
                        "Ok",
                        null,
                        this, DLG_ID_BLE_NOT_ENABLED, this);
            }
        }
    }

    public void onStartClick(View view)
    {
        PatientInfo.getInstance().Initialize();
        Intent intent = new Intent(this, PatInfoActivity.class);
        startActivity(intent);
    }

    public void onSettingsClick(View view)
    {
        LayoutInflater li = LayoutInflater.from(this);
        View passwordDialogView = li.inflate(R.layout.password_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        alertDialogBuilder.setView(passwordDialogView);

        final EditText passwordEntered = (EditText) passwordDialogView
                .findViewById(R.id.txtPasswordDialogPassword);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                Log.d(TAG, passwordEntered.getText().toString());
                                if (passwordEntered.getText().toString().equals("v1x1ar"))
                                {
                                    Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                                    startActivity(settingsIntent);
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void onManualClick(View view)
    {
        Intent intent = new Intent(this, PDFViewActivity.class);
        startActivity(intent);
    }

    private void checkDeviceSetup()
    {
        if (deviceSupportsBLE())
        {
            if (locationServicesOn())
            {
                if (isBLEEnabled())
                {
                    if (doesAppHaveLocationPermission())
                    {
                        if (!doesAppHaveStoragePermission())
                        {
                            CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                                    getString(R.string.dlg_title_storage_access_pre),
                                    getString(R.string.dlg_msg_storage_access_pre),
                                    "Ok",
                                    null,
                                    this, DLG_ID_STORAGE_NOT_ENABLED_PRE, this);
                        }
                    }
                    else
                    {

                        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                                getString(R.string.dlg_title_location_access_pre),
                                getString(R.string.dlg_msg_location_access_pre),
                                "Ok",
                                null,
                                this, DLG_ID_LOCATION_ACCESS_PRE, this);
                    }
                }
                else
                {
                    // If BLE is not enabled, put up a dialog alerting the user to enable it
                    // In the ok m_deviceCheckHandler for the dialog, the intent to enable BLE will be fired
                    CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                            getString(R.string.dlg_title_ble_enable_pre),
                            getString(R.string.dlg_msg_ble_enable_pre),
                            "Ok",
                            null,
                            this, DLG_ID_BLE_NOT_ENABLED_PRE, this);
                }
            }
            else
            {
                // location services are off, inform the user what to do
                CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                        getString(R.string.dlg_title_location_service),
                        getString(R.string.dlg_msg_location_service),
                        "Ok",
                        null, this, DLG_ID_LOCATION_SERVICES, this);
            }
        }
        else
        {
            // device does not support BLE, inform the user
            CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 1,
                    getString(R.string.dlg_title_ble_not_supported),
                    getString(R.string.dlg_msg_ble_not_supported),
                    "Ok",
                    null,
                    this, DLG_ID_NO_BLE, this);
        }
    }

    private boolean locationServicesOn()
    {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager != null)
        {
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        else
        {
            return false;
        }
    }

    private boolean deviceSupportsBLE()
    {
        return GetAdapter() != null;
    }

    public boolean isBLEEnabled()
    {
        BluetoothAdapter adapter = GetAdapter();
        if (adapter != null)
        {
            return adapter.isEnabled();
        }
        else
        {
            return false;
        }
    }

    private boolean doesAppHaveLocationPermission()
    {
        // Check for location access
        return (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean doesAppHaveStoragePermission()
    {
        // Check for location access
        return (this.checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onClickPositiveButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
            case DLG_ID_LOCATION_SERVICES:
            case DLG_ID_NO_BLE:
            case DLG_ID_BLE_NOT_ENABLED:
            case DLG_ID_LOCATION_ACCESS_NOT_ENABLED:
            case DLG_ID_STORAGE_NOT_ENABLED:
                finish();
                System.exit(0);
                break;

            case DLG_ID_LOCATION_ACCESS_PRE:
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                break;

            case DLG_ID_BLE_NOT_ENABLED_PRE:
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_START_CONNECTION_BLE);
                break;

            case DLG_ID_STORAGE_NOT_ENABLED_PRE:
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
                break;
        }
    }

    @Override
    public void onClickNegativeButton(DialogInterface dialog, int dialogID)
    {

    }
}
