package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.vixiar.indicor.R;

public class QuestionnaireActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);
        SetFontFamily();
    }

    public void SetFontFamily()
    {
        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView v = findViewById(R.id.lblQ1);
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

}
