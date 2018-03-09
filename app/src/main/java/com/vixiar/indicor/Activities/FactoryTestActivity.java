package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.vixiar.indicor.Activities.HeaderFooterControl;
import com.vixiar.indicor.BLEInterface.IndicorBLEServiceInterface;
import com.vixiar.indicor.BLEInterface.IndicorBLEServiceInterfaceCallbacks;
import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.R;

import static android.content.ContentValues.TAG;

public class FactoryTestActivity extends Activity implements IndicorBLEServiceInterfaceCallbacks
{
    private static LineGraphSeries m_PPGSeries;
    private static LineGraphSeries m_PressureSeries;
    private static Double m_PPGLastX = 0.0;
    private static Double m_pressureLastX = 0.0;
    private static int m_nLastDataIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factory_test);
        IndicorBLEServiceInterface.getInstance().initialize(this, this);
        IndicorBLEServiceInterface.getInstance().ConnectToIndicor();

        GraphView PPGgraph = findViewById(R.id.PPGgraph);
        GraphView pressureGraph = findViewById(R.id.pressuregraph);

        m_PPGSeries = new LineGraphSeries<>();
        PPGgraph.addSeries(m_PPGSeries);
        PPGgraph.getViewport().setXAxisBoundsManual(true);
        PPGgraph.getViewport().setMinX(0);
        PPGgraph.getViewport().setMaxX(10);
        m_PPGSeries.setColor(Color.BLACK);
        m_PPGSeries.setThickness(2);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitle("PPG");
        //PPGgraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        PPGgraph.setTitleTextSize(45f);
        PPGgraph.getViewport().setDrawBorder(true);

        m_PressureSeries = new LineGraphSeries<>();
        pressureGraph.addSeries(m_PressureSeries);
        pressureGraph.getViewport().setXAxisBoundsManual(true);
        pressureGraph.getViewport().setMinX(0);
        pressureGraph.getViewport().setMaxX(10);
        m_PressureSeries.setColor(Color.BLACK);
        m_PressureSeries.setThickness(2);
        pressureGraph.getGridLabelRenderer().setVerticalAxisTitle("Pressure");
        pressureGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        pressureGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        pressureGraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        pressureGraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        pressureGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        pressureGraph.setTitleTextSize(45f);
        pressureGraph.getViewport().setDrawBorder(true);

    }

    public void iFullyConnected()
    {
        Log.i(TAG, "Connected");
        HeaderFooterControl.getInstance().ShowBatteryIcon(this, IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel());
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    public void iBatteryLevelRead(int level)
    {
    }

    public void iRestart()
    {
        IndicorBLEServiceInterface.getInstance().ConnectToIndicor();
    }


    public void iDisconnected()
    {
    }

    public void iError(int e)
    {
        onBackPressed();
    }

    public void iRealtimeDataNotification()
    {
        int currentDataIndex = PatientInfo.getInstance().getRealtimeData().GetRawData().size();

        for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
        {
            m_PPGSeries.appendData(new DataPoint(m_PPGLastX, PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_PPG), true, 500);
            m_PPGLastX += 0.02;

            m_PressureSeries.appendData(new DataPoint(m_pressureLastX, PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_pressure), true, 500);
            m_pressureLastX += 0.02;
        }
        m_nLastDataIndex = currentDataIndex;
    }
}
