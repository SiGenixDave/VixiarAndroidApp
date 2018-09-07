package com.vixiar.indicor.Data;//import android.util.Log;

//import java.util.ArrayList;

import java.util.*;

/**
 * Created by gyurk on 11/15/2017.
 */

/* scaling info
pressure sensor Vout = VS*[(0.1533*P) + 0.053]  ...  Vs = 5.0V, P is pressure in kPa
1 kPa = 0.133322368 mmHg
circuit board 0V at pressure sensor = 3V at A/D. 5V at pressure sensor = 0V at A/D
A/D 3.3V = 2048 counts
doing the math, p(mmHg) = (-0.0263 * counts) + 46.335

*/
public class RealtimeData
{
    private ArrayList<RealtimeDataSample> m_rawData = new ArrayList<RealtimeDataSample>();
    private ArrayList<RealtimeDataSample> m_filteredData = new ArrayList<RealtimeDataSample>();
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
        double pressure_value;
        int pressure_counts;
        int ppg_value;
        for (int i = 1; i < new_data.length; i += 4)
        {
            // convert the a/d counts from the handheld to pressure in mmHg
            pressure_counts = (256 * (new_data[i + 2] & 0xFF)) + (new_data[i + 3] & 0xFF);
            pressure_value = ((double) pressure_counts * (-0.0263)) + 46.726;
            if (pressure_value < 0.0)
            {
                pressure_value = 0.0;
            }

            ppg_value = (256 * (new_data[i] & 0xFF)) + (new_data[i + 1] & 0xFF);

            RealtimeDataSample pd = new RealtimeDataSample(ppg_value, pressure_value);
            m_rawData.add(pd);

            // filter the sample and store it in the filtered array
            m_PPGFIRFilter.PutSample(ppg_value);
            int ppgFiltered = (int) m_PPGFIRFilter.GetOutput();

            RealtimePeakValleyDetect.getInstance().AddToDataArray(ppgFiltered);
            pd = new RealtimeDataSample(ppgFiltered, pressure_value);

            m_filteredData.add(pd);
        }

        RealtimePeakValleyDetect.getInstance().ExecuteRealtimePeakDetection();

    }

    public int GetCurrentDataIndex()
    {
        return m_rawData.size() - 1;
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
        boolean isMovement = false;

        // make sure there is enough data collected based on the lookback window setting
        if ((m_filteredData.size() - startIndex) > (AppConstants.LOOKBACK_SECONDS_FOR_MOVEMENT * AppConstants.SAMPLES_PER_SECOND))
        {
            // get the standard deviation for the last 2 seconds
            int startCheckIndex = m_filteredData.size() - (AppConstants.LOOKBACK_SECONDS_FOR_MOVEMENT * AppConstants.SAMPLES_PER_SECOND);
            int endCheckIndex = m_filteredData.size() - 1;

            for (int i = startCheckIndex; i < endCheckIndex - 1; i++)
            {
                if (m_filteredData.get(i).m_PPG > AppConstants.PPG_AMPLITUDE_LIMIT_FOR_MOVEMENT)
                {
                    isMovement = true;
                    break;
                }
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
        return false;
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

    public ArrayList<RealtimeDataSample> GetRawData()
    {
        return m_rawData;
    }

    public ArrayList<RealtimeDataSample> GetFilteredData()
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
        RealtimeDataSample pd = new RealtimeDataSample(PPGSample.intValue(), pressureSample.intValue());
        m_rawData.add(pd);

        // filter the sample and store it in the filtered array
        m_PPGFIRFilter.PutSample(PPGSample);
        int ppgFiltered = (int) m_PPGFIRFilter.GetOutput();

        RealtimePeakValleyDetect.getInstance().AddToDataArray(ppgFiltered);

        // filter the pressure sample
        m_PressureFIRFilter.PutSample(pressureSample);
        double pressureFiltered = m_PressureFIRFilter.GetOutput();

        pd = new RealtimeDataSample(ppgFiltered, pressureFiltered);

        m_filteredData.add(pd);

        RealtimePeakValleyDetect.getInstance().ExecuteRealtimePeakDetection();

    }

}