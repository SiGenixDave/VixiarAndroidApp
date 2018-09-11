package com.vixiar.indicor.Data;

import java.util.ArrayList;
import java.util.List;


public class HeartRateInfo
{
    // //////////////////////////////////////////////////////////////////////////
    // / Attributes
    // //////////////////////////////////////////////////////////////////////////
    // Used to maintain a single instance of the RealtimePeakValleyDetect class
    private static final HeartRateInfo ourInstance = new HeartRateInfo();
    private final String TAG = this.getClass().getSimpleName();
    // Sample rate of the PPG data on the hand held.
    private double m_SampleRateHz;
    // The most recent calculation of the heart rate.
    private double m_CurrentBeatsPerMinute;
    // becomes true when heart rate is stable for m_StableTimeWindowSamples
    private boolean m_HeartRateStable;
    // this value represents the window of interest width in number of samples
    // instead of time, that way indexes can be used instead of time increments
    private int m_StableTimeWindowSamples;
    // A sliding fixed window is used so as soon as the required number of peaks are
    // detected, this index increments past the last index to start looking for a
    // new window of peaks
    private int m_CurrentStartIndex;
    // The heart rate calculation uses the time between peaks to determine the
    // heart rate. This value represents the number of peaks needed to be "seen"
    // before a calculation occurs.
    private int m_NumHeartBeatsToAverage;
    // Stores the average beats per minute calculations
    private List<HistoricalData> m_HistoricalDataList = new ArrayList<>();
    // If the difference between the largest and the smallest average beats per
    // minute exceeds this value, an error is declared.
    private double m_MaxDeviationBeatsPerMinute;
    // The minimum allowed heart rate allowed during verification
    private double m_MinHeartRate;
    // The maximum allowed heart rate allowed during verification
    private double m_MaxHeartRate;
    // Becomes true when InitializeValidation() method called
    private boolean m_AlgorithmInitialized;

