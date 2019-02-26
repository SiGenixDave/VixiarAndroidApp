package com.vixiar.indicor2.Data;//import android.util.Log;

//import java.util.ArrayList;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by gyurk on 11/15/2017.
 */

public class RealtimeData
{
    private ArrayList<RealtimeDataSample> m_rawData = new ArrayList<RealtimeDataSample>();
    private ArrayList<RealtimeDataSample> m_HPLPfilteredData = new ArrayList<RealtimeDataSample>();
    private ArrayList<RealtimeDataSample> m_HPfilteredData = new ArrayList<RealtimeDataSample>();
    private ArrayList<RealtimeDataSample> m_LPfilteredData = new ArrayList<RealtimeDataSample>();
    private ArrayList<RealtimeDataMarker> m_markers = new ArrayList<RealtimeDataMarker>();
    public ArrayList<ValueAndLocation> m_InterpolatedValleys = new ArrayList<>();
    private I_FIRFilterTap PPGTaps = new PPG_FIRFilterTaps();
    private I_FIRFilterTap PressureTaps = new Pressure_FIRFilterTaps();
    private FIRFilter m_PPGFIRFilter = new FIRFilter(PPGTaps.GetTaps());
    private FIRFilter m_PressureFIRFilter = new FIRFilter(PressureTaps.GetTaps());
    private BiquadFilter bq1 = new BiquadFilter();
    private BiquadFilter bq2 = new BiquadFilter();

    public void Initialize()
    {
        RealtimePeakValleyDetect.getInstance().Initialize(5000, 5000, false);
        m_rawData.clear();
        m_HPLPfilteredData.clear();
        m_HPfilteredData.clear();
        m_LPfilteredData.clear();
        m_markers.clear();
        m_InterpolatedValleys.clear();
        m_PressureFIRFilter.Initialize();
        m_PPGFIRFilter.Initialize();
    }

    public void AppendNewSample(byte[] new_data)
    {
        // extract the m_rawData...the first byte is the sequence number
        // followed by two bytes of PPG then pressure repetitively
        double pressureMMHg = 0.0;
        int pressureCounts = 0;
        int ppgCounts = 0;
        for (int i = 1; i < new_data.length; i += 4)
        {
            // convert the a/d counts from the handheld to pressure in mmHg
            pressureCounts = (256 * (new_data[i + 2] & 0xFF)) + (new_data[i + 3] & 0xFF);

            // remove a known offset from the counts that are sent from the handheld
            pressureCounts -= 15;

            // convert to mmHg
            // the conversion depends on the version of the handheld
            // if it's version is less than 0.5.0.0 then it's the old conversion
            // otherwise, the new conversion
            String handheldVersion = PatientInfo.getInstance().get_firmwareRevision();
            String splitUp[] = handheldVersion.split("\\.", 4);

            // assume a default version of 4
            int version = 4;
            if (splitUp.length >= 3)
            {
                version = Integer.parseInt(splitUp[1]);
            }

            if (version < 4)
            {
                pressureMMHg = ((double) pressureCounts * (-0.0263)) + 46.726;
            }
            else if (version == 4)
            {
                pressureMMHg = ((double) (pressureCounts + 15) * (-0.0263)) + 48.96;
            }
            else
            {
                pressureMMHg = (double) (pressureCounts - 15) / 38.027506;
            }

            // make sure it's not negative
            if (pressureMMHg < 0.0)
            {
                pressureMMHg = 0.0;
            }

            ppgCounts = (256 * (new_data[i] & 0xFF)) + (new_data[i + 1] & 0xFF);

            RealtimeDataSample pdIn = new RealtimeDataSample(ppgCounts, pressureMMHg);
            pdIn.m_PPG = 65535-pdIn.m_PPG;
            m_rawData.add(pdIn);

            // apply the FIR filter to the PPG
            m_PPGFIRFilter.PutSample(pdIn.m_PPG);
            int ppgFiltered = (int) m_PPGFIRFilter.GetOutput();

            // apply the FIR filter to the pressure
            // this filter just delays the pressure by the same amount as the PPG filter delays the PPG
            m_PressureFIRFilter.PutSample(pressureMMHg);
            double pressureFiltered = m_PressureFIRFilter.GetOutput();

            // put the filtered data back into a structure to be stored
            RealtimeDataSample pdLPFiltered = new RealtimeDataSample(ppgFiltered, pressureFiltered);
            m_LPfilteredData.add(pdLPFiltered);

            // apply the highpass filter
            double HPOut = bq1.filter(pdLPFiltered.m_PPG);

            // save the HP-LP filtered data to a different structure
            RealtimeDataSample pdHPLPFiltered = new RealtimeDataSample((int)HPOut, pressureFiltered);
            m_HPLPfilteredData.add(pdHPLPFiltered);

            // save the HP filtered data
            double HPOut2 = bq2.filter(pdIn.m_PPG);
            RealtimeDataSample pdHPFiltered = new RealtimeDataSample((int)HPOut2, pdIn.m_pressure);
            m_HPfilteredData.add(pdHPFiltered);

            // let the PV detect use the HPLP filtered data
            RealtimePeakValleyDetect.getInstance().AddToDataArray((int)HPOut);
        }

        RealtimePeakValleyDetect.getInstance().ExecuteRealtimePeakDetection();
    }

