package com.vixiar.indicor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class PracticeActivity extends Activity
{
    PracticePressureGraph pvg;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        pvg = findViewById(R.id.practicePressureGraph);
        pvg.setBallPressure((float)0.0);

        InitializeHeaderAndFooter();
    }

    private void InitializeHeaderAndFooter()
    {
        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.practice_screen_title));
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.keep_ball));
        HeaderFooterControl.getInstance().UnDimNextButton(this);
        HeaderFooterControl.getInstance().HidePracticeButton(this);
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });
        HeaderFooterControl.getInstance().SetNextButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(PracticeActivity.this, TestingActivity.class);
                startActivity(intent);
            }
        });
    }

}