    // //////////////////////////////////////////////////////////////////////////
    // / Getters
    // //////////////////////////////////////////////////////////////////////////
/*
    public double getCurrentBeatsPerMinute()
    {
        return m_CurrentBeatsPerMinute;
    }

    public boolean isHeartRateStable()
    {
        return m_HeartRateStable;
    }

    // Parameters are used during validation
    public void InitializeValidation(double sampleRateHz, int numHeartbeatsToAverage, double maxDeviation, double minHeartRate, double maxHeartRate, double stableTimeSecs)
    {
        m_SampleRateHz = sampleRateHz;
        m_NumHeartBeatsToAverage = numHeartbeatsToAverage;
        // The value passed in is +/- X Beats/Minute. The validation algorithm ensures
        // that all heart rates
        // during the window satisfy the following (max HR - min HR) < (2 *
        // maxDeviation)
        m_MaxDeviationBeatsPerMinute = maxDeviation * 2;
        m_MinHeartRate = minHeartRate;
        m_MaxHeartRate = maxHeartRate;

        m_StableTimeWindowSamples = (int) (stableTimeSecs * sampleRateHz);

        // Enforces that this method must be called prior to allowing the calculations
        // to proceed
        m_AlgorithmInitialized = true;
    }

    public void StartRealtimeCalcs(int startIndex)
    {
        m_HistoricalDataList.clear();
        m_CurrentBeatsPerMinute = 0.0;
        m_CurrentStartIndex = startIndex;
        m_HeartRateStable = false;
    }

    // Perform the heart rate calculations. Can be called as often as needed (real time or
    // after all of the data PPG data is received). "m_CurrentStartIndex" will be adjusted on successive
    // calls if this method is invoked after all of the data is received. "m_CurrentStartIndex" will
    // also be adjusted
    public boolean RealtimeHeartRateValidation()
    {

        // Make sure the calculation is initialized and enabled
        if (!m_AlgorithmInitialized)
        {
            return false;
        }

        boolean newHeartRateAvailable = false;
        // This call returns a list of peaks between the start index and the most recent data received
        List<Integer> peaks = BeatProcessing.getInstance().GetItemsBetween(m_CurrentStartIndex, -1,
                RealtimePeakValleyDetect.eSlopeZero.PEAK, RealtimePeakValleyDetect.getInstance().GetPeaksAndValleys());

        // Discard any old samples from history to ensure the window if interest "slides". This also ensures
        // that if any dead time exists (no peaks), they get flushed also
        DiscardOldSamples(RealtimePeakValleyDetect.getInstance().AmountOfData() - 1);

        // The number of peaks detected exceeds the required amount. In order to calculate heart rate, the number of
        // peaks + 1 is required since the difference between is needed. So if m_NumHeartBeatsToAverage is X,
        // then X+1 peaks (p0, p1, ... pX) are needed. The algorithm then calculates heart rate by performing
        // pX - p0 / X
        if (peaks.size() > m_NumHeartBeatsToAverage)
        {

            newHeartRateAvailable = true;

            // Get "p0 and "pX" (see comment above)
            int firstPeakSampleIndex = peaks.get(0);
            int lastPeakSampleIndex = peaks.get(m_NumHeartBeatsToAverage);

            Log.d(TAG, "delta = " + (lastPeakSampleIndex - firstPeakSampleIndex));
            if (lastPeakSampleIndex == firstPeakSampleIndex)
            {
                lastPeakSampleIndex = peaks.get(peaks.size() - 1);
            }
            Log.d("AVG_HR", " firstPeakSampleIndex: " + firstPeakSampleIndex + " lastPeakSampleIndex: " + lastPeakSampleIndex);

            // Calculate the average heart rate over the sample window
            m_CurrentBeatsPerMinute = HeartRate(firstPeakSampleIndex, lastPeakSampleIndex, m_NumHeartBeatsToAverage);

            // Save the heart rate and the sample index (1st peak) where the average heart rate calculation started
            HistoricalData historicalData = new HistoricalData();
            historicalData.heartRate = m_CurrentBeatsPerMinute;
            historicalData.startSampleIndex = firstPeakSampleIndex;
            historicalData.lastSampleIndex = lastPeakSampleIndex;
            m_HistoricalDataList.add(historicalData);

//            Log.d ("HIST", "-------------------------------------------------------" );
//            for (HistoricalData h: m_HistoricalDataList) {
//                Log.d ("HIST", "h.heartRate: " + h.heartRate +
//                                        " h.startSampleIndex: " + h.startSampleIndex +
//                                        " h.lastSampleIndex: " + h.lastSampleIndex );
//            }

            // Verify enough average samples are present (meets or exceeds window length)
            if (EnoughSamplesPresent())
            {

                //Log.d ("AVG_HR", "EnoughSamplesPresent() = true");

                boolean allSamplesWithinRange = VerifyHeartRateInRange();
                boolean allSamplesTwoSigma = VerifyDeviation();

                // Let the outside world know that all hear rate calculations have been validated over
                // the window of interest
                if (allSamplesWithinRange && allSamplesTwoSigma)
                {
                    m_HeartRateStable = true;
                }

            }

            // Prepare for the next window, this ensures that p0 is captured and this X more peaks are needed
            m_CurrentStartIndex = lastPeakSampleIndex;

        }

        return newHeartRateAvailable;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Public Methods
    // //////////////////////////////////////////////////////////////////////////

    // Method purges old historical samples that fall outside the desired window
    private void DiscardOldSamples(int currentSampleIndex)
    {

        if (m_HistoricalDataList.size() == 0)
        {
            return;
        }

        int purgeCount = 0;

        // Scan for old samples that fall outside the desired window
        for (HistoricalData p : m_HistoricalDataList)
        {
            if ((currentSampleIndex - p.startSampleIndex) > (m_StableTimeWindowSamples * 120 / 100))
            {
                purgeCount++;
            }
            else
            {
                break;
            }
        }

        // Keep purging all of the samples that are considered "old" (i.e. fall outside the
        // window time
        while (purgeCount > 0)
        {
            m_HistoricalDataList.remove(0);
            purgeCount--;
        }
    }
*/

