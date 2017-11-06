package com.vixiar.indicor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class StabilityActivity extends Activity
{
    private int data[] =
            {
                    29756,29780,29736,29753,29708,29635,29622,29524,29551,29727,30665,32564,34892,37165,38802,39850,40352,40533,40601,40504,40426,40130,39501,38540,37394,36477,35781,35369,35036,34677,34355,33885,33451,32931,32484,32153,31822,31613,31329,31161,31029,30863,30751,30547,30456,30343,30231,30135,29964,29925,29832,29795,29735,29639,29683,29652,29697,29655,29623,29670,29632,29637,29523,29539,29915,31088,33225,35622,37869,39487,40431,40881,40976,41079,41075,41003,40710,40028,39101,37939,36932,36133,35603,35293,34925,34567,34033,33540,33088,32604,32256,31851,31596,31371,31170,31035,30803,30711,30570,30453,30340,30172,30126,30005,29944,29830,29725,29719,29661,29710,29656,29663,29721,29717,29786,29705,29681,29678,29735,30273,31628,33904,36353,38431,39869,40592,40979,41064,41092,41034,40821,40474,39713,38707,37556,36599,35986,35526,35220,34783,34359,33901,33393,32943,32443,32104,31795,31523,31324,31066,30955,30798,30676,30551,30376,30317,30187,30116,29977,29866,29838,29765,29775,29690,29672,29688,29668,29695,29589,29567,29538,29545,29877,30875,32843,35137,37193,38612,39315,39619,39584,39493,39314,39035,38586,37734,36781,35811,35133,34760,34464,34243,33859,33470,33037,32572,32169,31715,31443,31187,30999,30854,30665,30602,30472,30389,30270,30131,30086,29966,29911,29760,29654,29642,29590,29613,29526,29531,29556,29554,29605
            };
    private static Double PressureLastX = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stability);

        InitializeHeaderAndFooter();

        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView v = (TextView) findViewById(R.id.txtAcquiringSignal);
        v.setTypeface(robotoTypeface);

        GraphView PPGgraph = findViewById(R.id.PPGStabilityGraph);

        PPGgraph.getViewport().setXAxisBoundsManual(true);
        PPGgraph.getViewport().setMinX(0);
        PPGgraph.getViewport().setMaxX(50);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitle(getResources().getString(R.string.pulse_amplitude));
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitle(getResources().getString(R.string.time));
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        PPGgraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        PPGgraph.getGridLabelRenderer().setNumVerticalLabels(0);
        PPGgraph.getGridLabelRenderer().setNumHorizontalLabels(0);
        PPGgraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        PPGgraph.getViewport().setDrawBorder(true);


        LineGraphSeries mPPGSeries = new LineGraphSeries<>();
        mPPGSeries.setColor(getResources().getColor(R.color.colorChartLine));
        mPPGSeries.setThickness(10);
        PPGgraph.addSeries(mPPGSeries);

        for (int i = 0; i < data.length; i++)
        {
            mPPGSeries.appendData(new DataPoint(PressureLastX, data[i]), false, 500);
            PressureLastX += 0.2;
        }
    }

    private void InitializeHeaderAndFooter()
    {
        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.measurement));
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.keep_arm_steady));
        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });
    }

}
