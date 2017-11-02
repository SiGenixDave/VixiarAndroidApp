package com.vixiar.indicor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    private EditText txtHeight;
    private EditText txtWeight;
    private EditText txtSystolic;
    private EditText txtDiastolic;
    private EditText txtGender;
    private EditText txtNotes;
    private TextView txtMessage;

    private NumberPicker npDOBMonth;
    private NumberPicker npDOBDay;
    private NumberPicker npDOBYear;
    private NumberPicker npHeightFeet;
    private NumberPicker npHeightInches;
    private NumberPicker npWeight;
    private NumberPicker npSystolic;
    private NumberPicker npDiastolic;
    private NumberPicker npGender;

    private String[] monthString;
    private String[] genderString;

    private ImageButton btnStartTest;

    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pat_info);
        initializeControls();

        ImageButton btnExit = (ImageButton) findViewById(R.id.exitButton);
        btnExit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder adb = new AlertDialog.Builder(PatInfoActivity.this);
                adb.setTitle(getString(R.string.exit_confirmation_title));
                adb.setMessage(getString(R.string.exit_confirmation_message));
                adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        System.exit(0);
                    }
                });
                adb.setNegativeButton(android.R.string.cancel, null);
                adb.show();
            }
        });

        ImageButton btnBack = (ImageButton) findViewById(R.id.backButton);
        btnBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent main = new Intent(PatInfoActivity.this, MainActivity.class);
                navigateUpTo(main);
            }
        });

        btnStartTest = (ImageButton) findViewById(R.id.startTestButton);
        btnStartTest.setEnabled(false);
        btnStartTest.setAlpha((float)0.5);
        btnStartTest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Start test screen coming soon",
                        Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();            }
        });

        ImageButton btnPractice = (ImageButton) findViewById(R.id.practiceButton);
        btnPractice.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(PatInfoActivity.this, PracticeActivity.class);
                startActivity(intent);
            }
        });


        txtMessage = (TextView) findViewById(R.id.txtMessage);
        txtMessage.setText(getString(R.string.complete_data_entry));

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
        // --------------------------------------------------------------------------------------------------------
        // patient ID
        txtPatientID = (EditText) findViewById(R.id.txtPatientID);
        txtPatientID.addTextChangedListener(fieldChangeWatcher);
        txtPatientID.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtPatientID.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                } else
                {
                    txtPatientID.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    hideKeyBoard(v);
                }
            }
        });

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout patIDGroup = (LinearLayout) findViewById(R.id.patientIDGroup);
        patIDGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtPatientID.requestFocus();
            }
        });

        // --------------------------------------------------------------------------------------------------------
        // DOB
        txtDOB = (EditText) findViewById(R.id.txtDOB);
        txtDOB.addTextChangedListener(fieldChangeWatcher);
        txtDOB.setShowSoftInputOnFocus(false);
        txtDOB.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtDOB.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npDOBDay.setVisibility(View.VISIBLE);
                    npDOBMonth.setVisibility(View.VISIBLE);
                    npDOBYear.setVisibility(View.VISIBLE);
                    int nMonth = npDOBMonth.getValue();
                    int nDay = npDOBDay.getValue();
                    int nYear = npDOBYear.getValue();
                    txtDOB.setText(monthString[nMonth] + " " + String.valueOf(nDay) + " " + String.valueOf(nYear));
                } else
                {
                    txtDOB.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
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
        npDOBMonth.setFormatter(new NumberPicker.Formatter()
        {
            @Override
            public String format(int value)
            {
                return monthString[value];
            }
        });
        // this code fixes bug in numberpicker where when first opening a numberpicker with a formatter,
        // the string is not shown until the picker is touched
        try
        {
            Method method = npDOBMonth.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npDOBMonth, true);
        } catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        } catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        } catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
        npDOBMonth.setMinValue(0);
        npDOBMonth.setMaxValue(monthString.length - 1);
        npDOBMonth.setVisibility(View.GONE);
        npDOBMonth.setOnValueChangedListener(dobChangeListener);

        npDOBYear = (NumberPicker) findViewById(R.id.npDOBYear);
        npDOBYear.setVisibility(View.GONE);
        npDOBYear.setMinValue(1930);
        npDOBYear.setMaxValue(2017);
        npDOBYear.setValue(1960);
        npDOBYear.setOnValueChangedListener(dobChangeListener);

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout DOBGroup = (LinearLayout) findViewById(R.id.DOBGroup);
        DOBGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtDOB.requestFocus();
            }
        });
        // --------------------------------------------------------------------------------------------------------
        // Height
        txtHeight = (EditText) findViewById(R.id.txtHeight);
        txtHeight.addTextChangedListener(fieldChangeWatcher);
        txtHeight.setShowSoftInputOnFocus(false);
        txtHeight.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtHeight.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npHeightFeet.setVisibility(View.VISIBLE);
                    npHeightInches.setVisibility(View.VISIBLE);
                    int nFeet = npHeightFeet.getValue();
                    int nInches = npHeightInches.getValue();
                    txtHeight.setText(String.valueOf(nFeet) + " / " + String.valueOf(nInches));
                } else
                {
                    txtHeight.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    npHeightFeet.setVisibility(View.GONE);
                    npHeightInches.setVisibility(View.GONE);
                }
            }
        });

        NumberPicker.OnValueChangeListener heightChangeListener = new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                int nFeet = npHeightFeet.getValue();
                int nInches = npHeightInches.getValue();
                txtHeight.setText(String.valueOf(nFeet) + " / " + String.valueOf(nInches));
            }
        };

        npHeightFeet = (NumberPicker) findViewById(R.id.npFeet);
        npHeightFeet.setVisibility(View.GONE);
        npHeightFeet.setMinValue(3);
        npHeightFeet.setMaxValue(7);
        npHeightFeet.setOnValueChangedListener(heightChangeListener);
        npHeightFeet.setValue(5);

        npHeightInches = (NumberPicker) findViewById(R.id.npInches);
        npHeightInches.setVisibility(View.GONE);
        npHeightInches.setMinValue(0);
        npHeightInches.setMaxValue(11);
        npHeightInches.setOnValueChangedListener(heightChangeListener);
        npHeightInches.setValue(8);

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout heightGroup = (LinearLayout) findViewById(R.id.heightGroup);
        heightGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtHeight.requestFocus();
            }
        });