    // Calculates the heart rate. The first and last peak is used to calculate the time span. The value returned
    // is in units beats / minute
    public double HeartRate(int firstPeakIndex, int lastPeakIndex, int numHeartBeats)
    {
        if (numHeartBeats < 1)
        {
            return -1.0;
        }

        double timeSpanSecs = (lastPeakIndex - firstPeakIndex) / (double) AppConstants.SAMPLES_PER_SECOND;

        // make sure we're not dividing by 0
        if (timeSpanSecs > 0)
        {
            return numHeartBeats / timeSpanSecs * 60.0;
        }
        else
        {
            return 0;
        }
    }

    public double CalculateEndHeartRateStartingAt(int firstPeakIndex, int numHeartBeatsToAverage)
    {
        double hr;

        // get all of the peaks from the first index till now
        List<Integer> peaks = BeatProcessing.getInstance().GetItemsBetween(firstPeakIndex, -1,
                RealtimePeakValleyDetect.eSlopeZero.PEAK, RealtimePeakValleyDetect.getInstance().GetPeaksAndValleys());

        // if numBeatsToAverage is set to -1, the user wants to calculate the HR using all of the beats
        if (numHeartBeatsToAverage == -1)
        {
            int startIndex = peaks.get(0);
            int endIndex = peaks.get(peaks.size() - 1);
            hr = HeartRate(startIndex, endIndex, peaks.size() - 1);
        }
        else
        {
            // see if there are enough peaks
            if (peaks.size() > numHeartBeatsToAverage)
            {
                int startIndex = peaks.get(peaks.size() - (numHeartBeatsToAverage + 1));
                int endIndex = peaks.get(peaks.size() - 1);
                hr = HeartRate(startIndex, endIndex, numHeartBeatsToAverage);
            }
            else
            {
                // indicat that there's not enough peaks yet
                hr = 0.0;
            }
        }
        return hr;
    }

/*
    // Method determines if enough samples are present in the historical data so that the hear rate
    // data can be verified
    private boolean EnoughSamplesPresent()
    {
        int indexFirst = m_HistoricalDataList.get(0).startSampleIndex;
        int indexLast = m_HistoricalDataList.get(m_HistoricalDataList.size() - 1).lastSampleIndex;

        Log.d("AVG_HR", "... " + indexLast + " ... " + indexFirst);
        boolean answer = false;
        // The time over which the data is validated is converted to samples based on the
        // data sampling frequency
        if ((indexLast - indexFirst) >= m_StableTimeWindowSamples)
        {
            answer = true;
        }

        return answer;
    }

    // Method verifies that all heart rate data currently in historical data falls between min and max
    // allowed heart rate. Returns false if any of this data falls outside of the range
    private boolean VerifyHeartRateInRange()
    {
        boolean inRange = true;

        // Scan all historical data until a heart rate falls outside of the range
        for (HistoricalData p : m_HistoricalDataList)
        {
            if ((p.heartRate < m_MinHeartRate) || (p.heartRate > m_MaxHeartRate))
            {
                Log.d("AVG_HR", "Bad heartrate = " + p.heartRate);
                inRange = false;
                break;
            }
        }

        if (inRange)
        {
            Log.d("AVG_HR", "Range OK");
        }
        return inRange;
    }

    // Verifies that the difference between the max heart rate and the min heart rate in all historical data
    // doesn't exceed the allowed value. Returns true if all is well; false otherwise
    private boolean VerifyDeviation()
    {
        boolean inRange = true;

        double largest = Double.MIN_VALUE;
        double smallest = Double.MAX_VALUE;
        boolean twoSamplesExtracted = false;

        // Scan all historical data and verify the max difference between the largest and smallest
        // heart rate doesn't exceed m_MaxDeviationBeatsPerMinute
        for (HistoricalData p : m_HistoricalDataList)
        {
            if (p.heartRate > largest)
            {
                largest = p.heartRate;
            }
            if (p.heartRate < smallest)
            {
                smallest = p.heartRate;
            }
            if (twoSamplesExtracted)
            {
                if ((largest - smallest) > m_MaxDeviationBeatsPerMinute)
                {
                    inRange = false;
                    Log.d("AVG_HR", "Deviation FALSE = " + (largest - smallest));
                    break;
                }
            }
            // Guarantees that there are at least 2 samples (a min and max is present)
            // before the algorithm above is executed
            twoSamplesExtracted = true;

        }

        // Verify that check was done at least once
        if (!twoSamplesExtracted)
        {
            return false;
        }

        if (inRange)
        {
            Log.d("AVG_HR", "Deviation OK");
        }
        return inRange;
    }
*/

