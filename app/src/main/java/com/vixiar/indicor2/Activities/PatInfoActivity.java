package com.vixiar.indicor2.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.vixiar.indicor2.BLEInterface.IndicorBLEServiceInterface;
import com.vixiar.indicor2.BLEInterface.IndicorBLEServiceInterfaceCallbacks;
import com.vixiar.indicor2.CustomDialog.CustomAlertDialog;
import com.vixiar.indicor2.CustomDialog.CustomDialogInterface;
import com.vixiar.indicor2.Data.PatientInfo;
import com.vixiar.indicor2.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PatInfoActivity extends Activity implements CustomDialogInterface, IndicorBLEServiceInterfaceCallbacks
{
    // TAG is used for informational messages
    private final static String TAG = PatInfoActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private EditText txtPatientID;
    private EditText txtAge;
    private EditText txtHeight;
    private EditText txtWeight;
    private EditText txtSystolic;
    private EditText txtDiastolic;
    private EditText txtGender;
    private EditText txtNotes;

    private NumberPicker npAge;
    private NumberPicker npHeightFeet;
    private NumberPicker npHeightInches;
    private NumberPicker npWeight;
    private NumberPicker npGender;

    // limits for input fields
    final int AGE_MIN = 18;
    final int AGE_MAX = 110;
    final int AGE_START = 50;
    final int HEIGHT_FT_MIN = 3;
    final int HEIGHT_FT_MAX = 7;
    final int HEIGHT_FT_START = 5;
    final int HEIGHT_IN_MIN = 0;
    final int HEIGHT_IN_MAX = 11;
    final int HEIGHT_IN_START = 8;
    final int WEIGHT_MIN = 88;
    final int WEIGHT_MAX = 500;
    final int SYSTOLIC_MIN = 90;
    final int SYSTOLIC_MAX = 200;
    final int MIN_DIASTOLIC = 30;
    final int MAX_DIASTOLIC = 150;


    private String[] genderString;

    private final int DLG_ID_CANCEL = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pat_info);
        SetFontFamily();
        InitializeHeaderAndFooter();
        initializeControls();

        if (DEBUG)
        {
            HeaderFooterControl.getInstance().UnDimNextButton(PatInfoActivity.this);
            HeaderFooterControl.getInstance().UnDimPracticeButton(PatInfoActivity.this);
            HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.continue_practice_or_test));
        }
        else
        {
            HeaderFooterControl.getInstance().DimNextButton(PatInfoActivity.this);
            HeaderFooterControl.getInstance().DimPracticeButton(PatInfoActivity.this);
            HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.complete_data_entry));
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // store the data entered to the patient class
        // PATIENT ID
        PatientInfo.getInstance().set_patientId(txtPatientID.getText().toString());

        // AGE
        try
        {
            PatientInfo.getInstance().set_age_years(Integer.parseInt(txtAge.getText().toString()));
        } catch (NumberFormatException e)
        {
            PatientInfo.getInstance().set_age_years(0);
        }

        // HEIGHT
        try
        {
            // split the height up into ft and inches
            String[] splitHeight = txtHeight.getText().toString().split("/");

            // get rid of the whitespace
            String ft = splitHeight[0].replaceAll("\\s", "");
            String in = splitHeight[1].replaceAll("\\s", "");

            // convert to an int
            int height_ft = Integer.parseInt(ft);
            int height_in = Integer.parseInt(in);
            PatientInfo.getInstance().set_height_Inches(height_ft * 12 + height_in);
        } catch (NumberFormatException e)
        {

            PatientInfo.getInstance().set_height_Inches(0);
        } catch (ArrayIndexOutOfBoundsException e)
        {
            PatientInfo.getInstance().set_height_Inches(0);
        }

        // WEIGHT
        try
        {
            PatientInfo.getInstance().set_weight_lbs(Integer.parseInt(txtWeight.getText().toString()));
        } catch (NumberFormatException e)
        {
            PatientInfo.getInstance().set_weight_lbs(0);
        }

        // SYSTOLIC PRESSURE
        try
        {
            PatientInfo.getInstance().set_systolicBloodPressure(Integer.parseInt(txtSystolic.getText().toString()));
        } catch (NumberFormatException e)
        {
            PatientInfo.getInstance().set_systolicBloodPressure(0);
        }

        // GENDER
        PatientInfo.getInstance().set_gender(txtGender.getText().toString());

        // DIASTOLIC PRESSURE
        try
        {
            PatientInfo.getInstance().set_diastolicBloodPressure(Integer.parseInt(txtDiastolic.getText().toString()));
        } catch (NumberFormatException e)
        {
            PatientInfo.getInstance().set_diastolicBloodPressure(0);
        }

        // NOTES
        PatientInfo.getInstance().set_notes(txtNotes.getText().toString());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Button et = findViewById(R.id.navButton);
        et.setFocusableInTouchMode(true);
        et.requestFocus();

        IndicorBLEServiceInterface.getInstance().initialize(this, this );
    }

    @Override
    public void onBackPressed()
    {
        HandleRequestToCancel();
    }

    private void initializeControls()
    {
        // TODO: theres some kind of divide by 0 when this runs
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
                    showKeyBoard(v);
                    txtPatientID.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                }
                else
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
        txtAge = (EditText) findViewById(R.id.txtAge);
        txtAge.addTextChangedListener(fieldChangeWatcher);
        txtAge.setShowSoftInputOnFocus(false);
        txtAge.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    hideKeyBoard(v);
                    txtAge.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npAge.setVisibility(View.VISIBLE);
                    txtAge.setText(String.valueOf(npAge.getValue()));
                }
                else
                {
                    txtAge.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    npAge.setVisibility(View.GONE);
                }
            }
        });

        NumberPicker.OnValueChangeListener ageChangeListener = new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                txtAge.setText(String.valueOf(npAge.getValue()));
            }
        };

        npAge = (NumberPicker) findViewById(R.id.npAge);
        npAge.setVisibility(View.GONE);
        npAge.setMinValue(AGE_MIN);
        npAge.setMaxValue(AGE_MAX);
        npAge.setValue(AGE_START);
        npAge.setOnValueChangedListener(ageChangeListener);

        // make sure a touch anywhere in the area sets the focus to the edittext
        LinearLayout ageGroup = findViewById(R.id.ageGroup);
        ageGroup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                txtAge.requestFocus();
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

                    hideKeyBoard(v);
                    txtHeight.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                    npHeightFeet.setVisibility(View.VISIBLE);
                    npHeightInches.setVisibility(View.VISIBLE);
                    int nFeet = npHeightFeet.getValue();
                    int nInches = npHeightInches.getValue();
                    txtHeight.setText(String.valueOf(nFeet) + " / " + String.valueOf(nInches));
                }
                else
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
        npHeightFeet.setMinValue(HEIGHT_FT_MIN);
        npHeightFeet.setMaxValue(HEIGHT_FT_MAX);
        npHeightFeet.setOnValueChangedListener(heightChangeListener);
        npHeightFeet.setValue(HEIGHT_FT_START);

        npHeightInches = (NumberPicker) findViewById(R.id.npInches);
        npHeightInches.setVisibility(View.GONE);
        npHeightInches.setMinValue(HEIGHT_IN_MIN);
        npHeightInches.setMaxValue(HEIGHT_IN_MAX);
        npHeightInches.setOnValueChangedListener(heightChangeListener);
        npHeightInches.setValue(HEIGHT_IN_START);

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
        txtWeight.setShowSoftInputOnFocus(true);
        txtWeight.setFilters(new InputFilter[]{new InputFilterMinMax(WEIGHT_MIN >= 10 ? "0" : String.valueOf(WEIGHT_MIN), WEIGHT_MAX > -10 ? String.valueOf(WEIGHT_MAX) : "0")});
        txtWeight.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    showKeyBoard(v);
                    txtWeight.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                }
                else
                {
                    hideKeyBoard(v);
                    txtWeight.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    try
                    {
                        if (Integer.parseInt(txtWeight.getText().toString()) > WEIGHT_MAX)
                        {
                            txtWeight.setText(String.valueOf(WEIGHT_MAX));
                        }
                        if (Integer.parseInt(txtWeight.getText().toString()) < WEIGHT_MIN)
                        {
                            txtWeight.setText(String.valueOf(WEIGHT_MIN));
                        }
                    } catch (NumberFormatException e)
                    {

                    }
                }
            }
        });

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
        txtSystolic.setShowSoftInputOnFocus(true);
        txtSystolic.setFilters(new InputFilter[]{new InputFilterMinMax(SYSTOLIC_MIN >= 10 ? "0" : String.valueOf(SYSTOLIC_MIN), SYSTOLIC_MAX > -10 ? String.valueOf(SYSTOLIC_MAX) : "0")});
        txtSystolic.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    showKeyBoard(v);
                    txtSystolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                }
                else
                {
                    hideKeyBoard(v);
                    txtSystolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    try
                    {
                        if (Integer.parseInt(txtSystolic.getText().toString()) > SYSTOLIC_MAX)
                        {
                            txtSystolic.setText(String.valueOf(SYSTOLIC_MAX));
                        }
                        if (Integer.parseInt(txtSystolic.getText().toString()) < SYSTOLIC_MIN)
                        {
                            txtSystolic.setText(String.valueOf(SYSTOLIC_MIN));
                        }
                    } catch (NumberFormatException e)
                    {

                    }
                }
            }
        });

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
        txtDiastolic.setShowSoftInputOnFocus(true);
        txtDiastolic.setFilters(new InputFilter[]{new InputFilterMinMax(MIN_DIASTOLIC >= 0 ? "0" : String.valueOf(SYSTOLIC_MIN), MAX_DIASTOLIC > -10 ? String.valueOf(MAX_DIASTOLIC) : "0")});

        txtDiastolic.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    showKeyBoard(v);
                    txtDiastolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                }
                else
                {
                    hideKeyBoard(v);
                    txtDiastolic.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryNormalValue));
                    try
                    {
                        if (Integer.parseInt(txtDiastolic.getText().toString()) > MAX_DIASTOLIC)
                        {
                            txtDiastolic.setText(String.valueOf(MAX_DIASTOLIC));
                        }
                        if (Integer.parseInt(txtDiastolic.getText().toString()) < MIN_DIASTOLIC)
                        {
                            txtDiastolic.setText(String.valueOf(MIN_DIASTOLIC));
                        }
                    } catch (NumberFormatException e)
                    {

                    }
                }
            }
        });

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
                }
                else
                {
                    hideKeyBoard(v);
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

        npGender = findViewById(R.id.npGender);
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

        // TODO: See if this really works
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
                    showKeyBoard(v);
                    txtNotes.setTextColor(ContextCompat.getColor(PatInfoActivity.this, R.color.colorPatientEntryHighlightedValue));
                }
                else
                {
                    hideKeyBoard(v);
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

    public void showKeyBoard(View v)
    {
        if (v != null)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
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
            if (!DEBUG)
            {
                 if (txtPatientID.getText().toString().length() != 0 && txtAge.getText().toString().length() != 0 &&
                        txtHeight.getText().toString().length() != 0 && txtWeight.getText().toString().length() != 0 &&
                        txtDiastolic.getText().toString().length() != 0 && txtSystolic.getText().toString().length() != 0 &&
                        txtGender.getText().toString().length() != 0)
                {
                    HeaderFooterControl.getInstance().UnDimNextButton(PatInfoActivity.this);
                    HeaderFooterControl.getInstance().UnDimPracticeButton(PatInfoActivity.this);
                    HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.continue_practice_or_test));
                }
                else
                {
                    HeaderFooterControl.getInstance().DimNextButton(PatInfoActivity.this);
                    HeaderFooterControl.getInstance().DimPracticeButton(PatInfoActivity.this);
                    HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.complete_data_entry));
                }
            }
            else
            {
                HeaderFooterControl.getInstance().UnDimNextButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().UnDimPracticeButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.continue_practice_or_test));
            }
        }
    };

    public void SetFontFamily()
    {
        Typeface robotoTypeface = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");

        TextView v = (TextView) findViewById(R.id.patIDLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.dobLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.heightLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.ftinLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.weightLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.lbsLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.diasLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.mmhg1Lbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.systLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.mmhg2Lbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.genderLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.notesLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtPatientID);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtAge);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtHeight);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtWeight);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtDiast);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtSystolic);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtGender);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.txtNotes);
        v.setTypeface(robotoTypeface);
    }

    private void InitializeHeaderAndFooter()
    {
        HeaderFooterControl.getInstance().SetTypefaces(this, this);
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.pat_info_screen_title));
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.complete_data_entry));
        HeaderFooterControl.getInstance().DimNextButton(this);
        HeaderFooterControl.getInstance().DimPracticeButton(this);
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                HandleRequestToCancel();
            }
        });
        HeaderFooterControl.getInstance().SetNextButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                PatientInfo.getInstance().getRealtimeData().ClearAllData();
                Intent intent = new Intent(PatInfoActivity.this, TestingActivity.class);
                startActivity(intent);
            }
        });
        HeaderFooterControl.getInstance().SetPracticeButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // clear all of the realtime data
                PatientInfo.getInstance().getRealtimeData().ClearAllData();
                Intent intent = new Intent(PatInfoActivity.this, PracticeActivity.class);
                startActivity(intent);
            }
        });
    }

    public void HandleRequestToCancel()
    {
        // display the test cancel dialog
        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                getString(R.string.dlg_title_cancel_test),
                getString(R.string.dlg_msg_cancel_test),
                "Yes",
                "No", PatInfoActivity.this , DLG_ID_CANCEL, PatInfoActivity.this);
    }

    @Override
    public void onClickPositiveButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
            case DLG_ID_CANCEL:
                Intent main = new Intent(PatInfoActivity.this, MainActivity.class);
                navigateUpTo(main);
                break;
        }
    }

    @Override
    public void onClickNegativeButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
        }
    }

    @Override
    public void iRestart()
    {
        // Intentionally do nothing, needed to support connection errors when app is connected
        // to hand held
    }

    @Override
    public void iError(int e)
    {
        // Intentionally do nothing, needed to support connection errors when app is connected
        // to hand held
    }

    @Override
    public void iRealtimeDataNotification()
    {
        // Intentionally do nothing, needed to support connection errors when app is connected
        // to hand held
    }

    @Override
    public void iFullyConnected()
    {
        // Intentionally do nothing, needed to support connection errors when app is connected
        // to hand held
    }

    @Override
    public void iDisconnected()
    {
        // Intentionally do nothing, needed to support connection errors when app is connected
        // to hand held
    }

    @Override
    public void iBatteryLevelRead(int level)
    {
        // Intentionally do nothing, needed to support connection errors when app is connected
        // to hand held
    }
}
