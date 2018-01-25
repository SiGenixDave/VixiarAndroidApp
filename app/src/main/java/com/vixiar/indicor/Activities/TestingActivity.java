package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
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
import com.vixiar.indicor.CustomDialog.CustomAlertDialog;
import com.vixiar.indicor.CustomDialog.CustomDialogInterface;
import com.vixiar.indicor.Data.PPGDataCalibrate;
import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.Data.RealtimeDataMarker;
import com.vixiar.indicor.Graphics.TestPressureGraph;
import com.vixiar.indicor.R;
import com.vixiar.indicor.Upload_Interface.UploadServiceInterface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TestingActivity extends Activity implements IndicorBLEServiceInterfaceCallbacks, TimerCallback, CustomDialogInterface
{
    //TODO: Pressing back button when this activity started from practice causes practice activity to start, should go directly to pat info or main
    //TODO: need to allow a bit of tolerance in the pressure checks in the first part of tha Valsalva maneuver
    //TODO: End of rest and start of valsalva are not the same point...end of rest is when pressure starts up

    private final String TAG = this.getClass().getSimpleName();

    private int m_nLastDataIndex = 0;
    private static Double m_nPPGGraphLastX = 0.0;
    private int m_nCountdownSecLeft;
    private int m_nTestNumber;
    private double m_nAvgPressure;
    private boolean m_bIsConnected;

    // UI Components (Stability Screen)
    private ProgressBar m_spinnerProgress;
    private TextView m_lblAcquiring;
    private TextView m_txtHeartRate;
    private static LineGraphSeries m_seriesPPGData;
    private GraphView m_chartPPG;

    // UI Components (Testing Screen)
    private ImageView m_imgTimeRemaining;
    private TextView m_lblTimeRemaining;
    private TextView m_lblBottomMessage;
    private TextView m_lblBottomMessageCentered;
    private TextView m_lblBottomCountdownNumber;
    private TestPressureGraph m_graphPressure;

    // UI Components (Results Screen)
    private ImageView m_imgResults1Checkbox;
    private ImageView m_imgResults2Checkbox;
    private ImageView m_imgResults3Checkbox;
    private ImageView m_imgRestIcon;
    private ImageView m_imgHomeButton;
    private TextView m_lblRest;
    private TextView m_lblPatID;
    private TextView m_txtPatID;
    private TextView m_txtDateTime;
    private TextView m_txtResults1;
    private TextView m_txtResults2;
    private TextView m_txtResults3;

    private PPGDataCalibrate m_PPPGDataCalibrate = new PPGDataCalibrate();

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
        PRESSURE_ERROR,
    }

    private Testing_State m_testingState;
    private Testing_State m_StateAtDisconnect;

    private enum Testing_Events
    {
        EVT_ONESHOT_TIMER_TIMEOUT,
        EVT_CONNECTED,
        EVT_PERIODIC_TIMER_TICK,
        EVT_VALSALVA_PRESSURE_UPDATE,
        EVT_HR_STABLE,
    }

    // Timer stuff
    private GenericTimer m_oneShotTimer;
    private GenericTimer m_periodicTimer;
    private GenericTimer m_ppgcalTimer;
    private GenericTimer m_pressureOutTimer;

    private final int ONESHOT_TIMER_ID = 1;
    private final int PERIODIC_TIMER_ID = 2;
    private final int PPG_CAL_TIMER_ID = 3;
    private final int PRESSURE_OUT_TIMER_ID = 4;

    private final int DLG_ID_PRESSURE_ERROR_START = 0;
    private final int DLG_ID_PRESSURE_ERROR_RUNNING = 1;
    private final int DLG_ID_CANCEL_TEST = 2;
    private final int DLG_ID_HR_NOT_STABLE = 4;

    // Timing constants
    private final int PPGCAL_TIME_MS = 2000;
    private final int STABILIZING_TIMEOUT_MS = 60000;
    private final int AFTER_STABLE_DELAY_SECONDS = 5;
    private final int VALSALVA_WAIT_FOR_PRESSURE_TIMEOUT_MS = 10000;
    private final int VALSALVA_LOADING_RESULTS_DELAY_MS = 3000;
    private final int VALSALVA_LOADING_FINAL_RESULTS_DELAY_MS = 15000;
    private final int ONE_SEC = 1000;
    private final int NEXT_TEST_DELAY_SECONDS = 60;
    private final int VALSALVA_DURATION_SECONDS = 10;
    private final int PRESSURE_OUT_MAX_TIME_MS = 1000;

    private final double VALSALVA_MIN_PRESSURE = 16.0;
    private final double VALSALVA_MAX_PRESSURE = 30.0;

    private final double PRESSURE_FILTER_OLD_VALUE_MULTIPLIER = 0.3;
    private final double PRESSURE_FILTER_NEW_VALUE_MULTIPLIER = (1.0 - PRESSURE_FILTER_OLD_VALUE_MULTIPLIER);

    private int[] m_tenSecCountdownImages = new int[]
            {
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

        m_bIsConnected = false;

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

            case PPG_CAL_TIMER_ID:

                // Update the y scaling on the chart ony for first stability check, use the
                // saved y axis scaled values for the remaining 2 tests
                if (m_PPPGDataCalibrate.Complete(7, 7))
                {
                    double yMaxScaling = m_PPPGDataCalibrate.getYMaxChartScale();
                    double yMinScaling = m_PPPGDataCalibrate.getYMinChartScale();

                    m_chartPPG.getViewport().setMaxY(yMaxScaling);
                    m_chartPPG.getViewport().setMinY(yMinScaling);

                    m_chartPPG.getViewport().setYAxisBoundsManual(true);

                }
                else
                {
                    // Restart the timer because the cal hasn't occurred yet
                    m_ppgcalTimer.Start(this, PPGCAL_TIME_MS, true);
                }
                break;

            case PRESSURE_OUT_TIMER_ID:
                // the user couldn't keep the pressure in range
                m_periodicTimer.Cancel();
                DisplayPressureErrorRunning();
                m_testingState = Testing_State.PRESSURE_ERROR;
                // mark the error
                PatientInfo.getInstance().getRealtimeData().CreateMarker(RealtimeDataMarker.Marker_Type.MARKER_TEST_ERROR,
                        PatientInfo.getInstance().getRealtimeData().GetRawData().size() - 1);
                break;
        }
    }

    @Override
    public void iFullyConnected()
    {
        TestingStateMachine(Testing_Events.EVT_CONNECTED);
        m_bIsConnected = true;
    }

    @Override
    public void iDisconnected()
    {
        m_bIsConnected = false;
        switch (m_testingState)
        {
            case STABILIZING:
                InactivateStabilityView();
                break;

            case STABLE_5SEC_COUNTDOWN:
            case VALSALVA_WAIT_FOR_PRESSURE:
            case VALSALVA:
                InactivateTestingView();
                break;

            case RESULTS:
                break;

        }
    }

    @Override
    public void iError(int e)
    {
        //TODO Currently any error will take the user back to main screen
        Intent intent = new Intent(TestingActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void iRestart()
    {
        // Do specific stuff based on the current state

        switch (m_testingState)
        {
            case STABILIZING:
                RestartStability();
                break;

            case STABLE_5SEC_COUNTDOWN:
            case VALSALVA_WAIT_FOR_PRESSURE:
            case VALSALVA:
                SwitchToStabilityView();
                InactivateStabilityView();
                RestartStability();
                break;

            case RESULTS:
                break;

        }

        IndicorBLEServiceInterface.getInstance().ConnectToIndicor();
    }

    private void RestartStability()
    {
        if (m_oneShotTimer != null)
        {
            m_oneShotTimer.Cancel();
        }
        if (m_periodicTimer != null)
        {
            m_periodicTimer.Cancel();
        }
        if (m_ppgcalTimer != null)
        {
            m_ppgcalTimer.Cancel();
        }
        if (m_pressureOutTimer != null)
        {
            m_pressureOutTimer.Cancel();
        }

        m_testingState = Testing_State.STABILIZING_NOT_CONNECTED;

    }

    @Override
    public void iRealtimeDataNotification()
    {
        // see if we need to do anything wit the data based on the state
        switch (m_testingState)
        {
            case STABILIZING:
                // update the PPG chart
                int currentDataIndex = PatientInfo.getInstance().getRealtimeData().GetRawData().size();
                for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
                {
                    m_seriesPPGData.appendData(new DataPoint(m_nPPGGraphLastX, PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_PPG), true, 500);
                    m_nPPGGraphLastX += 0.02;
                }
                m_nLastDataIndex = currentDataIndex;

                // update the heart rate on the screen
                double BPM = PatientInfo.getInstance().getRealtimeData().GetCurrentBPM();
                String sTemp = "HR = " + String.valueOf((int) BPM) + " BPM";
                m_txtHeartRate.setText(sTemp);

                // see if the HR is stable
                if (PatientInfo.getInstance().getRealtimeData().IsHeartRateStable())
                {
                    TestingStateMachine(Testing_Events.EVT_HR_STABLE);
                }
                break;

            case VALSALVA_WAIT_FOR_PRESSURE:
            case VALSALVA:
                currentDataIndex = PatientInfo.getInstance().getRealtimeData().GetRawData().size();
                double tempSum = 0.0;
                for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
                {
                    // sum up all the pressures from this set of data
                    tempSum += PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_pressure;
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

    @Override
    public void onBackPressed()
    {
    }

    @Override
    public void onClickPositiveButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
            case DLG_ID_HR_NOT_STABLE:
            case DLG_ID_PRESSURE_ERROR_START:
            case DLG_ID_PRESSURE_ERROR_RUNNING:
                // this is the "Try again" button, we need to restart this test
                SwitchToStabilityView();
                ActivateStabilityView();

                m_testingState = Testing_State.STABILIZING;

                PatientInfo.getInstance().getRealtimeData().StartHeartRateValidation();
                break;

            case DLG_ID_CANCEL_TEST:
                ExitToMainActivity();
                break;
        }
    }

    @Override
    public void onClickNegativeButton(DialogInterface dialog, int dialogID)
    {

        switch (dialogID)
        {
            case DLG_ID_HR_NOT_STABLE:
            case DLG_ID_PRESSURE_ERROR_START:
            case DLG_ID_PRESSURE_ERROR_RUNNING:
                ExitToMainActivity();
                break;
        }
    }


    private void ExitToMainActivity()
    {
        // stop any running timers
        m_periodicTimer.Cancel();
        m_oneShotTimer.Cancel();
        m_ppgcalTimer.Cancel();
        m_pressureOutTimer.Cancel();

        // disconnect from the handheld
        IndicorBLEServiceInterface.getInstance().DisconnectFromIndicor();

        // clear any data and return to the main activity
        PatientInfo.getInstance().ClearAllPatientData();
        Intent intent = new Intent(TestingActivity.this, MainActivity.class);
        startActivity(intent);
    }

    // GUI functions

    private void SwitchToStabilityView()
    {
        setContentView(R.layout.activity_testing_stability);

        // get the controls
        m_lblAcquiring = findViewById(R.id.txtAcquiringSignal);
        m_spinnerProgress = findViewById(R.id.progressBar);
        m_chartPPG = findViewById(R.id.PPGStabilityGraph);
        m_txtHeartRate = findViewById(R.id.lblHeartRate);

        m_lblAcquiring.setTypeface(m_robotoLightTypeface);
        m_txtHeartRate.setTypeface(m_robotoLightTypeface);

        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, GetMeasurementScreenTitle());
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                UserRequestingToCancel();
            }
        });

        m_chartPPG.getGridLabelRenderer().setHighlightZeroLines(false);
        m_chartPPG.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.LEFT);
        m_chartPPG.getGridLabelRenderer().setLabelVerticalWidth(100);
        m_chartPPG.getGridLabelRenderer().setTextSize(20);
        m_chartPPG.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        m_chartPPG.getGridLabelRenderer().setHorizontalLabelsAngle(90);
        m_chartPPG.getGridLabelRenderer().reloadStyles();

        //m_chartPPG.getGridLabelRenderer().setVerticalAxisTitle(getResources().getString(R.string.pulse_amplitude));
        //m_chartPPG.getGridLabelRenderer().setHorizontalAxisTitle(getResources().getString(R.string.time));
        //m_chartPPG.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        //m_chartPPG.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        m_chartPPG.getGridLabelRenderer().setVerticalLabelsVisible(false);

        m_chartPPG.getViewport().setXAxisBoundsManual(true);
        m_chartPPG.getViewport().setMinX(0);
        m_chartPPG.getViewport().setMaxX(5);

        CreateChartSeriesAddToChart();
    }

    private void CreateChartSeriesAddToChart()
    {
        m_seriesPPGData = new LineGraphSeries<>();
        m_seriesPPGData.setColor(getResources().getColor(R.color.colorChartLine));
        m_seriesPPGData.setThickness(5);
        m_chartPPG.addSeries(m_seriesPPGData);
    }

    private String GetMeasurementScreenTitle()
    {
        return getString(R.string.measurement) + " " + String.valueOf(m_nTestNumber) + "/3";
    }

    private void UserRequestingToCancel()
    {
        // display the test cancel dialog
        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                getString(R.string.dlg_title_cancel_test),
                getString(R.string.dlg_msg_cancel_test),
                "Yes",
                "No", TestingActivity.this, DLG_ID_CANCEL_TEST, TestingActivity.this);
    }

    private void InactivateStabilityView()
    {
        m_txtHeartRate.setVisibility(View.INVISIBLE);
        m_lblAcquiring.setVisibility(View.INVISIBLE);
        m_chartPPG.setVisibility(View.INVISIBLE);
        m_spinnerProgress.setVisibility(View.INVISIBLE);
    }

    private void ActivateStabilityView()
    {
        m_lblAcquiring.setVisibility(View.VISIBLE);

        // clear any data in the PPG chart
        m_chartPPG.removeAllSeries();
        m_seriesPPGData = null;

        // Set the current data index to the data size so that only the newest data is pulled out
        // of the buffer and displayed
        m_nLastDataIndex = PatientInfo.getInstance().getRealtimeData().GetRawData().size();
        m_nPPGGraphLastX = 0.0;

        CreateChartSeriesAddToChart();

        m_chartPPG.setVisibility(View.VISIBLE);
        m_spinnerProgress.setVisibility(View.VISIBLE);
        m_txtHeartRate.setVisibility(View.VISIBLE);
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.keep_arm_steady));

        m_oneShotTimer.Start(this, STABILIZING_TIMEOUT_MS, true);

        HeaderFooterControl.getInstance().ShowBatteryIcon(this, IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel());

        m_PPPGDataCalibrate.Start();
        m_ppgcalTimer.Start(this, PPGCAL_TIME_MS, true);

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
                UserRequestingToCancel();
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

    private void SetupLoadingResultsView()
    {
        // set the counter to show the done image
        UpdateValsalvaCountdown(-1);
        m_graphPressure.SetGraphActiveMode(m_graphPressure.INACTIVE);
        m_lblTimeRemaining.setVisibility(View.INVISIBLE);
        m_lblBottomCountdownNumber.setVisibility(View.INVISIBLE);
        if (m_nTestNumber < 3)
        {
            m_lblBottomMessage.setText(R.string.loading_results);
        }
        else
        {
            m_lblBottomMessage.setText(R.string.processing);
        }
    }

    private void SwitchToResultsView()
    {
        setContentView(R.layout.activity_testing_results);

        m_lblBottomMessage = findViewById(R.id.txtMessage);
        m_lblBottomMessageCentered = findViewById(R.id.txtMessageCentered);
        m_lblBottomCountdownNumber = findViewById(R.id.txtCountdown);
        m_imgResults1Checkbox = findViewById(R.id.imgMeasurement1Checkbox);
        m_imgResults2Checkbox = findViewById(R.id.imgMeasurement2Checkbox);
        m_imgResults3Checkbox = findViewById(R.id.imgMeasurement3Checkbox);
        m_txtResults1 = findViewById(R.id.lblRes1);
        m_txtResults2 = findViewById(R.id.lblRes2);
        m_txtResults3 = findViewById(R.id.lblRes3);
        m_imgRestIcon = findViewById(R.id.imgRestIcon);
        m_imgHomeButton = findViewById(R.id.imgHomeIcon);
        m_lblRest = findViewById(R.id.lblRest);
        m_lblPatID = findViewById(R.id.lblID);
        m_txtPatID = findViewById(R.id.txtPatID);
        m_txtDateTime = findViewById(R.id.txtDateTime);

        m_lblRest.setTypeface(m_robotoLightTypeface);
        m_lblPatID.setTypeface(m_robotoRegularTypeface);
        m_txtDateTime.setTypeface(m_robotoRegularTypeface);
        m_txtPatID.setTypeface(m_robotoRegularTypeface);
        m_txtResults1.setTypeface(m_robotoRegularTypeface);
        m_txtResults2.setTypeface(m_robotoRegularTypeface);
        m_txtResults3.setTypeface(m_robotoRegularTypeface);
        m_lblBottomMessageCentered.setTypeface(m_robotoRegularTypeface);

        m_txtPatID.setText(PatientInfo.getInstance().get_patientId());

        m_txtDateTime.setText(PatientInfo.getInstance().get_testDate());

        m_lblBottomMessageCentered.setText("");
        m_imgHomeButton.setVisibility(View.INVISIBLE);

        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.results));

        if (m_nTestNumber < 3)
        {
            HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
            HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    UserRequestingToCancel();
                }
            });
        }
        else
        {
            // there's no button once all three tests are completed
            HeaderFooterControl.getInstance().SetNavButtonTitle(this, "");
            HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                }
            });
        }

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
                String result = RoundAndTruncateDouble(PatientInfo.getInstance().get_LVEDP(0)) +
                        " mmHg";
                m_txtResults1.setText(result);
                break;

            case 2:
                m_imgResults1Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults2Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults3Checkbox.setImageResource(R.drawable.measurement_unchecked);
                result = RoundAndTruncateDouble(PatientInfo.getInstance().get_LVEDP(0)) +
                        " mmHg";
                m_txtResults1.setText(result);
                result = RoundAndTruncateDouble(PatientInfo.getInstance().get_LVEDP(1)) +
                        " mmHg";
                m_txtResults2.setText(result);
                break;

            case 3:
                m_imgResults1Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults2Checkbox.setImageResource(R.drawable.measurement_checked);
                m_imgResults3Checkbox.setImageResource(R.drawable.measurement_checked);
                result = RoundAndTruncateDouble(PatientInfo.getInstance().get_LVEDP(0)) +
                        " mmHg";
                m_txtResults1.setText(result);
                result = RoundAndTruncateDouble(PatientInfo.getInstance().get_LVEDP(1)) +
                        " mmHg";
                m_txtResults2.setText(result);
                result = RoundAndTruncateDouble(PatientInfo.getInstance().get_LVEDP(2)) +
                        " mmHg";
                m_txtResults3.setText(result);

                float avg = (float) ((PatientInfo.getInstance().get_LVEDP(0) +
                                        PatientInfo.getInstance().get_LVEDP(1) +
                                        PatientInfo.getInstance().get_LVEDP(2)) / 3.0);
                TextView avgText = findViewById(R.id.lblTestAverage);
                avgText.setText(RoundAndTruncateDouble(avg) + " mmHg");

                break;

            default:
                m_imgResults1Checkbox.setImageResource(R.drawable.measurement_unchecked);
                m_imgResults2Checkbox.setImageResource(R.drawable.measurement_unchecked);
                m_imgResults3Checkbox.setImageResource(R.drawable.measurement_unchecked);
                break;

        }
    }

    private String RoundAndTruncateDouble(double value)
    {
        String s = String.format("%d", (int)(value + 0.5));
        return s;
    }

    private void SetResultsViewComplete()
    {
        UpdateResults();
        m_lblBottomMessage.setText("");
        m_imgRestIcon.setVisibility(View.INVISIBLE);
        m_lblRest.setText("");
        m_lblBottomCountdownNumber.setText("");
        m_imgHomeButton.setVisibility(View.VISIBLE);
        m_lblBottomMessageCentered.setText(getString(R.string.test_complete));

        m_imgHomeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                PatientInfo.getInstance().ClearAllPatientData();
                Intent intent = new Intent(TestingActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void DisableHomeButton()
    {
        m_imgHomeButton.setAlpha((float)0.3);
        m_imgHomeButton.setEnabled(false);
    }

    private void EnableHomeButton()
    {
        m_imgHomeButton.setAlpha((float)1.0);
        m_imgHomeButton.setEnabled(true);
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
        PatientInfo.getInstance().set_testDate(getDateTime());

        SwitchToStabilityView();
        InactivateStabilityView();

        m_oneShotTimer = new GenericTimer(ONESHOT_TIMER_ID);
        m_periodicTimer = new GenericTimer(PERIODIC_TIMER_ID);
        m_ppgcalTimer = new GenericTimer(PPG_CAL_TIMER_ID);
        m_pressureOutTimer = new GenericTimer(PRESSURE_OUT_TIMER_ID);

        m_testingState = Testing_State.STABILIZING_NOT_CONNECTED;
    }

    private String getDateTime()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.getDefault());
        Date date = new Date();
        return simpleDateFormat.format(date);
    }

    private void DisplayPressureErrorOnStart()
    {
        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                getString(R.string.dlg_title_pressure_error_start),
                getString(R.string.dlg_msg_pressure_error_start),
                "Try Again",
                "End Test", this, DLG_ID_PRESSURE_ERROR_START, this);
    }

    private void DisplayPressureErrorRunning()
    {
        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                getString(R.string.dlg_title_pressure_error_running),
                getString(R.string.dlg_msg_pressure_error_running),
                "Try Again",
                "End Test", this, DLG_ID_PRESSURE_ERROR_RUNNING, this);
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

                    // start checking for stability
                    PatientInfo.getInstance().getRealtimeData().StartHeartRateValidation();
                }
                break;

            case STABILIZING:
                //Log.i(TAG, "In state: STABILIZING");
                if (event == Testing_Events.EVT_HR_STABLE)
                {
                    SwitchToTestingView();
                    InactivateTestingView();
                    m_testingState = Testing_State.STABLE_5SEC_COUNTDOWN;
                    m_periodicTimer.Start(this, ONE_SEC, false);
                    m_nCountdownSecLeft = AFTER_STABLE_DELAY_SECONDS;
                    UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                    m_oneShotTimer.Cancel();
                }
                else if (event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT)
                {
                    if (m_bIsConnected)
                    {
                        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                                getString(R.string.dlg_title_hr_not_stable),
                                getString(R.string.dlg_msg_hr_not_stable),
                                "Try Again",
                                "End Test",
                                this, DLG_ID_HR_NOT_STABLE, this);
                    }
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
                        if (m_bIsConnected)
                        {
                            m_periodicTimer.Cancel();
                            ActivateTestingView();
                            m_testingState = Testing_State.VALSALVA_WAIT_FOR_PRESSURE;
                            m_oneShotTimer.Start(this, VALSALVA_WAIT_FOR_PRESSURE_TIMEOUT_MS, true);
                            m_nAvgPressure = 0.0;
                        }
                        else
                        {
                            UpdateBottomCountdownNumber(m_nCountdownSecLeft);
                        }
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
                                PatientInfo.getInstance().getRealtimeData().GetRawData().size() - 1);
                        m_testingState = Testing_State.VALSALVA;
                        m_periodicTimer.Start(this, ONE_SEC, false);
                        m_nCountdownSecLeft = VALSALVA_DURATION_SECONDS;
                        m_oneShotTimer.Cancel();
                        UpdateValsalvaCountdown(m_nCountdownSecLeft);
                    }
                }
                else if ((event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT) && (m_bIsConnected))
                {
                    DisplayPressureErrorOnStart();
                    m_testingState = Testing_State.PRESSURE_ERROR;
                    m_oneShotTimer.Cancel();
                }
                break;

            case VALSALVA:
                if (event == Testing_Events.EVT_VALSALVA_PRESSURE_UPDATE)
                {
                    if (m_nAvgPressure < VALSALVA_MIN_PRESSURE || m_nAvgPressure > VALSALVA_MAX_PRESSURE)
                    {
                        // start the timer if it isn't already running
                        if (!m_pressureOutTimer.IsRunning())
                        {
                            m_pressureOutTimer.Start(this, PRESSURE_OUT_MAX_TIME_MS, true);
                        }
                    }
                    else
                    {
                        m_pressureOutTimer.Cancel();
                    }
                }
                else if (event == Testing_Events.EVT_PERIODIC_TIMER_TICK)
                {
                    m_nCountdownSecLeft--;
                    if (m_nCountdownSecLeft > 0)
                    {
                        UpdateValsalvaCountdown(m_nCountdownSecLeft);
                    }
                    else
                    {
                        if (m_bIsConnected)
                        {
                            // Valsalva is over
                            PatientInfo.getInstance().getRealtimeData().CreateMarker(RealtimeDataMarker.Marker_Type.MARKER_END_VALSALVA,
                                    PatientInfo.getInstance().getRealtimeData().GetRawData().size() - 1);

                            m_periodicTimer.Cancel();
                            m_pressureOutTimer.Cancel();
                            SetupLoadingResultsView();
                            m_testingState = Testing_State.LOADING_RESULTS;
                            if (m_nTestNumber < 3)
                            {
                                m_oneShotTimer.Start(this, VALSALVA_LOADING_RESULTS_DELAY_MS, true);
                            }
                            else
                            {
                                m_oneShotTimer.Start(this, VALSALVA_LOADING_FINAL_RESULTS_DELAY_MS, true);
                            }
                        }
                    }
                }
                break;

            case LOADING_RESULTS:
                //Log.i(TAG, "In state: LOADING_RESULTS");
                if (event == Testing_Events.EVT_ONESHOT_TIMER_TIMEOUT)
                {
                    PatientInfo.getInstance().CalculateResults(m_nTestNumber - 1);
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
                        // disconect from the handheld
                        IndicorBLEServiceInterface.getInstance().DisconnectFromIndicor();

                        // pause the upload service so it doesn't try to send a partial file
                        UploadServiceInterface.getInstance().PauseUpload();

                        // save the csv file
                        PatientInfo.getInstance().SaveCSVFile(this);

                        UploadServiceInterface.getInstance().ResumeUpload();
                        SwitchToResultsView();
                        SetResultsViewComplete();
                        m_testingState = Testing_State.COMPLETE;
                    }
                }
                break;

            case RESULTS:
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
                        if (m_bIsConnected)
                        {
                            // starting next test
                            m_periodicTimer.Cancel();
                            m_nTestNumber++;
                            SwitchToStabilityView();
                            ActivateStabilityView();
                            m_testingState = Testing_State.STABILIZING;

                            // start checking for stability
                            PatientInfo.getInstance().getRealtimeData().StartHeartRateValidation();
                        }
                        else
                        {
                            UpdateBottomCountdownNumber(0);
                        }
                    }
                }
                break;


            case COMPLETE:
                // sit here and wait for the user to click the home button
                break;

            case PRESSURE_ERROR:
                //Log.i(TAG, "In state: PRESSURE_ERROR");
                break;

        }
    }
}