    // Calculates the average heart rate between 2 samples. It first gets all of the peaks between the first and
    // last sample index. It then gets the sample index of first peak and the sample index of the last peak. It
    // then calculates the average heart rate over that time and returns a value in heart beats per minute
    public double GetAvgHRInRange(int firstSampleIndex, int lastSampleIndex, PeaksAndValleys pv, ArrayList<PPG_PressureDataPoint> dataSet)
    {
        List<Integer> peaks = BeatProcessing.getInstance().GetItemsBetween(firstSampleIndex, lastSampleIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);

        if (peaks.size() > 1)
        {
            int firstPeakIndex = peaks.get(0);
            int lastPeakIndex = peaks.get(peaks.size() - 1);

            // Method requires the number of heart beats, since a heart beat is considered one peak to the next
            // peak, the value passed in for the number of methods is the number of peaks - 1
            return HeartRate(firstPeakIndex, lastPeakIndex, peaks.size() - 1);
        }
        else
        {
            return 0;
        }
    }

    // Method calculates the minimum heart rate detected between "firstSampleIndex" and "lastSampleIndex"
    // The numHeartbeatsInAvg is the number of heart beats to use to calculate the heart rate. For example
    // if there are 18 heart beats in sampling interval and the caller wishes to use 3 as the
    // numHeartbeatsInAvg, then there will be six heart rates calculates and the minimum heart rate detected will
    // be returned from this function. Any negative number returned from this function indicate an error
    // was detected
    public double MinimumHeartRate(int firstSampleIndex, int lastSampleIndex, int numHeartbeatsInAvg, PeaksAndValleys pv)
    {
        if (numHeartbeatsInAvg < 1)
        {
            return -1.0;
        }

        List<Integer> peaks = BeatProcessing.getInstance().GetItemsBetween(firstSampleIndex, lastSampleIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);

        final int numPeaks = peaks.size();

        // Need at least 2 peaks to determine a heart beat and calculate a heart rate
        if (numPeaks <= 1)
        {
            return -2.0;
        }
        // Need at least numHeartbeatsInAvg + 1 peaks to have a valid calculation
        else if (numPeaks < numHeartbeatsInAvg + 1)
        {
            return -3.0;
        }

        // Set the minHeart rate to the maximum value so that the first comparison will be the initial
        // minimum value
        double minHeartRate = Double.MAX_VALUE;
        int index = 0;

        // As soon as the index that is accessing the last peak in the averaging window exceeds
        // the array, exit the loop
        while ((index + numHeartbeatsInAvg) < (numPeaks - 1))
        {
            int firstPeakIndex = peaks.get(index);
            int lastPeakIndex = peaks.get(index + numHeartbeatsInAvg);
            double heartRate = HeartRate(firstPeakIndex, lastPeakIndex, numHeartbeatsInAvg);
            // New minimum detected
            if (heartRate < minHeartRate)
            {
                minHeartRate = heartRate;
            }
            // Set the index to get the next set of peaks
            index += numHeartbeatsInAvg;
        }

        return minHeartRate;
    }



    // //////////////////////////////////////////////////////////////////////////
    // / Constructors
    // //////////////////////////////////////////////////////////////////////////
    public static HeartRateInfo getInstance()
    {
        return ourInstance;
    }


    // Used as a data structure to hold historical data when using a real time
    // sliding
    // window to validate heart rate
    private class HistoricalData
    {
        // Calculate heart rate
        public double heartRate;
        // Starting sample (index into data array) where heart rate calculation started
        public int startSampleIndex;
        // Last sample (index into data array) where heart rate calculation ended
        public int lastSampleIndex;
    }
}
