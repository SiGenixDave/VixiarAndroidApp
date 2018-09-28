package com.vixiar.indicor2.Data;//import android.util.Log;

//import java.util.ArrayList;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by gyurk on 11/15/2017.
 */

public class RealtimeData
{
    private ArrayList<PPG_PressureDataPoint> m_rawData = new ArrayList<PPG_PressureDataPoint>();
    private ArrayList<PPG_PressureDataPoint> m_filteredData = new ArrayList<PPG_PressureDataPoint>();
    private ArrayList<RealtimeDataMarker> m_markers = new ArrayList<RealtimeDataMarker>();
    private I_FIRFilterTap PPGTaps = new PPG_FIRFilterTaps();
    private I_FIRFilterTap PressureTaps = new Pressure_FIRFilterTaps();
    private FIRFilter m_PPGFIRFilter = new FIRFilter(PPGTaps.GetTaps());
    private FIRFilter m_PressureFIRFilter = new FIRFilter(PressureTaps.GetTaps());

    public void Initialize()
    {
        RealtimePeakValleyDetect.getInstance().Initialize(5000, 5000, false);
        //HeartRateInfo.getInstance().InitializeValidation(50.0, 4, 5.0, 40.0, 150.0, 20.0);
        m_PPGFIRFilter.Initialize();
        m_PressureFIRFilter.Initialize();
        m_rawData.clear();
        m_filteredData.clear();
        m_markers.clear();
    }

    public void AppendNewSample(byte[] new_data)
    {
        // extract the m_rawData...the first byte is the sequence number
        // followed by two bytes of PPG then pressure repetitively
        double pressureValue;
        int pressureCounts;
        int ppgValue;
        for (int i = 1; i < new_data.length; i += 4)
        {
            // convert the a/d counts from the handheld to pressure in mmHg
            pressureCounts = (256 * (new_data[i + 2] & 0xFF)) + (new_data[i + 3] & 0xFF);

            // remove a known offset from the counts that are sent from the handheld
            pressureCounts -= 15;

            // convert to mmHg

            // the converstion depends on the version of the handheld
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
                pressureValue = ((double) pressureCounts * (-0.0263)) + 46.726;
            }
            else if (version == 4)
            {
                pressureValue = ((double) (pressureCounts + 15) * (-0.0263)) + 48.96;
            }
            else
            {
                pressureValue = (double) (pressureCounts - 15) / 38.027506;
            }

            // make sure it's not negative
            if (pressureValue < 0.0)
            {
                pressureValue = 0.0;
            }

            ppgValue = (256 * (new_data[i] & 0xFF)) + (new_data[i + 1] & 0xFF);

            // add the raw data to the list
            PPG_PressureDataPoint pd = new PPG_PressureDataPoint(ppgValue, pressureValue);
            m_rawData.add(pd);

            // filter the sample and store it in the filtered array
            m_PPGFIRFilter.PutSample(ppgValue);
            int ppgFiltered = (int) m_PPGFIRFilter.GetOutput();
            m_PressureFIRFilter.PutSample(pressureValue);
            double pressureFiltered = m_PressureFIRFilter.GetOutput();
            pd = new PPG_PressureDataPoint(ppgFiltered, pressureFiltered);
            m_filteredData.add(pd);

            RealtimePeakValleyDetect.getInstance().AddToDataArray(ppgFiltered);
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
        if ((m_filteredData.size() - startIndex) > (AppConstants.LOOKBACK_SECONDS_FOR_FLATLINE * AppConstants.SAMPLES_PER_SECOND))
        {
            // get the standard deviation for the last 2 seconds
            int startCheckIndex = m_filteredData.size() - (AppConstants.LOOKBACK_SECONDS_FOR_FLATLINE * AppConstants.SAMPLES_PER_SECOND);
            int endCheckIndex = m_filteredData.size() - 1;

            double stDev = DataMath.getInstance().CalculateStdev(startCheckIndex, endCheckIndex, m_filteredData);
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
        if ((m_filteredData.size() - startIndex) > (AppConstants.LOOKBACK_SECONDS_FOR_MOVEMENT * AppConstants.SAMPLES_PER_SECOND))
        {
            int startCheckIndex = m_filteredData.size() - (AppConstants.LOOKBACK_SECONDS_FOR_MOVEMENT * AppConstants.SAMPLES_PER_SECOND);
            int endCheckIndex = m_filteredData.size() - AppConstants.SAMPLES_PER_SECOND - 1;

            double mean = DataMath.getInstance().CalculateMean(startCheckIndex, endCheckIndex, m_filteredData);
            double stdev = DataMath.getInstance().CalculateStdev(startCheckIndex, endCheckIndex, m_filteredData);
            double upperLimit = mean + (AppConstants.STDEVS_ABOVE_MEAN_LIMIT_FOR_MOVEMENT * stdev);
            double lowerLimit = mean - (AppConstants.STDEVS_BELOW_MEAN_LIMIT_FOR_MOVEMENT * stdev);

            // see if the sample is within the limit
            int dataPoint = m_filteredData.get(m_filteredData.size()-1).m_PPG;
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
        if (m_filteredData.size() > (AppConstants.SAMPLES_PER_SECOND * 3) + 2)
        {
            // start at the start of baseline plus 3 seconds
            int startingRangeIndex = startIndex + (AppConstants.SAMPLES_PER_SECOND * 3);
            int endingRangeIndex = m_filteredData.size() - 3;
            for (int i = startingRangeIndex; i < endingRangeIndex; i++)
            {
                double localPointsStdev = DataMath.getInstance().CalculateStdev(i-2, i + 2, m_filteredData);
                double localSecondsStdev = DataMath.getInstance().CalculateStdev(i - (AppConstants.SAMPLES_PER_SECOND * 3), i, m_filteredData);

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

    public List<Integer> GetFilteredPPGData()
    {
        List<Integer> retData = new ArrayList<>();
        for (int i = 0; i < m_filteredData.size(); i++)
        {
            retData.add(m_filteredData.get(i).m_PPG);
        }
        return retData;
    }

    public void CreateMarker(RealtimeDataMarker.Marker_Type type, int index)
    {
        RealtimeDataMarker marker = new RealtimeDataMarker(type, index);
        m_markers.add(marker);
    }

    public ArrayList<PPG_PressureDataPoint> GetRawData()
    {
        return m_rawData;
    }

    public ArrayList<PPG_PressureDataPoint> GetFilteredData()
    {
        return m_filteredData;
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
        PPG_PressureDataPoint pd = new PPG_PressureDataPoint(PPGSample.intValue(), pressureSample.intValue());
        m_rawData.add(pd);

        // filter the sample and store it in the filtered array
        m_PPGFIRFilter.PutSample(PPGSample);
        int ppgFiltered = (int) m_PPGFIRFilter.GetOutput();

        RealtimePeakValleyDetect.getInstance().AddToDataArray(ppgFiltered);

        // filter the pressure sample
        m_PressureFIRFilter.PutSample(pressureSample);
        double pressureFiltered = m_PressureFIRFilter.GetOutput();

        pd = new PPG_PressureDataPoint(ppgFiltered, pressureFiltered);

        m_filteredData.add(pd);

        RealtimePeakValleyDetect.getInstance().ExecuteRealtimePeakDetection();

    }

}