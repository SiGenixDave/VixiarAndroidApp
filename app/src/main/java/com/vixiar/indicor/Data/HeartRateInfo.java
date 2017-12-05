package com.vixiar.indicor.Data;

import java.util.ArrayList;
import java.util.List;



public class HeartRateInfo {


    private class HistoricalData {

        public double heartRate;
        public int startIndex;

    }


    // //////////////////////////////////////////////////////////////////////////
    // / Constructors
    // //////////////////////////////////////////////////////////////////////////
    public static HeartRateInfo getInstance() {
        return ourInstance;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Attributes
    // //////////////////////////////////////////////////////////////////////////
    // Used to maintain a single instance of the PeakValleyDetect class
    private static final HeartRateInfo ourInstance = new HeartRateInfo();

    // Sample rate of the PPG data on the hand held.
    private double m_SampleRateHz;

    // The most recent calculation of the heart rate.
    private double m_CurrentBeatsPerMinute;

    // becomes true when heart rate is stable for m_StableWindow
    private boolean m_HeartRateStable;

    private int m_StableTimeIndexesWindow;

    // A sliding fixed window is used so as soon as the required number of peaks are
    // detected, this index increments past the last index to start looking for a
    // new window of peaks
    private int m_CurrentStartIndex;

    // The heart rate calculation uses the time between peaks to determine the
    // heart rate. This value represents the number of peaks needed to be "seen"
    // before a calculation occurs
    private int m_NumPeaksForAverageCalc;

    // Stores the average beats per minute calculations
    private List<HistoricalData> m_HistoricalDataList = new ArrayList<>();

    // If the difference between the largest and the smallest average beats per minute
    // exceeds this value, an error is declared
    private double m_MaxDeviationBeatsPerMinute;

    private double m_MinHeartRate;
    private double m_MaxHeartRate;

    // Becomes true when Initialize() method called
    private boolean m_AlgorithmInitialized;


    // //////////////////////////////////////////////////////////////////////////
    // / Getters
    // //////////////////////////////////////////////////////////////////////////
    public double getCurrentBeatsPerMinute() {
        return m_CurrentBeatsPerMinute;
    }

    public boolean isHeartRateStable() {
        return m_HeartRateStable;
    }



    // //////////////////////////////////////////////////////////////////////////
    // / Public Methods
    // //////////////////////////////////////////////////////////////////////////

    // Parameters are used to determine
    public void Initialize(double sampleRateHz, int numPeaksForAverage, double maxDeviation,
                           double minHeartRate, double maxHeartRate, double stableTimeSecs) {
        m_SampleRateHz = sampleRateHz;
        m_NumPeaksForAverageCalc = numPeaksForAverage;
        m_MaxDeviationBeatsPerMinute = maxDeviation * 2;
        m_MinHeartRate = minHeartRate;
        m_MaxHeartRate = maxHeartRate;

        m_StableTimeIndexesWindow = (int)(stableTimeSecs * sampleRateHz);

        // Enforces that this method must be called prior to allowing the calculations to proceed
        m_AlgorithmInitialized = true;
    }

    public void StartRealtimeCalcs (int startIndex) {
        m_HistoricalDataList.clear();
        m_CurrentBeatsPerMinute = 0.0;
        m_CurrentStartIndex = startIndex;
        m_HeartRateStable = false;
    }

    // Perform the heart rate calculations. Can be called as often as needed (real time or
    // after all of the data PPG data is received). "m_CurrentStartIndex" will be adjusted on successive
    // calls if this method is invoked after all of the data is received. "m_CurrentStartIndex" will
    // also be adjusted
    public boolean HeartRateValidation() {

        // Make sure the calculation is initialized and enabled
        if (!m_AlgorithmInitialized) {
            return false;
        }

        boolean newHeartRateAvailable = false;
        // This call returns a list of peaks between the start index and the most recent data received
        List<Integer> peaks = PeakValleyDetect.getInstance().PeakIndexesBetween(m_CurrentStartIndex, -1);

        if (peaks.size() >= m_NumPeaksForAverageCalc) {

            newHeartRateAvailable = true;

            int startPeakIndex = peaks.get(0);
            int lastPeakIndex = peaks.get(m_NumPeaksForAverageCalc - 1);

            m_CurrentBeatsPerMinute = CalculateHeartRate (startPeakIndex, lastPeakIndex, m_NumPeaksForAverageCalc);

            HistoricalData historicalData = new HistoricalData();
            historicalData.heartRate = m_CurrentBeatsPerMinute;
            historicalData.startIndex = startPeakIndex;

            m_HistoricalDataList.add(historicalData);

            if (EnoughSamplesPresent()) {

                PurgeOldSamples();

                boolean allSamplesWithinRange = VerifyHeartRateInRange();
                boolean allSamplesTwoSigma = VerifyDeviation();

                if (allSamplesWithinRange && allSamplesTwoSigma) {
                    m_HeartRateStable = true;
                }

            }

            // Prepare for the next window
            m_CurrentStartIndex = lastPeakIndex;

        }

        return newHeartRateAvailable;
    }

    public double CalculateAverageHeartRate(int firstIndex, int lastIndex) {

        List<Integer> peaks = PeakValleyDetect.getInstance().PeakIndexesBetween(firstIndex, lastIndex);
        int firstPeakIndex = peaks.get(0);
        int lastPeakIndex = peaks.get(peaks.size() - 1);

        double heartRateAverage = CalculateHeartRate(firstPeakIndex, lastPeakIndex, peaks.size());

        return heartRateAverage;

    }

    private boolean EnoughSamplesPresent () {

        int indexFirst = m_HistoricalDataList.get(0).startIndex;
        int indexLast = m_HistoricalDataList.get(m_HistoricalDataList.size() - 1).startIndex;

        boolean answer = false;
        if ((indexLast - indexFirst) >= m_StableTimeIndexesWindow) {
            answer = true;
        }

        return answer;
    }

    private void PurgeOldSamples() {

        int indexLast = m_HistoricalDataList.get(m_HistoricalDataList.size() - 1).startIndex;
        int purgeCount = 0;

        for (HistoricalData p : m_HistoricalDataList)
        {
            int index = p.startIndex;
            if ((indexLast - index) > m_StableTimeIndexesWindow) {
                purgeCount++;
            }
            else {
                break;
            }
        }

        while (purgeCount > 0) {
            m_HistoricalDataList.remove(0);
            purgeCount--;
        }
    }

    private double CalculateHeartRate(int firstPeakIndex, int lastPeakIndex, int numPeaks) {

        if (numPeaks <= 1) {
            return Double.MIN_VALUE;
        }

        numPeaks--;

        double timeSpanSecs = (lastPeakIndex - firstPeakIndex) / m_SampleRateHz;

        return numPeaks / timeSpanSecs * 60.0;
    }


    private boolean VerifyHeartRateInRange() {

        boolean inRange = true;

        for (HistoricalData p : m_HistoricalDataList)
        {
            if ((p.heartRate < m_MinHeartRate) || (p.heartRate > m_MaxHeartRate)) {

                inRange = false;
                break;
            }
        }

        return inRange;
    }

    private boolean VerifyDeviation() {

        boolean inRange = true;

        double largest = Double.MIN_VALUE;
        double smallest = Double.MAX_VALUE;
        boolean twoSamplesExtracted = false;
        for (HistoricalData p : m_HistoricalDataList)
        {
            if (p.heartRate > largest) {
                largest = p.heartRate;
            }
            if (p.heartRate < smallest) {
                smallest = p.heartRate;
            }
            if (twoSamplesExtracted)
            {
                if ((largest - smallest) > m_MaxDeviationBeatsPerMinute) {
                    inRange = false;
                    break;
                }
            }
            twoSamplesExtracted = true;

        }

        return inRange;
    }

}
