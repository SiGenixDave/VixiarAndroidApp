/**
 *
 * @file
 * @brief
 * @copyright Copyright 2018 Vixiar Inc.. All rights reserved.
 */

package com.vixiar.indicor.Data;

/**
 * Created by Dave on 12/13/2017.
 */

import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PPGDataCalibrate {

    // //////////////////////////////////////////////////////////////////////////
    // / Attributes
    // //////////////////////////////////////////////////////////////////////////

    // point in time when to start examining peak/valley info
    private int m_StartIndex;

    private double m_YMaxChartScale;

    private double m_YMinChartScale;

    // //////////////////////////////////////////////////////////////////////////
    // / Setters
    // //////////////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////////////////
    // / Getters
    // //////////////////////////////////////////////////////////////////////////
    public double getYMaxChartScale() {
        return m_YMaxChartScale;
    }

    public double getYMinChartScale() {
        return m_YMinChartScale;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Public Methods
    // //////////////////////////////////////////////////////////////////////////
    public void Initialize() {
        m_StartIndex = -1;
        m_YMaxChartScale = -1;
        m_YMinChartScale = -1;
    }

    public void Start() {
        m_StartIndex = PeakValleyDetect.getInstance().AmountOfData();
    }

    public boolean Complete(int minPeaks, int minValleys) {

        // Get all of the peaks and valleys from start until now
        List<Integer> peaks = PeakValleyDetect.getInstance().GetIndexesBetween(m_StartIndex, -1,
                PeakValleyDetect.eSlopeZero.PEAK);
        List<Integer> valleys = PeakValleyDetect.getInstance().GetIndexesBetween(m_StartIndex, -1,
                PeakValleyDetect.eSlopeZero.VALLEY);

        if ((peaks == null) || (valleys == null)) {
            m_StartIndex = -1;
            return false;
        }

        // Verify that we have the required number of peaks and valleys
        if ((peaks.size() < minPeaks) || (valleys.size() < minValleys)) {
            return false;
        }

        // Determine the max and min amplitude
        int maxValue = CalculateMaxValue(peaks, 3);
        int minValue = CalculateMinValue(valleys, 3);

        // Allow the chart scaling to slightly exceed the min and max values. The special algorithm for the
        // min is to make the scaling symmetric (if max is 40000 and min is 30000, then the scaling will be
        // 44000 and 26000... 4000 quanta above and below)
        m_YMaxChartScale = maxValue * 1.1;
        m_YMinChartScale = minValue - (m_YMaxChartScale - maxValue);
        if (m_YMaxChartScale > 65535.0) {
            m_YMaxChartScale = 65535;
        }
        if (m_YMinChartScale < 0.0) {
            m_YMinChartScale = 0.0;
        }

        Log.d ("DAS", "m_YMaxChartScale = " + m_YMaxChartScale);
        Log.d ("DAS", "m_YMinChartScale = " + m_YMinChartScale);
        Log.d ("DAS", "m_StartIndex = " + m_StartIndex);

        return true;

    }

    // //////////////////////////////////////////////////////////////////////////
    // / Private Methods
    // //////////////////////////////////////////////////////////////////////////
    private int CalculateMaxValue(List<Integer> list, int recentSamplesToUse) {
        int maxValue = Integer.MIN_VALUE;

        int samples = 0;
        int index = list.size() - 1;

        for (Integer i: list) {
            Log.d ("DAS", "Peak: " + PeakValleyDetect.getInstance().GetData(i));
        }


        while (samples < recentSamplesToUse) {
            int data = PeakValleyDetect.getInstance().GetData(list.get(index));
            Log.d ("DAS", "Peak of Interest: " + data);
            if (data > maxValue) {
                maxValue = data;
            }
            samples++;
            index--;
        }


        return maxValue;
    }

    private int CalculateMinValue(List<Integer> list, int recentSamplesToUse) {
        int minValue = Integer.MAX_VALUE;

        int samples = 0;
        int index = list.size() - 1;

        for (Integer i: list) {
            Log.d ("DAS", "Valley: " + PeakValleyDetect.getInstance().GetData(i));
        }


        while (samples < recentSamplesToUse) {
            int data = PeakValleyDetect.getInstance().GetData(list.get(index));
            Log.d ("DAS", "Valley of Interest: " + data);
            if (data < minValue) {
                minValue = data;
            }
            samples++;
            index--;
        }

        return minValue;
    }

}
