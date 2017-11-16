package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.vixiar.indicor.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PatInfoActivity extends Activity
{
    // TAG is used for informational messages
    private final static String TAG = PatInfoActivity.class.getSimpleName();

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
    private ImageButton btnPractice;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pat_info);
        SetFontFamily();
        InitializeHeaderAndFooter();
        initializeControls();

        HeaderFooterControl.getInstance().UnDimNextButton(PatInfoActivity.this);
        HeaderFooterControl.getInstance().UnDimPracticeButton(PatInfoActivity.this);
        HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.continue_practice_or_test));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Button et = findViewById(R.id.navButton);
        //et.setFocusable(true);
        et.setFocusableInTouchMode(true );
        et.requestFocus();
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
                HeaderFooterControl.getInstance().UnDimNextButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().UnDimPracticeButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.continue_practice_or_test));
            } else
            {
                HeaderFooterControl.getInstance().UnDimNextButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().UnDimPracticeButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.continue_practice_or_test));
/*
                HeaderFooterControl.getInstance().DimNextButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().DimPracticeButton(PatInfoActivity.this);
                HeaderFooterControl.getInstance().SetBottomMessage(PatInfoActivity.this, getString(R.string.complete_data_entry));
*/
            }
        }
    };

    public void SetFontFamily()
    {
        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

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

        v = (TextView) findViewById(R.id.txtDOB);
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
        HeaderFooterControl.getInstance().SetTypefaces(this);
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
                Intent main = new Intent(PatInfoActivity.this, MainActivity.class);
                navigateUpTo(main);
            }
        });
        HeaderFooterControl.getInstance().SetNextButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(PatInfoActivity.this, TestingActivity.class);
                startActivity(intent);
            }
        });
        HeaderFooterControl.getInstance().SetPracticeButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(PatInfoActivity.this, PracticeActivity.class);
                startActivity(intent);
            }
        });
    }
}

