package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.vixiar.indicor.BLE_Interface.IndicorConnection;
import com.vixiar.indicor.BLE_Interface.IndicorDataInterface;
import com.vixiar.indicor.Graphics.TestPressureGraph;
import com.vixiar.indicor.R;

public class TestingActivity extends Activity implements IndicorDataInterface, TimerCallback
{
    private static Double m_nPPGGraphLastX = 0.0;
    private int m_nCountdownSecLeft;
    private int m_nTestNumber;
    private double m_nAvgPressure;

    // UI Components (Stability Screen)
    private ProgressBar m_spinnerProgress;
    private TextView m_lblAcquiring;
    private static LineGraphSeries m_seriesPPGData;
    private GraphView m_chartPPG;

    // UI Components (Testing Screen)
    private ImageView m_imgTimeRemaining;
    private TextView m_lblTimeRemaning;
    private TextView m_lblBottomMessage;
    private TextView m_lblBottomCountdownNumber;
    private TestPressureGraph m_graphPressurePressure;

    Typeface m_robotoLightTypeface;
    Typeface m_robotoRegularTypeface;

    private enum Testing_State
    {
        STABILIZING_NOT_CONNECTED,
        STABILIZING,
        STABLE_5SEC_COUNTDOWN,
        VALSALVA_WAIT_FOR_PRESSURE,
        VALSALVA,
        LOADING_RESULTS,
        RESULTS,
        COMPLETE,
        PRESSURE_ERROR
    }

    private Testing_State m_testingState;

    private enum Testing_Events
    {
        EVT_ONESHOT_TIMER_TIMEOUT,
        EVT_CONNECTED,
        EVT_PERIODIC_TIMER_TICK,
        EVT_VALSALVA_START,
    }

    // Timer stuff
    private StateMachineTimer m_oneShotTimer;
    private StateMachineTimer m_periodicTimer;

    private final int ONESHOT_TIMER_ID = 1;
    private final int PERIODIC_TIMER_ID = 2;

    // Timing constants
    private final int STABILIZING_TIME_MS = 10000;
    private final int AFTER_STABLE_DELAY_SECONDS = 5;
    private final int VALSALVA_WAIT_FOR_PRESSURE_TIMEOUT_SECONDS = 10;
    private final int VALSALVA_LOADING_RESULTS_DELAY_MS = 3000;
    private final int ONE_SEC = 1000;
    private final int NEXT_TEST_DELAY_SECONDS = 60;
    private final int VALSALVA_DURATION_SECONDS = 10;

    private final double VALSALVA_MIN_PRESSURE = 16.0;
    private final double VALSALVA_MAX_PRESSURE = 30.0;

    private final double PRESSURE_FILTER_OLD_VALUE_MULTIPLIER = 0.3;
    private final double PRESSURE_FILTER_NEW_VALUE_MULTIPLIER = (1.0 - PRESSURE_FILTER_OLD_VALUE_MULTIPLIER);

    private int[] m_tenSecCountdownImages = new int[]{
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

        // Some UI work
        m_robotoLightTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);
        m_robotoRegularTypeface = ResourcesCompat.getFont(this, R.font.roboto_regular);

        InitStateMachine();

