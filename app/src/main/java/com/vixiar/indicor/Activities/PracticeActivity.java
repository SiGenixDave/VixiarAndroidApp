package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.vixiar.indicor.BLE_Interface.IndicorConnection;
import com.vixiar.indicor.BLE_Interface.IndicorDataInterface;
import com.vixiar.indicor.Graphics.PracticePressureGraph;
import com.vixiar.indicor.R;

import static android.content.ContentValues.TAG;

public class PracticeActivity extends Activity implements IndicorDataInterface
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

        IndicorConnection.getInstance().initialize(this, this);
        IndicorConnection.getInstance().ConnectToIndicor();
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

    public void iConnected()
    {
        Log.i(TAG, "Connected");
    }

    public void iCharacteristicRead(Object o)
    {
    }

    public void iDisconnected()
    {
        // for now, just leave the activity
        onBackPressed();
    }

    public void iError(int e)
    {
        // for now, just leave the activity
        onBackPressed();
    }

    public void iNotify()
    {
        int currentIndex = IndicorConnection.getInstance().GetData().GetData().size();
        pvg.setBallPressure((float)IndicorConnection.getInstance().GetData().GetData().get(currentIndex-1).m_pressure);
    }
}