    public int GetCurrentDataIndex()
    {
        return m_rawData.size();
    }


    public boolean IsPPGSignalFlatlining(int startIndex)
    {
        boolean isFlatlining = false;

        // make sure there is enough data collected based on the lookback window setting
        if ((m_HPLPfilteredData.size() - startIndex) > (AppConstants.LOOKBACK_SECONDS_FOR_FLATLINE * AppConstants.SAMPLES_PER_SECOND))
        {
            // get the standard deviation for the last 2 seconds
            int startCheckIndex = m_HPLPfilteredData.size() - (AppConstants.LOOKBACK_SECONDS_FOR_FLATLINE * AppConstants.SAMPLES_PER_SECOND);
            int endCheckIndex = m_HPLPfilteredData.size() - 1;

            double stDev = DataMath.getInstance().CalculateStdev(startCheckIndex, endCheckIndex, m_HPLPfilteredData);
            if (stDev < AppConstants.SD_LIMIT_FOR_FLATLINE)
            {
                isFlatlining = true;
            }
        }
        else
        {
            // there's not enough data yet to tell, just say everything is ok
            isFlatlining = false;
        }

        return isFlatlining;
    }

    public boolean IsMovementDetected(int startIndex)
    {
        // this function will do 2 things, one, verify that there's no clipping
        // second, it will make sure that the number of zero crossings over the last 2 seconds aren't too high
        // for zero crossings, zero is defined as the mean of the data over the past 2 seconds
        boolean isMovement = false;

        // make sure there is enough data collected based on the lookback window setting
        if ((m_LPfilteredData.size() - startIndex) > (AppConstants.LOOKBACK_SECONDS_FOR_MOVEMENT * AppConstants.SAMPLES_PER_SECOND))
        {
            int startCheckIndex = m_LPfilteredData.size() - (AppConstants.LOOKBACK_SECONDS_FOR_MOVEMENT * AppConstants.SAMPLES_PER_SECOND);
            int endCheckIndex = m_LPfilteredData.size() - AppConstants.SAMPLES_PER_SECOND - 1;

            double mean = DataMath.getInstance().CalculateMean(startCheckIndex, endCheckIndex, m_LPfilteredData);
            double stdev = DataMath.getInstance().CalculateStdev(startCheckIndex, endCheckIndex, m_LPfilteredData);
            double upperLimit = mean + (AppConstants.STDEVS_ABOVE_MEAN_LIMIT_FOR_MOVEMENT * stdev);
            double lowerLimit = mean - (AppConstants.STDEVS_BELOW_MEAN_LIMIT_FOR_MOVEMENT * stdev);

            // see if the sample is within the limit
            int dataPoint = m_LPfilteredData.get(m_LPfilteredData.size()-1).m_PPG;
            if (dataPoint > upperLimit || dataPoint < lowerLimit)
            {
                isMovement = true;
            }
        }
        else
        {
            // there's not enough data yet to tell, just say everything is ok
            isMovement = false;
        }

        return isMovement;

    }

