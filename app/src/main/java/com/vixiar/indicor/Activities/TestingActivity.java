package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.vixiar.indicor.BLEInterface.IndicorBLEServiceInterface;
import com.vixiar.indicor.BLEInterface.IndicorBLEServiceInterfaceCallbacks;
import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.Data.RealtimeDataMarker;
import com.vixiar.indicor.Graphics.TestPressureGraph;
import com.vixiar.indicor.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TestingActivity extends Activity implements IndicorBLEServiceInterfaceCallbacks, TimerCallback
{
    //TODO: need to put data markers at the beginning and end of valsalva

    private final String TAG = this.getClass().getSimpleName();

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
    private TextView m_lblTimeRemaining;
    private TextView m_lblBottomMessage;
    private TextView m_lblBottomCountdownNumber;
    private TestPressureGraph m_graphPressure;

    // UI Components (Results Screen)
    private ImageView m_imgResults1Checkbox;
    private ImageView m_imgResults2Checkbox;
    private ImageView m_imgResults3Checkbox;
    private ImageView m_imgRestIcon;
    private TextView m_lblRest;
    private TextView m_lblPatID;
    private TextView m_txtPatID;
    private TextView m_txtDateTime;

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
        EVT_VALSALVA_PRESSURE_UPDATE,
    }

    // Timer stuff
    private GenericTimer m_oneShotTimer;
    private GenericTimer m_periodicTimer;

    private final int ONESHOT_TIMER_ID = 1;
    private final int PERIODIC_TIMER_ID = 2;

    // Timing constants
    private final int STABILIZING_TIME_MS = 20000;
    private final int AFTER_STABLE_DELAY_SECONDS = 5;
    private final int VALSALVA_WAIT_FOR_PRESSURE_TIMEOUT_MS = 10000;
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

        InitTest();

        IndicorBLEServiceInterface.getInstance().initialize(this, this);
        IndicorBLEServiceInterface.getInstance().ConnectToIndicor();
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
        // TODO: bring up dialog about disconnecting, handle result
        onBackPressed();
    }

    @Override
    public void iError(int e)
    {
        // TODO: handle different errors, bring up dialog about error, handle result
        onBackPressed();
    }

    @Override
    public void iRealtimeDataNotification()
    {
        // see if we need to do anything wit the data based on the state
        switch (m_testingState)
        {
            case STABILIZING:
                // update the PPG chart
                int currentDataIndex = PatientInfo.getInstance().getRealtimeData().GetData().size();
                for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
                {
                    m_seriesPPGData.appendData(new DataPoint(m_nPPGGraphLastX, PatientInfo.getInstance().getRealtimeData().GetData().get(i).m_PPG), true, 500);
                    m_nPPGGraphLastX += 0.02;
                }
                m_nLastDataIndex = currentDataIndex;
                break;

            case VALSALVA_WAIT_FOR_PRESSURE:
            case VALSALVA:
                currentDataIndex = PatientInfo.getInstance().getRealtimeData().GetData().size();
                double tempSum = 0.0;
                for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
                {
                    // sum up all the pressures from this set of data
                    tempSum += PatientInfo.getInstance().getRealtimeData().GetData().get(i).m_pressure;
                }
                double thisAvg = tempSum / (currentDataIndex - m_nLastDataIndex);
                m_nLastDataIndex = currentDataIndex;

                // calculate the new pressure average
                m_nAvgPressure = (PRESSURE_FILTER_OLD_VALUE_MULTIPLIER * m_nAvgPressure) + (PRESSURE_FILTER_NEW_VALUE_MULTIPLIER * thisAvg);

                // update the ball
                m_graphPressure.setBallPressure(m_nAvgPressure);

                TestingStateMachine(Testing_Events.EVT_VALSALVA_PRESSURE_UPDATE);
                break;
        }
    }

    @Override
    public void iBatteryLevelRead(int level)
    {
        Log.i(TAG, "Bat. level = " + level);
        HeaderFooterControl.getInstance().ShowBatteryIcon(this, level);
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
        HeaderFooterControl.getInstance().SetScreenTitle(this, GetMeasurementScreenTitle());
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
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
        m_chartPPG.getViewport().setMaxX(5);
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
        m_seriesPPGData.setThickness(3);
        m_chartPPG.addSeries(m_seriesPPGData);
    }

    private String GetMeasurementScreenTitle()
    {
        return getString(R.string.measurement) + " " + String.valueOf(m_nTestNumber) + "/3";
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

        // if the battery level was read at least once since the connection, display it, otherwise
        // it will be updated when it comes back from the handheld
        if (IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel() != -1)
        {
            HeaderFooterControl.getInstance().ShowBatteryIcon(this, IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel());
        }
    }

    @Override
    public void onBackPressed()
    {
        // TODO: (1) need to tell the interface class to cleanup and stop the battery update timer
        Log.i(TAG, "OnBackPressed");
        super.onBackPressed();
    }

    private void SwitchToTestingView()
    {
        setContentView(R.layout.activity_testing_valsalva);

        m_lblTimeRemaining = findViewById(R.id.txtTimeRemaining);
        m_lblBottomMessage = findViewById(R.id.txtMessage);
        m_lblBottomCountdownNumber = findViewById(R.id.txtCountdown);
        m_graphPressure = findViewById(R.id.testPressureGraph);
        m_imgTimeRemaining = findViewById(R.id.imgTimeRemaing);

        m_lblTimeRemaining.setTypeface(m_robotoLightTypeface);
        m_lblBottomMessage.setTypeface(m_robotoLightTypeface);
        m_lblBottomCountdownNumber.setTypeface(m_robotoRegularTypeface);

        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, GetMeasurementScreenTitle());
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });

        // if the battery level was read at least once since the connection, display it, otherwise
        // it will be updated when it comes back from the handheld
        if (IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel() != -1)
        {
            HeaderFooterControl.getInstance().ShowBatteryIcon(this, IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel());
        }
    }

    private void InactivateTestingView()
    {
        m_imgTimeRemaining.setImageResource(R.drawable.countdown10sec_dim);
        m_graphPressure.SetGraphActiveMode(m_graphPressure.INACTIVE);
        m_lblBottomMessage.setText(R.string.test_will_begin);
    }

    private void ActivateTestingView()
    {
        m_graphPressure.SetGraphActiveMode(m_graphPressure.ACTIVE);
        m_graphPressure.setBallPressure((float) 0.0);
        m_imgTimeRemaining.setImageResource(R.drawable.countdown10sec);
        m_lblBottomMessage.setText(R.string.exhale);
        m_lblBottomCountdownNumber.setText("");
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, "");
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
            }
        });
    }

    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    private void SetupLoadingResultsView()
    {
        // set the counter to show the done image
        UpdateValsalvaCountdown(-1);
        m_graphPressure.SetGraphActiveMode(m_graphPressure.INACTIVE);
        m_lblTimeRemaining.setVisibility(View.INVISIBLE);
        m_lblBottomCountdownNumber.setVisibility(View.INVISIBLE);
        m_lblBottomMessage.setText(R.string.loading_results);
    }

    private void SwitchToResultsView()
    {
        setContentView(R.layout.activity_testing_results);

        m_lblBottomMessage = findViewById(R.id.txtMessage);
        m_lblBottomCountdownNumber = findViewById(R.id.txtCountdown);
        m_imgResults1Checkbox = findViewById(R.id.imgMeasurement1Checkbox);
        m_imgResults2Checkbox = findViewById(R.id.imgMeasurement2Checkbox);
        m_imgResults3Checkbox = findViewById(R.id.imgMeasurement3Checkbox);
        m_imgRestIcon = findViewById(R.id.imgRestIcon);
        m_lblRest = findViewById(R.id.lblRest);
        m_lblPatID = findViewById(R.id.lblID);
        m_txtPatID = findViewById(R.id.txtPatID);
        m_txtDateTime = findViewById(R.id.txtDateTime);

        m_lblRest.setTypeface(m_robotoLightTypeface);
        m_lblPatID.setTypeface(m_robotoRegularTypeface);
        m_txtDateTime.setTypeface(m_robotoRegularTypeface);
        m_txtPatID.setTypeface(m_robotoRegularTypeface);

        m_txtPatID.setText(PatientInfo.getInstance().getPatientId());

        m_txtDateTime.setText(PatientInfo.getInstance().getTestDate());

        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.end_test));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.results));
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // TODO: Handle end test button from results screen
            }
        });

        // if the battery level was read at least once since the connection, display it, otherwise
        // it will be updated when it comes back from the handheld
        if (IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel() != -1)
        {
            HeaderFooterControl.getInstance().ShowBatteryIcon(this, IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel());
        }
    }

    private void UpdateResults()
    {
        // mark the correct number of check boxes
        switch (m_nTestNumber)
        {
            case 1:
                m_imgResults1Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults2Checkbox.setImageResource(R.drawable.measurement_unchecked);
                m_imgResults3Checkbox.setImageResource(R.drawable.measurement_unchecked);
                break;

            case 2:
                m_imgResults1Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults2Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults3Checkbox.setImageResource(R.drawable.measurement_unchecked);
                break;

            case 3:
                m_imgResults1Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults2Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults3Checkbox.setImageResource(R.drawable.measurement_checked);
                break;

            default:
                m_imgResults1Checkbox.setImageResource(R.drawable.measurement_unchecked);
                m_imgResults2Checkbox.setImageResource(R.drawable.measurement_unchecked);
                m_imgResults3Checkbox.setImageResource(R.drawable.measurement_unchecked);
                break;

        }
    }

    private void SetResultsViewComplete()
    {
        UpdateResults();
        m_lblBottomMessage.setText(R.string.test_complete);
        m_imgRestIcon.setVisibility(View.INVISIBLE);
        m_lblRest.setText("");
        m_lblBottomCountdownNumber.setText("");
        // TODO: (1) display the save and home icons
        // TODO: move the test complete label to the center
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

    private void InitTest()
    {
        m_nTestNumber = 1;
        PatientInfo.getInstance().setTestDate(getDateTime());

        SwitchToStabilityView();
        InactivateStabilityView();

        m_oneShotTimer = new GenericTimer(ONESHOT_TIMER_ID);
        m_periodicTimer = new GenericTimer(PERIODIC_TIMER_ID);

        m_testingState = Testing_State.STABILIZING_NOT_CONNECTED;
    }

    private String getDateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return simpleDateFormat.format(date);
    }

    private void PressureError()
    {
        // TODO: (1) bring up dialog about pressure error, handle result
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Pressure error");
        alertDialogBuilder.setMessage("Something happened with the pressure");
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void TestingStateMachine(Testing_Events event)
    {
        switch (m_testingState)
        {
            case STABILIZING_NOT_CONNECTED:
                //Log.i(TAG, "In state: STABILIZING_NOT_CONNECTED");
                if (event == Testing_Events.EVT_CONNECTED)
                {
                    ActivateStabilityView();

                    m_testingState = Testing_State.STABILIZING;

                    // start the stability timer
                    m_oneShotTimer.Start(this, STABILIZING_TIME_MS, true);
                }
                break;
            case STABILIZING:
                //Log.i(TAG, "In state: STABILIZING");
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
                //Log.i(TAG, "In state: STABLE_5SEC_COUNTDOWN");
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
                        m_oneShotTimer.Start(this, VALSALVA_WAIT_FOR_PRESSURE_TIMEOUT_MS, true);
                        m_nAvgPressure = 0.0;
                    }
                }
                break;

            case VALSALVA_WAIT_FOR_PRESSURE:
                //Log.i(TAG, "In state: VALSALVA_WAIT_FOR_PRESSURE");
                // see if we reached the starting pressure for Valsalva
                if (event == Testing_Events.EVT_VALSALVA_PRESSURE_UPDATE)
                {
                    if (m_nAvgPressure > VALSALVA_MIN_PRESSURE)
                    {
                        // Valsalva is starting
                        PatientInfo.getInstance().getRealtimeData().CreateMarker(RealtimeDataMarker.Marker_Type.MARKER_START_VALSALVA,
                                PatientInfo.getInstance().getRealtimeData().GetData().size() - 1);
                        m_testingState = Testing_State.VALSALVA;
                        m_periodicTimer.Start(this, ONE_SEC, false);
                        m_nCountdownSecLeft = VALSALVA_DURATION_SECONDS;
                        m_oneShotTimer.Cancel();
                        UpdateValsalvaCountdown(m_nCountdownSecLeft);
                    }
                }
                else if (event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT)
                {
                    PressureError();
                    m_testingState = Testing_State.PRESSURE_ERROR;
                }
                break;

            case VALSALVA:
                // TODO: disable the Android back button during valsalva
                //Log.i(TAG, "In state: VALSALVA");
                if (event == Testing_Events.EVT_VALSALVA_PRESSURE_UPDATE)
                {
                    if (m_nAvgPressure < VALSALVA_MIN_PRESSURE || m_nAvgPressure > VALSALVA_MAX_PRESSURE)
                    {
                        m_periodicTimer.Cancel();
                        PressureError();
                        m_testingState = Testing_State.PRESSURE_ERROR;
                    }
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
                        // Valsalva is over
                        PatientInfo.getInstance().getRealtimeData().CreateMarker(RealtimeDataMarker.Marker_Type.MARKER_END_VALSALVA,
                                PatientInfo.getInstance().getRealtimeData().GetData().size() - 1);

                        m_periodicTimer.Cancel();
                        SetupLoadingResultsView();
                        m_testingState = Testing_State.LOADING_RESULTS;
                        m_oneShotTimer.Start(this, VALSALVA_LOADING_RESULTS_DELAY_MS, true);
                    }
                }
                break;

            case LOADING_RESULTS:
                //Log.i(TAG, "In state: LOADING_RESULTS");
                if (event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT)
                {
                    if (m_nTestNumber < 3)
                    {
                        SwitchToResultsView();
                        UpdateResults();
                        m_periodicTimer.Start(this, ONE_SEC, false);
                        m_testingState = Testing_State.RESULTS;
                        m_nCountdownSecLeft = NEXT_TEST_DELAY_SECONDS;
                        UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                    }
                    else
                    {
                        SwitchToResultsView();
                        SetResultsViewComplete();
                        m_testingState = Testing_State.COMPLETE;
                    }
                }
                break;

            case RESULTS:
                // TODO: when countdown goes to 0, need to wait for button press to go to next test
                //Log.i(TAG, "In state: RESULTS");
                if (event == Testing_Events.EVT_PERIODIC_TIMER_TICK)
                {
                    m_nCountdownSecLeft--;
                    if (m_nCountdownSecLeft > 0)
                    {
                        UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                    }
                    else
                    {
                        // starting next test
                        m_periodicTimer.Cancel();
                        m_nTestNumber++;
                        SwitchToStabilityView();
                        ActivateStabilityView();
                        // TODO: clear the data on the stability graph so it starts a fresh display
                        m_testingState = Testing_State.STABILIZING;
                        m_oneShotTimer.Start(this, STABILIZING_TIME_MS, true);
                    }
                }
                break;

            case COMPLETE:
                //Log.i(TAG, "In state: COMPLETE");
                // TODO: (1) handle test complete state
                break;

            case PRESSURE_ERROR:
                //Log.i(TAG, "In state: PRESSURE_ERROR");
                // TODO: (1) handle pressure error state
                break;

        }
    }
}