        IndicorConnection.getInstance().initialize(this, this);
        IndicorConnection.getInstance().ConnectToIndicor();
    }

    @Override
    public void TimerExpired(int id)
    {
        switch (id)
        {
            case ONESHOT_TIMER_ID:
                TestingStateMachine(Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT);
                break;

            case PERIODIC_TIMER_ID:
                TestingStateMachine(Testing_Events.EVT_PERIODIC_TIMER_TICK);
                break;
        }
    }

    @Override
    public void iConnected()
    {
        TestingStateMachine(Testing_Events.EVT_CONNECTED);
    }

    @Override
    public void iDisconnected()
    {
        // for now, just leave the activity
        onBackPressed();
    }

    @Override
    public void iCharacteristicRead(Object o)
    {
    }

    @Override
    public void iError(int e)
    {
        // for now, just leave the activity
        onBackPressed();
    }

    @Override
    public void iNotify()
    {
        // see if we need to do anything wit the data based on the state
        switch (m_testingState)
        {
            case STABILIZING:
                // update the PPG chart
                int currentDataIndex = IndicorConnection.getInstance().GetRealtimeData().GetData().size();
                for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
                {
                    m_seriesPPGData.appendData(new DataPoint(m_nPPGGraphLastX, IndicorConnection.getInstance().GetRealtimeData().GetData().get(i).m_PPG), true, 500);
                    m_nPPGGraphLastX += 0.02;
                }
                m_nLastDataIndex = currentDataIndex;
                break;

            case VALSALVA_WAIT_FOR_PRESSURE:
            case VALSALVA:
                currentDataIndex = IndicorConnection.getInstance().GetRealtimeData().GetData().size();
                double tempSum = 0.0;
                for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
                {
                    // sum up all the pressures from this set of data
                    tempSum += IndicorConnection.getInstance().GetRealtimeData().GetData().get(i).m_pressure;
                }
                double thisAvg = tempSum / (currentDataIndex - m_nLastDataIndex);
                m_nLastDataIndex = currentDataIndex;

                // calculate the new pressure average
                m_nAvgPressure = (PRESSURE_FILTER_OLD_VALUE_MULTIPLIER * m_nAvgPressure) + (PRESSURE_FILTER_NEW_VALUE_MULTIPLIER * thisAvg);

                // update the ball
                m_graphPressurePressure.setBallPressure(m_nAvgPressure);
                break;
        }
    }

    private int m_nLastDataIndex = 0;

    // GUI functions

    private void SwitchToStabilityView()
    {
        setContentView(R.layout.activity_testing_stability);

        // get the controls
        m_lblAcquiring = findViewById(R.id.txtAcquiringSignal);
        m_spinnerProgress = findViewById(R.id.progressBar);
        m_chartPPG = findViewById(R.id.PPGStabilityGraph);

        m_lblAcquiring.setTypeface(m_robotoLightTypeface);

        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.measurement));
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });

        m_chartPPG.getViewport().setXAxisBoundsManual(true);
        m_chartPPG.getViewport().setMinX(0);
        m_chartPPG.getViewport().setMaxX(10);
        m_chartPPG.getGridLabelRenderer().setVerticalAxisTitle(getResources().getString(R.string.pulse_amplitude));
        m_chartPPG.getGridLabelRenderer().setHorizontalAxisTitle(getResources().getString(R.string.time));
        m_chartPPG.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        m_chartPPG.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        m_chartPPG.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        m_chartPPG.getGridLabelRenderer().setVerticalLabelsVisible(false);
        m_chartPPG.getGridLabelRenderer().setNumVerticalLabels(0);
        m_chartPPG.getGridLabelRenderer().setNumHorizontalLabels(0);
        m_chartPPG.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        m_chartPPG.getViewport().setDrawBorder(true);

        m_seriesPPGData = new LineGraphSeries<>();
        m_seriesPPGData.setColor(getResources().getColor(R.color.colorChartLine));
        m_seriesPPGData.setThickness(8);
        m_chartPPG.addSeries(m_seriesPPGData);

    }

    private void InactivateStabilityView()
    {
        m_lblAcquiring.setVisibility(View.INVISIBLE);
        m_chartPPG.setVisibility(View.INVISIBLE);
        m_spinnerProgress.setVisibility(View.INVISIBLE);
    }

    private void ActivateStabilityView()
    {
        m_lblAcquiring.setVisibility(View.VISIBLE);
        m_chartPPG.setVisibility(View.VISIBLE);
        m_spinnerProgress.setVisibility(View.VISIBLE);
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.keep_arm_steady));
    }

    private void SwitchToTestingView()
    {
        setContentView(R.layout.activity_testing_valsalva);

        m_lblTimeRemaning = findViewById(R.id.txtTimeRemaining);
        m_lblBottomMessage = findViewById(R.id.txtMessage);
        m_lblBottomCountdownNumber = findViewById(R.id.txtCountdown);
        m_graphPressurePressure = findViewById(R.id.testPressureGraph);
        m_imgTimeRemaining = findViewById(R.id.imgTimeRemaing);

        m_lblTimeRemaning.setTypeface(m_robotoLightTypeface);
        m_lblBottomMessage.setTypeface(m_robotoLightTypeface);
        m_lblBottomCountdownNumber.setTypeface(m_robotoRegularTypeface);

        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.measurement));
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });

    }

    private void InactivateTestingView()
    {
        m_graphPressurePressure.SetGraphActiveMode(m_graphPressurePressure.INACTIVE);
        m_imgTimeRemaining.setImageResource(R.drawable.countdown10sec);
        m_lblBottomMessage.setText(R.string.test_will_begin);
    }

    private void ActivateTestingView()
    {
        m_graphPressurePressure.SetGraphActiveMode(m_graphPressurePressure.ACTIVE);
        m_graphPressurePressure.setBallPressure((float) 0.0);
        m_imgTimeRemaining.setImageResource(R.drawable.countdown10sec);
        m_lblBottomMessage.setText(R.string.exhale);
    }

    private void SwitchToResultsView()
    {

    }

    private void SetupDoneLoadingResultsView()
    {
    }

    private void UpdateBottomCountdownNumber(int value)
    {
        if (m_lblBottomCountdownNumber != null)
        {
            m_lblBottomCountdownNumber.setText(String.valueOf(value));
        }
    }

    private void UpdateValsalvaCountdown(int value)
    {
        if (value < m_tenSecCountdownImages.length)
        {
            if (value == -1)
            {
                m_imgTimeRemaining.setImageResource(R.drawable.done_circle);
            }
            else
            {
                m_imgTimeRemaining.setImageResource(m_tenSecCountdownImages[value]);
            }
        }
    }

    private void InitStateMachine()
    {
        SwitchToStabilityView();
        InactivateStabilityView();

        m_oneShotTimer = new StateMachineTimer(ONESHOT_TIMER_ID);
        m_periodicTimer = new StateMachineTimer(PERIODIC_TIMER_ID);

        m_testingState = Testing_State.STABILIZING_NOT_CONNECTED;
    }

    private void PressureError()
    {

    }

    private void TestingStateMachine(Testing_Events event)
    {
        switch (m_testingState)
        {
            case STABILIZING_NOT_CONNECTED:
                if (event == Testing_Events.EVT_CONNECTED)
                {
                    ActivateStabilityView();

                    // start the stability timer
                    m_oneShotTimer.Start(this, STABILIZING_TIME_MS, true);
                }
                break;
            case STABILIZING:
                if (event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT)
                {
                    SwitchToTestingView();
                    InactivateTestingView();
                    m_testingState = Testing_State.STABLE_5SEC_COUNTDOWN;
                    m_periodicTimer.Start(this, ONE_SEC, false);
                    m_nCountdownSecLeft = AFTER_STABLE_DELAY_SECONDS;
                    UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                }
                break;

            case STABLE_5SEC_COUNTDOWN:
                if (event == Testing_Events.EVT_PERIODIC_TIMER_TICK)
                {
                    m_nCountdownSecLeft--;
                    if (m_nCountdownSecLeft > 0)
                    {
                        UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                    }
                    else
                    {
                        m_periodicTimer.Cancel();
                        ActivateTestingView();
                        m_testingState = Testing_State.VALSALVA_WAIT_FOR_PRESSURE;
                        m_oneShotTimer.Start(this, VALSALVA_WAIT_FOR_PRESSURE_TIMEOUT_SECONDS, true);
                        m_nAvgPressure = 0.0;
                    }
                }
                break;

            case VALSALVA_WAIT_FOR_PRESSURE:
                // see if we reached the starting pressure for Valsalva
                if (m_nAvgPressure > VALSALVA_MIN_PRESSURE)
                {
                    m_testingState = Testing_State.VALSALVA;
                    m_periodicTimer.Start(this, ONE_SEC, false);
                    m_nCountdownSecLeft = VALSALVA_DURATION_SECONDS;
                    UpdateValsalvaCountdown(m_nCountdownSecLeft);
                }
                if (event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT)
                {
                    PressureError();
                    m_testingState = Testing_State.PRESSURE_ERROR;
                }
                break;

            case VALSALVA:
                if (m_nAvgPressure < VALSALVA_MIN_PRESSURE || m_nAvgPressure > VALSALVA_MAX_PRESSURE)
                {

                }
                else if (event == Testing_Events.EVT_PERIODIC_TIMER_TICK)
                {
                    m_nCountdownSecLeft--;
                    if (m_nCountdownSecLeft >= 0)
                    {
                        UpdateValsalvaCountdown(m_nCountdownSecLeft);
                    }
                    else
                    {
                        UpdateValsalvaCountdown(-1);
                        m_periodicTimer.Cancel();
                        SetupDoneLoadingResultsView();
                        m_testingState = Testing_State.LOADING_RESULTS;
                        m_oneShotTimer.Start(this, VALSALVA_LOADING_RESULTS_DELAY_MS, true);
                    }
                }
                break;

            case LOADING_RESULTS:
                if (event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT)
                {
                    m_nTestNumber++;
                    SwitchToResultsView();
                    m_periodicTimer.Start(this, ONE_SEC, false);
                    m_testingState = Testing_State.RESULTS;
                    m_nCountdownSecLeft = NEXT_TEST_DELAY_SECONDS;
                    UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                }
                break;

            case RESULTS:
            {
                if (event == Testing_Events.EVT_PERIODIC_TIMER_TICK)
                {
                    m_nCountdownSecLeft--;
                    if (m_nCountdownSecLeft > 0)
                    {
                        UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                    }
                    else
                    {
                        m_periodicTimer.Cancel();
                        InactivateStabilityView();
                        m_testingState = Testing_State.STABILIZING;
                        m_oneShotTimer.Start(this, STABILIZING_TIME_MS, true);
                    }
                }
            }
        }
    }
}