    public boolean DidPPGSignalContainHighFrequencyNoise(int startIndex)
    {
        // in order to determine if there's high frequency noise, it's necessary to go through all of the
        // baseline data and take the standard deviation of 5 points with the sample of concern in the middle.
        // that get's divided by the standard deviation of 3 seconds of data before the sample of concern
        // then those values are accumulated and the average of the 20 lowest values is computed...if this
        // average is greater than 0.4, there's high frequency noise present.

        boolean hfNoiseDetected = false;
        List<Double> ratioList = new ArrayList<>();

        // first, there must be at least 3 seconds (+ 2 samples) of data before we even start
        if (m_HPLPfilteredData.size() > (AppConstants.SAMPLES_PER_SECOND * 3) + 2)
        {
            // start at the start of baseline plus 3 seconds
            int startingRangeIndex = startIndex + (AppConstants.SAMPLES_PER_SECOND * 3);
            int endingRangeIndex = m_HPLPfilteredData.size() - 3;
            for (int i = startingRangeIndex; i < endingRangeIndex; i++)
            {
                double localPointsStdev = DataMath.getInstance().CalculateStdev(i-2, i + 2, m_HPLPfilteredData);
                double localSecondsStdev = DataMath.getInstance().CalculateStdev(i - (AppConstants.SAMPLES_PER_SECOND * 3), i, m_HPLPfilteredData);

                if (localSecondsStdev != 0.0)
                {
                    double ratio = localPointsStdev / localSecondsStdev;

                    // keep all of the ratios in an array
                    ratioList.add(ratio);
                }
            }
            // now sort the ratios
            Collections.sort(ratioList);

            // get the average of the lowest 20
            double sum = 0.0;
            int numToAverage;
            if (ratioList.size() < 20)
            {
                numToAverage = ratioList.size();
            }
            else
            {
                numToAverage = 20;
            }
            for (int i = 0; i < numToAverage; i++)
            {
                sum += ratioList.get(i);
            }
            double average = sum / numToAverage;

            if (average > AppConstants.HF_NOISE_LIMIT)
            {
                hfNoiseDetected = true;
            }
        }
        return hfNoiseDetected;
    }

    public boolean WasHeartRateInRange(int startIndex)
    {
        boolean isInRange = false;

        double baselineHeartRate  = HeartRateInfo.getInstance().CalculateEndHeartRateStartingAt(startIndex, -1);

        if (baselineHeartRate >= AppConstants.MIN_STABLE_HR && baselineHeartRate <= AppConstants.MAX_STABLE_HR)
        {
            isInRange = true;
        }
        return isInRange;
    }

    public double GetCurrentHeartRate(int startIndex)
    {
        return HeartRateInfo.getInstance().CalculateEndHeartRateStartingAt(startIndex, 3);
    }

    public ArrayList<RealtimeDataMarker> GetDataMarkers()
    {
        return m_markers;
    }

    public void CreateMarker(RealtimeDataMarker.Marker_Type type, int index)
    {
        RealtimeDataMarker marker = new RealtimeDataMarker(type, index);
        m_markers.add(marker);
    }

    public ArrayList<RealtimeDataSample> GetRawData()
    {
        return m_rawData;
    }

    public ArrayList<RealtimeDataSample> GetHPLPFilteredData()
    {
        return m_HPLPfilteredData;
    }

    public ArrayList<RealtimeDataSample> GetLPFilteredData()
    {
        return m_LPfilteredData;
    }

    public ArrayList<RealtimeDataSample> GetHPFilteredData()
    {
        return m_HPfilteredData;
    }

    public void ClearAllData()
    {
    }

    public ArrayList<RealtimeDataMarker> GetMarkers()
    {
        return m_markers;
    }

    public void AppendNewFileSample(Double PPGSample, Double pressureSample)
    {
        RealtimeDataSample pdIn = new RealtimeDataSample(PPGSample.intValue(), pressureSample.intValue());
        pdIn.m_PPG = 65535-pdIn.m_PPG;
        m_rawData.add(pdIn);

        // apply the FIR filter to the PPG
        m_PPGFIRFilter.PutSample(pdIn.m_PPG);
        int ppgFiltered = (int) m_PPGFIRFilter.GetOutput();

        // apply the FIR filter to the pressure
        // this filter just delays the pressure by the same amount as the PPG filter delays the PPG
        m_PressureFIRFilter.PutSample(pressureSample);
        double pressureFiltered = m_PressureFIRFilter.GetOutput();

        // put the filtered data back into a structure to be stored
        RealtimeDataSample pdLPFiltered = new RealtimeDataSample(ppgFiltered, pressureFiltered);
        m_LPfilteredData.add(pdLPFiltered);

        // apply the highpass filter
        double HPOut = bq1.filter(pdLPFiltered.m_PPG);

        // save the HP-LP filtered data to a different structure
        RealtimeDataSample pdHPLPFiltered = new RealtimeDataSample((int)HPOut, pressureFiltered);
        m_HPLPfilteredData.add(pdHPLPFiltered);

        // save the HP filtered data
        double HPOut2 = bq2.filter(pdIn.m_PPG);
        RealtimeDataSample pdHPFiltered = new RealtimeDataSample((int)HPOut2, pdIn.m_pressure);
        m_HPfilteredData.add(pdHPFiltered);

        // let the PV detect use the HPLP filtered data
        RealtimePeakValleyDetect.getInstance().AddToDataArray((int)HPOut);
    }

}