// --------------------------------------------------------------------------------------------------------
        // Weight
        txtWeight = (EditText) findViewById(R.id.txtWeight);
        txtWeight.addTextChangedListener(fieldChangeWatcher);
        txtWeight.setShowSoftInputOnFocus(false);
        txtWeight.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtWeight.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npWeight.setVisibility(View.VISIBLE);
                    int nWeight = npWeight.getValue();
                    txtWeight.setText(String.valueOf(nWeight));
                } else
                {
                    txtWeight.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    npWeight.setVisibility(View.GONE);
                }
            }
        });

        NumberPicker.OnValueChangeListener weightChangeListener = new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                int nWeight = npWeight.getValue();
                txtWeight.setText(String.valueOf(nWeight));
            }
        };

        npWeight = (NumberPicker) findViewById(R.id.npWeight);
        npWeight.setVisibility(View.GONE);
        npWeight.setMinValue(88);
        npWeight.setMaxValue(250);
        npWeight.setOnValueChangedListener(weightChangeListener);
        npWeight.setValue(150);

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout weightGroup = (LinearLayout) findViewById(R.id.weightGroup);
        weightGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtWeight.requestFocus();
            }
        });
        // --------------------------------------------------------------------------------------------------------
        // Systolic
        txtSystolic = (EditText) findViewById(R.id.txtSystolic);
        txtSystolic.addTextChangedListener(fieldChangeWatcher);
        txtSystolic.setShowSoftInputOnFocus(false);
        txtSystolic.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtSystolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npSystolic.setVisibility(View.VISIBLE);
                    int nDiastolic = npSystolic.getValue();
                    txtSystolic.setText(String.valueOf(nDiastolic));
                } else
                {
                    txtSystolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    npSystolic.setVisibility(View.GONE);
                }
            }
        });

        NumberPicker.OnValueChangeListener systolicChangeListener = new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                int nDiastolic = npSystolic.getValue();
                txtSystolic.setText(String.valueOf(nDiastolic));
            }
        };

        npSystolic = (NumberPicker) findViewById(R.id.npSystolic);
        npSystolic.setVisibility(View.GONE);
        npSystolic.setMinValue(90);
        npSystolic.setMaxValue(160);
        npSystolic.setOnValueChangedListener(systolicChangeListener);
        npSystolic.setValue(120);

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout systolicGroup = (LinearLayout) findViewById(R.id.systolicGroup);
        systolicGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtSystolic.requestFocus();
            }
        });
        // --------------------------------------------------------------------------------------------------------
        // Diastolic
        txtDiastolic = (EditText) findViewById(R.id.txtDiast);
        txtDiastolic.addTextChangedListener(fieldChangeWatcher);
        txtDiastolic.setShowSoftInputOnFocus(false);
        txtDiastolic.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtDiastolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npDiastolic.setVisibility(View.VISIBLE);
                    int nDiastolic = npDiastolic.getValue();
                    txtDiastolic.setText(String.valueOf(nDiastolic));
                } else
                {
                    txtDiastolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    npDiastolic.setVisibility(View.GONE);
                }
            }
        });

        NumberPicker.OnValueChangeListener diastolicChangeListener = new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                int nDiastolic = npDiastolic.getValue();
                txtDiastolic.setText(String.valueOf(nDiastolic));
            }
        };

        npDiastolic = (NumberPicker) findViewById(R.id.npDiastolic);
        npDiastolic.setVisibility(View.GONE);
        npDiastolic.setMinValue(80);
        npDiastolic.setMaxValue(100);
        npDiastolic.setOnValueChangedListener(diastolicChangeListener);
        npDiastolic.setValue(80);

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout diastolicGroup = (LinearLayout) findViewById(R.id.diastolicGroup);
        diastolicGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtDiastolic.requestFocus();
            }
        });
        // --------------------------------------------------------------------------------------------------------
        // Gender
        txtGender = (EditText) findViewById(R.id.txtGender);
        txtGender.addTextChangedListener(fieldChangeWatcher);
        txtGender.setShowSoftInputOnFocus(false);
        txtGender.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtGender.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npGender.setVisibility(View.VISIBLE);
                    int nGender = npGender.getValue();
                    txtGender.setText(genderString[nGender]);
                } else
                {
                    txtGender.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    npGender.setVisibility(View.GONE);
                }
            }
        });

        genderString = getResources().getStringArray(R.array.gender_array);

        final NumberPicker.OnValueChangeListener genderChangeListener = new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                int nGender = npGender.getValue();
                txtGender.setText(genderString[nGender]);
            }
        };

        npGender = (NumberPicker) findViewById(R.id.npGender);
        npGender.setVisibility(View.GONE);
        npGender.setMinValue(0);
        npGender.setMaxValue(genderString.length - 1);
        npGender.setFormatter(new NumberPicker.Formatter()
        {
            @Override
            public String format(int value)
            {
                return genderString[value];
            }
        });
        npGender.setOnValueChangedListener(genderChangeListener);
        // this code fixes bug in numberpicker where when first opening a numberpicker with a formatter,
        // the string is not shown until the picker is touched
        try
        {
            Method method = npGender.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npGender, true);
        } catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        } catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        } catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout genderGroup = (LinearLayout) findViewById(R.id.genderGroup);
        genderGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtGender.requestFocus();
            }
        });

        // --------------------------------------------------------------------------------------------------------
        // Notes
        txtNotes = (EditText) findViewById(R.id.txtNotes);
        txtNotes.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    txtNotes.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                } else
                {
                    txtNotes.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    hideKeyBoard(v);
                }
            }
        });

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout notesGroup = (LinearLayout) findViewById(R.id.notesGroup);
        notesGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtNotes.requestFocus();
            }
        });

    }

    public void hideKeyBoard(View v)
    {
        if (v != null)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    // this function gets called whenever any text fields change
    // it will enable the start test button once all the required fields are filled in
    private final TextWatcher fieldChangeWatcher = new TextWatcher()
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            if (txtPatientID.getText().toString().length() != 0 && txtDOB.getText().toString().length() != 0 &&
                    txtHeight.getText().toString().length() != 0 && txtWeight.getText().toString().length() != 0 &&
                    txtDiastolic.toString().trim().length() != 0 && txtSystolic.getText().toString().length() != 0 &&
                    txtGender.getText().toString().length() != 0)
            {
                btnStartTest.setEnabled(true);
                btnStartTest.setAlpha((float) 1.0);
                txtMessage.setText(getString(R.string.continue_practice_or_test));
            } else
            {
                btnStartTest.setEnabled(false);
                btnStartTest.setAlpha((float) 0.5);
                txtMessage.setText(getString(R.string.complete_data_entry));
            }
        }
    };
}
