package com.vixiar.indicor.Activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.TextView;

import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.R;

public class RemindersActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);
        SetFontFamily();
        InitializeHeaderAndFooter();
    }

    public void SetFontFamily()
    {
        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView v = findViewById(R.id.lblR1);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblR2);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblR3);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblR4);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblR5);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblR6);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblR7);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblR8);
        v.setTypeface(robotoTypeface);
    }


    public void InitializeHeaderAndFooter()
    {
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.title_activity_reminders));
        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.complete_data_entry));
        HeaderFooterControl.getInstance().DimPracticeButton(this);

        HeaderFooterControl.getInstance().SetNextButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Intent intent = new Intent(QuestionnaireActivity.this, TestingActivity.class);
                //startActivity(intent);
            }
        });

        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //HandleRequestToCancel();
            }
        });

        HeaderFooterControl.getInstance().HidePracticeButton(this);

    }

}
