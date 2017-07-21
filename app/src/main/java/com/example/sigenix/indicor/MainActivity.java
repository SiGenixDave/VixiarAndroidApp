package com.example.sigenix.indicor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity  {

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

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        //Implement image click function
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


}

