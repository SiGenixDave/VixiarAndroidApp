package com.vixiar.indicor;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class TestingActivity extends Activity implements IndicorDataInterface
{
    Handler m_10SecValsalvaCountdownHandler = new Handler();
    final Runnable m_10SecValsalvaCountdownRunnable = new Runnable()
    {
        public void run()
        {
            Countdown10SecondValsalvaUpdate();
        }
    };

    Handler m_5SecCountdownHandler = new Handler();
    final Runnable m_5SecCountdownRunnable = new Runnable()
    {
        public void run()
        {
            Countdown5SecondUpdate();
        }
    };

    Handler mSimulatedStabilityHandler = new Handler();
    final Runnable mSimulatedStabilityRunnable = new Runnable()
    {
        public void run()
        {
            StabilityReached();
        }
    };

    private static Double m_nPPGGraphLastX = 0.0;
    private ProgressBar m_ProgressBar;
    private TextView m_lblAcquiring;
    private static LineGraphSeries m_PPGGraphSeries;
    private GraphView m_graphView;
    private int m_5SecCountdown;
    private ImageView timeRemainingImage;

    private int [] tenSecCountdownImages = new int[] {
            R.drawable.countdown0sec,
            R.drawable.countdown1sec,
            R.drawable.countdown2sec,
            R.drawable.countdown3sec,
            R.drawable.countdown4sec,
            R.drawable.countdown5sec,
            R.drawable.countdown6sec,
            R.drawable.countdown7sec,
            R.drawable.countdown8sec,
            R.drawable.countdown9sec,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing_stability);

        InitializeHeaderAndFooter();

        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        m_lblAcquiring = (TextView) findViewById(R.id.txtAcquiringSignal);
        m_lblAcquiring.setTypeface(robotoTypeface);

        m_graphView = findViewById(R.id.PPGStabilityGraph);

        m_graphView.getViewport().setXAxisBoundsManual(true);
        m_graphView.getViewport().setMinX(0);
        m_graphView.getViewport().setMaxX(10);
        m_graphView.getGridLabelRenderer().setVerticalAxisTitle(getResources().getString(R.string.pulse_amplitude));
        m_graphView.getGridLabelRenderer().setHorizontalAxisTitle(getResources().getString(R.string.time));
        m_graphView.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        m_graphView.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        m_graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        m_graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);
        m_graphView.getGridLabelRenderer().setNumVerticalLabels(0);
        m_graphView.getGridLabelRenderer().setNumHorizontalLabels(0);
        m_graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        m_graphView.getViewport().setDrawBorder(true);

        m_PPGGraphSeries = new LineGraphSeries<>();
        m_PPGGraphSeries.setColor(getResources().getColor(R.color.colorChartLine));
        m_PPGGraphSeries.setThickness(8);
        m_graphView.addSeries(m_PPGGraphSeries);

        m_ProgressBar = findViewById(R.id.progressBar);
        m_ProgressBar.setVisibility(View.INVISIBLE);
        m_lblAcquiring = findViewById(R.id.txtAcquiringSignal);
        m_lblAcquiring.setVisibility(View.INVISIBLE);
        m_graphView.setVisibility(View.INVISIBLE);

        IndicorConnection.getInstance().initialize(this, this);
        IndicorConnection.getInstance().ConnectToIndicor();
    }

    private void InitializeHeaderAndFooter()
    {
        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.measurement));
        HeaderFooterControl.getInstance().SetBottomMessage(this, "");
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });
    }

    public void iConnected()
    {
        m_lblAcquiring.setVisibility(View.VISIBLE);
        m_ProgressBar.setVisibility(View.VISIBLE);
        m_graphView.setVisibility(View.VISIBLE);
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.keep_arm_steady));

        mSimulatedStabilityHandler.postDelayed(mSimulatedStabilityRunnable, 5000);
    }

    public void iCharacteristicRead(Object o)
    {

    }

    public void iError(int e)
    {

    }

    public void iNotify(byte[] data)
    {
        int value = 0;
        for (int i = 1; i < data.length; i += 4)
        {
            value = (256 * (int)(data[i] & 0xFF)) + (data[i+1] & 0xFF);
            m_PPGGraphSeries.appendData(new DataPoint(m_nPPGGraphLastX, value),  true, 500);
            m_nPPGGraphLastX += 0.02;
        }
    }
    private void StabilityReached()
    {
        setContentView(R.layout.activity_testing_valsalva);

        // change the font to Roboto
        Typeface robotoLightTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView tv = findViewById(R.id.txtTimeRemaining);
        tv.setTypeface(robotoLightTypeface);

        tv = findViewById(R.id.txtMessage);
        tv.setTypeface(robotoLightTypeface);

        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_regular);

        tv = findViewById(R.id.txtCountdown);
        tv.setTypeface(robotoTypeface);

        m_5SecCountdown = 5;

        tv.setText(String.valueOf(m_5SecCountdown));
        m_5SecCountdownHandler.postDelayed(m_5SecCountdownRunnable, 1000);

    }

    private int countdownSecLeft;

    private void Countdown5SecondUpdate()
    {
        TextView tv = findViewById(R.id.txtCountdown);

        if (m_5SecCountdown > 0)
        {
            m_5SecCountdown--;
            tv.setText(String.valueOf(m_5SecCountdown));
            m_5SecCountdownHandler.postDelayed(m_5SecCountdownRunnable, 1000);
        }
        else
        {
            TestPressureGraph tpg = findViewById(R.id.testPressureGraph);
            tpg.SetGraphActiveMode(tpg.ACTIVE);
            tpg.setBallPressure((float)0.0);

            timeRemainingImage = findViewById(R.id.timeRemaingImage);
            timeRemainingImage.setImageResource(R.drawable.countdown10sec);
            m_10SecValsalvaCountdownHandler.postDelayed(m_10SecValsalvaCountdownRunnable, 1000);

            TextView tv1 = findViewById(R.id.txtMessage);
            tv1.setText(R.string.exhale);

            tv1 = findViewById(R.id.txtCountdown);
            tv1.setText("");

            countdownSecLeft = 10;
        }
    }

    private void Countdown10SecondValsalvaUpdate()
    {
        timeRemainingImage = findViewById(R.id.timeRemaingImage);
        countdownSecLeft--;
        if (countdownSecLeft < 0)
        {
            timeRemainingImage.setImageResource(R.drawable.done_circle);
        }
        else
        {
            timeRemainingImage.setImageResource(tenSecCountdownImages[countdownSecLeft]);
            m_10SecValsalvaCountdownHandler.postDelayed(m_10SecValsalvaCountdownRunnable, 1000);
        }
    }
}
