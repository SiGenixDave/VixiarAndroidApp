package com.vixiar.indicor;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.INotificationSideChannel;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class StabilityActivity extends Activity implements IndicorDataInterface
{

    Handler mUIUpdateHandler = new Handler();
    private ImageView pressureDetector;
    final Runnable mUIUpdateRunnable = new Runnable()
    {
        public void run()
        {
            mUIUpdateHandler.postDelayed(this, 20);
            UpdateUI();
        }
    };

    private static Double m_nPPGGraphLastX = 0.0;
    private ProgressBar m_ProgressBar;
    private TextView m_lblAcquiring;
    private static LineGraphSeries m_PPGGraphSeries;
    private Integer m_bBLEConnected = 0;
    private GraphView m_graphView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stability);

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
        mUIUpdateHandler.post(mUIUpdateRunnable);
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
        synchronized (m_bBLEConnected)
        {
            m_bBLEConnected = 1;
        }
    }

    public void iCharacteristicRead(Object o)
    {

    }

    public void iError(int e)
    {

    }

    private List<Integer> mSyncPPGData = new ArrayList<>();

    public void iNotify(byte[] data)
    {
        int value = 0;
        for (int i = 1; i < data.length; i += 4)
        {
            value = (256 * (int)(data[i] & 0xFF)) + (data[i+1] & 0xFF);
            synchronized (mSyncPPGData)
            {
                mSyncPPGData.add(value);
            }
        }
    }

    int graphCount;

    public void UpdateUI()
    {
        synchronized (mSyncPPGData)
        {
            while (graphCount < mSyncPPGData.size())
            {
                m_PPGGraphSeries.appendData(new DataPoint(m_nPPGGraphLastX, mSyncPPGData.get(graphCount)), true, 500);
                m_nPPGGraphLastX += 0.02;
                graphCount++;
            }
        }
        synchronized (m_bBLEConnected)
        {
            if(m_bBLEConnected == 1)
            {
                m_lblAcquiring.setVisibility(View.VISIBLE);
                m_ProgressBar.setVisibility(View.VISIBLE);
                m_graphView.setVisibility(View.VISIBLE);
                HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.keep_arm_steady));
                m_bBLEConnected = 2;
            }
        }
    }
}
