
package com.vixiar.indicor.Data;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class HeartRateInfo {
    private final String TAG = this.getClass().getSimpleName();

    // Used as a data structure to hold historical data when using a real time
    // sliding
    // window to validate heart rate
    private class HistoricalData {

        // Calculate heart rate
        public double heartRate;
        // Starting sample (index into data array) where heart rate calculation started
        public int startSampleIndex;
        // Last sample (index into data array) where heart rate calculation ended
        public int lastSampleIndex;
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
    public double getCurrentBeatsPerMinute() {
        return m_CurrentBeatsPerMinute;
    }

    public boolean isHeartRateStable() {
        return m_HeartRateStable;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Public Methods
    // //////////////////////////////////////////////////////////////////////////

    // Parameters are used during validation
    public void InitializeValidation(double sampleRateHz, int numHeartbeatsToAverage, double maxDeviation,
                                     double minHeartRate, double maxHeartRate, double stableTimeSecs) {
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

    public void StartRealtimeCalcs(int startIndex) {
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
        List<Integer> peaks = PeakValleyDetect.getInstance().GetIndexesBetween(m_CurrentStartIndex, -1,
                PeakValleyDetect.eSlopeZero.PEAK);

        // Discard any old samples from history to ensure the window if interest "slides". This also ensures
        // that if any dead time exists (no peaks), they get flushed also
        DiscardOldSamples(PeakValleyDetect.getInstance().AmountOfData() - 1);

        // The number of peaks detected exceeds the required amount. In order to calculate heart rate, the number of
        // peaks + 1 is required since the difference between is needed. So if m_NumHeartBeatsToAverage is X,
        // then X+1 peaks (p0, p1, ... pX) are needed. The algorithm then calculates heart rate by performing
        // pX - p0 / X
        if (peaks.size() > m_NumHeartBeatsToAverage) {

            newHeartRateAvailable = true;

            // Get "p0 and "pX" (see comment above)
            int firstPeakSampleIndex = peaks.get(0);
            int lastPeakSampleIndex = peaks.get(m_NumHeartBeatsToAverage);

            Log.d(TAG, "delta = " + (lastPeakSampleIndex - firstPeakSampleIndex));
            if (lastPeakSampleIndex == firstPeakSampleIndex)
            {
                lastPeakSampleIndex = peaks.get(peaks.size()-1);
            }
            Log.d ("AVG_HR", " firstPeakSampleIndex: " + firstPeakSampleIndex + " lastPeakSampleIndex: " + lastPeakSampleIndex);


            // Calculate the average heart rate over the sample window
            m_CurrentBeatsPerMinute = CalculateHeartRate(firstPeakSampleIndex, lastPeakSampleIndex,
                    m_NumHeartBeatsToAverage);

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
            if (EnoughSamplesPresent()) {

                //Log.d ("AVG_HR", "EnoughSamplesPresent() = true");

                boolean allSamplesWithinRange = VerifyHeartRateInRange();
                boolean allSamplesTwoSigma = VerifyDeviation();

                // Let the outside world know that all hear rate calculations have been validated over
                // the window of interest
                if (allSamplesWithinRange && allSamplesTwoSigma) {
                    m_HeartRateStable = true;
                }

            }

            // Prepare for the next window, this ensures that p0 is captured and this X more peaks are needed
            m_CurrentStartIndex = lastPeakSampleIndex;

        }

        return newHeartRateAvailable;
    }

    // Calculates the average heart rate between 2 samples. It first gets all of the peaks between the first and
    // last sample index. It then gets the sample index of first peak and the sample index of the last peak. It
    // then calculates the average heart rate over that time and returns a value in heart beats per minute
    public double GetAvgHROverRange(int firstSampleIndex, int lastSampleIndex) {

        List<Integer> peaks = PeakValleyDetect.getInstance().GetIndexesBetween(firstSampleIndex, lastSampleIndex,
                PeakValleyDetect.eSlopeZero.PEAK);

        if (peaks.size() > 1)
        {
            int firstPeakIndex = peaks.get(0);
            int lastPeakIndex = peaks.get(peaks.size() - 1);

            // Method requires the number of heart beats, since a heart beat is considered one peak to the next
            // peak, the value passed in for the number of methods is the number of peaks - 1
            return CalculateHeartRate(firstPeakIndex, lastPeakIndex, peaks.size() - 1);
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
    public double MinimumHeartRate(int firstSampleIndex, int lastSampleIndex, int numHeartbeatsInAvg) {

        if (numHeartbeatsInAvg < 1) {
            return -1.0;
        }

        List<Integer> peaks = PeakValleyDetect.getInstance().GetIndexesBetween(firstSampleIndex, lastSampleIndex,
                PeakValleyDetect.eSlopeZero.PEAK);

        final int numPeaks = peaks.size();

        // Need at least 2 peaks to determine a heart beat and calculate a heart rate
        if (numPeaks <= 1) {
            return -2.0;
        }
        // Need at least numHeartbeatsInAvg + 1 peaks to have a valid calculation
        else if (numPeaks < numHeartbeatsInAvg + 1) {
            return -3.0;
        }

        // Set the minHeart rate to the maximum value so that the first comparison will be the initial
        // minimum value
        double minHeartRate = Double.MAX_VALUE;
        int index = 0;

        // As soon as the index that is accessing the last peak in the averaging window exceeds
        // the array, exit the loop
        while ((index + numHeartbeatsInAvg) < (numPeaks - 1)) {
            int firstPeakIndex = peaks.get(index);
            int lastPeakIndex = peaks.get(index + numHeartbeatsInAvg);
            double heartRate = CalculateHeartRate(firstPeakIndex, lastPeakIndex, numHeartbeatsInAvg);
            // New minimum detected
            if (heartRate < minHeartRate) {
                minHeartRate = heartRate;
            }
            // Set the index to get the next set of peaks
            index += numHeartbeatsInAvg;
        }

        return minHeartRate;
    }

    // Method calculates and returns the minimum peak to valley amplitude seen between "first" and
    // "last" sample indexes. If "last" = -1, then all of the peaks and valleys collected from "first"
    // until when this method is called will be used.
    public double GetMinPAOverRange(int firstSampleIndex, int lastSampleIndex) {

        List<Integer> peaks = PeakValleyDetect.getInstance().GetIndexesBetween(firstSampleIndex, lastSampleIndex,
                PeakValleyDetect.eSlopeZero.PEAK);
        List<Integer> valleys = PeakValleyDetect.getInstance().GetIndexesBetween(firstSampleIndex, lastSampleIndex,
                PeakValleyDetect.eSlopeZero.VALLEY);

        final int numPeaks = peaks.size();
        final int numValleys = valleys.size();

        // Verify that there is at least 1 peak and 1 valley
        if ((numPeaks == 0) || (numValleys == 0)) {
            return -1.0;
        }

        int peakIndex = 0;
        int valleyIndex = 0;
        int peak = peaks.get(peakIndex);
        int valley = valleys.get(valleyIndex);

        // If the first valley occurs in time before the first peak, bump up the index
        // because the algorithm uses the amplitude from Peak to following Valley
        if (valley < peak) {
            valleyIndex++;
        }


        // Set the minimum peak to valley amplitude the maximum value so that the first comparison will be the initial
        // minimum peak to valley amplitude
        double minPeakToValley = Double.MAX_VALUE;

        // Verify the respective indexes don't exceed the array sizes
        while ((peakIndex < numPeaks) && (valleyIndex < numValleys)) {
            peak = peaks.get(peakIndex);
            valley = valleys.get(valleyIndex);
            // Verify valley occurs after peak; just comparing sample times here
            int diff = valley - peak;
            if (diff > 0) {
                // Now get the PPG data at the two points in time
                int peakData = PeakValleyDetect.getInstance().GetData(peak);
                int valleyData = PeakValleyDetect.getInstance().GetData(valley);
                // Calculate the
                int diffData = peakData - valleyData;
                if (diffData < minPeakToValley) {
                    minPeakToValley = diffData;
                }
            } else {
                return -2.0;
            }

            // Prepare to get the next set of data
            peakIndex++;
            valleyIndex++;

        }

        return minPeakToValley;

    }

    // Method calculates and returns the peak to valley amplitude average seen between "first" and
    // "last" sample indexes. If "last" = -1, then all of the peaks and valleys collected from "first"
    // until when this method is called will be used.
    public double GetAvgPAOverRange(int firstIndex, int lastIndex) {

        List<Integer> peaks = PeakValleyDetect.getInstance().GetIndexesBetween(firstIndex, lastIndex,
                PeakValleyDetect.eSlopeZero.PEAK);
        List<Integer> valleys = PeakValleyDetect.getInstance().GetIndexesBetween(firstIndex, lastIndex,
                PeakValleyDetect.eSlopeZero.VALLEY);

        int numPeaks = peaks.size();
        int numValleys = valleys.size();

        if ((numPeaks == 0) || (numValleys == 0)) {
            return -1.0;
        }

        int peakIndex = 0;
        int valleyIndex = 0;
        int peak = peaks.get(peakIndex);
        int valley = valleys.get(valleyIndex);

        if (valley < peak) {
            valleyIndex++;
        }

        double pulseAmplitudeSum = 0.0;
        int peakToValleysCount = 0;

        while ((peakIndex < numPeaks) && (valleyIndex < numValleys)) {
            peak = peaks.get(peakIndex);
            valley = valleys.get(valleyIndex);
            // Verify valley occurs after peak
            int diff = valley - peak;
            if (diff > 0) {
                // Now get the PPG data at the two points in time
                int peakData = PeakValleyDetect.getInstance().GetData(peak);
                int valleyData = PeakValleyDetect.getInstance().GetData(valley);
                int diffData = peakData - valleyData;
                pulseAmplitudeSum += diffData;
                peakToValleysCount++;
            } else {
                return -2.0;
            }
            peakIndex++;
            valleyIndex++;
        }

        return pulseAmplitudeSum / peakToValleysCount;

    }



    // Returns the average heart rage from "endIndex" looking back in time "numBeats" heart beats
    // -1.0 is returned if there are were no heart beats detected from "endIndex" looking back
    public double GetHistoricalAvgHR(int endIndex, int numBeats) {

        if (endIndex < 0) {
            endIndex = PeakValleyDetect.getInstance().AmountOfData() - 1;
        }

        boolean atLeastTwoPeaks = false;
        int timeMarkerN;
        int timeMarkerNPlus1 = endIndex;
        double heartRateAvgSum = 0.0;
        int numHeartbeats = 0;


        // Check if there less peaks (heart beats) than requested to check
        while (numBeats > 0) {
            timeMarkerN = PeakValleyDetect.getInstance().GetPriorDetect(timeMarkerNPlus1, PeakValleyDetect.eSlopeZero.PEAK);

            // timeMarkerN becomes equal to Integer.MAX_VALUE when there are no more historical peaks
            if (timeMarkerN == Integer.MAX_VALUE) {
                break;
            }

            // Make sure we have at least 2 peaks before calculating heart rate
            if (!atLeastTwoPeaks) {
                atLeastTwoPeaks = true;
            }
            else {
                double heartRate = CalculateHeartRate (timeMarkerN, timeMarkerNPlus1, 1);
                heartRateAvgSum += heartRate;
                numHeartbeats++;
                numBeats--;
            }

            timeMarkerNPlus1 = timeMarkerN;

        }

        double heartBeatAvg = -1.0;
        if (numHeartbeats > 0) {
            heartBeatAvg = heartRateAvgSum / numHeartbeats;
        }

        return heartBeatAvg;

    }

    // Returns the average pulse amplitude "peak to valley" from "endIndex" looking back in
    // time "numBeats" heart beats. -1.0 is returned if there were no peak to valleys detected
    // from "endIndex" looking back
    public double GetHistoricalAvgPA(int endIndex, int numBeats) {

        if (endIndex < 0) {
            endIndex = PeakValleyDetect.getInstance().AmountOfData() - 1;
        }

        int timeMarkerPeakN = endIndex;
        int timeMarkerValleyN;
        int numAmplitudes = 0;

        double amplitudeSum = 0.0;

        // Now go back the correct number of heart beats
        while (numBeats > 0) {
            timeMarkerValleyN = PeakValleyDetect.getInstance().GetPriorDetect(timeMarkerPeakN, PeakValleyDetect.eSlopeZero.VALLEY);

            // timeMarkerN becomes equal to Integer.MAX_VALUE when there are no more historical peaks
            if (timeMarkerValleyN == Integer.MAX_VALUE) {
                break;
            }

            timeMarkerPeakN = PeakValleyDetect.getInstance().GetPriorDetect(timeMarkerValleyN, PeakValleyDetect.eSlopeZero.PEAK);

            // timeMarkerN becomes equal to Integer.MAX_VALUE when there are no more historical peaks
            if (timeMarkerPeakN == Integer.MAX_VALUE) {
                break;
            }

            int peak = PeakValleyDetect.getInstance().GetData(timeMarkerPeakN);
            int valley = PeakValleyDetect.getInstance().GetData(timeMarkerValleyN);
            amplitudeSum += (peak - valley);
            numBeats--;
            numAmplitudes++;
        }

        double amplitudeAvg = -1.0;
        if (numAmplitudes > 0) {
            amplitudeAvg = amplitudeSum / numAmplitudes;
        }

        return amplitudeAvg;
    }




    // Method determines if enough samples are present in the historical data so that the hear rate
    // data can be verified
    private boolean EnoughSamplesPresent() {

        int indexFirst = m_HistoricalDataList.get(0).startSampleIndex;
        int indexLast = m_HistoricalDataList.get(m_HistoricalDataList.size() - 1).lastSampleIndex;

        Log.d ("AVG_HR", "... " + indexLast + " ... " + indexFirst);
        boolean answer = false;
        // The time over which the data is validated is converted to samples based on the
        // data sampling frequency
        if ((indexLast - indexFirst) >= m_StableTimeWindowSamples) {
            answer = true;
        }

        return answer;
    }

    // Method purges old historical samples that fall outside the desired window
    private void DiscardOldSamples(int currentSampleIndex) {

        if (m_HistoricalDataList.size() == 0) {
            return;
        }

        int purgeCount = 0;

        // Scan for old samples that fall outside the desired window
        for (HistoricalData p : m_HistoricalDataList) {
            if ((currentSampleIndex - p.startSampleIndex) > (m_StableTimeWindowSamples * 120 /100)) {
                purgeCount++;
            } else {
                break;
            }
        }

        // Keep purging all of the samples that are considered "old" (i.e. fall outside the
        // window time
        while (purgeCount > 0) {
            m_HistoricalDataList.remove(0);
            purgeCount--;
        }
    }

    // Calculates the heart rate. The first and last peak is used to calculate the time span. The value returned
    // is in units beats / minute
    private double CalculateHeartRate(int firstPeakIndex, int lastPeakIndex, int numHeartBeats) {

        if (numHeartBeats < 1) {
            return -1.0;
        }

        double timeSpanSecs = (lastPeakIndex - firstPeakIndex) / m_SampleRateHz;

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

    // Method verifies that all heart rate data currently in historical data falls between min and max
    // allowed heart rate. Returns false if any of this data falls outside of the range
    private boolean VerifyHeartRateInRange() {

        boolean inRange = true;

        // Scan all historical data until a heart rate falls outside of the range
        for (HistoricalData p : m_HistoricalDataList) {
            if ((p.heartRate < m_MinHeartRate) || (p.heartRate > m_MaxHeartRate)) {
                Log.d ("AVG_HR", "Bad heartrate = " + p.heartRate);
                inRange = false;
                break;
            }
        }

        if (inRange) {
            Log.d ("AVG_HR", "Range OK");
        }
        return inRange;
    }

    // Verifies that the difference between the max heart rate and the min heart rate in all historical data
    // doesn't exceed the allowed value. Returns true if all is well; false otherwise
    private boolean VerifyDeviation() {

        boolean inRange = true;

        double largest = Double.MIN_VALUE;
        double smallest = Double.MAX_VALUE;
        boolean twoSamplesExtracted = false;

        // Scan all historical data and verify the max difference between the largest and smallest
        // heart rate doesn't exceed m_MaxDeviationBeatsPerMinute
        for (HistoricalData p : m_HistoricalDataList) {
            if (p.heartRate > largest) {
                largest = p.heartRate;
            }
            if (p.heartRate < smallest) {
                smallest = p.heartRate;
            }
            if (twoSamplesExtracted) {
                if ((largest - smallest) > m_MaxDeviationBeatsPerMinute) {
                    inRange = false;
                    Log.d ("AVG_HR", "Deviation FALSE = " + (largest - smallest));
                    break;
                }
            }
            // Guarantees that there are at least 2 samples (a min and max is present)
            // before the algorithm above is executed
            twoSamplesExtracted = true;

        }

        // Verify that check was done at least once
        if (!twoSamplesExtracted) {
            return false;
        }

        if (inRange) {
            Log.d ("AVG_HR", "Deviation OK");
        }
        return inRange;
    }

    // Returns the root mean square between "startIndex" and "endIndex"
    // -1.0 is returned if "startIndex" or "endIndex" are out of bounds
    public double GetRMSInRange(int startIndex, int endIndex)
    {
        // 1. find number of PPG values between start and end indices
        int numValues = endIndex - startIndex;

        // 2. square all the PPG values within a window (startIndex and endIndex window)
        double sumSquared = 0;
        double avgData = 0;

        for (int i = 0; i < numValues; i++)
        {
            int value = PeakValleyDetect.getInstance().GetData(startIndex + i);
            if (value == -1)
            {
                return -1.0;  // 10 second window is out of bounds
            }

            // 1.5 calculate average of data points.
            avgData += ((double) value / (double) numValues);
        }

        for (int i = 0; i < numValues; i++)
        {
            int value = PeakValleyDetect.getInstance().GetData(startIndex + i);
            if (value == -1)
            {
                return -1.0;  // 10 second window is out of bounds
            }
            // subtract average of data points (#1.5 above) from the data points to shift data closer to zero line
            value -= avgData;
            sumSquared += (value * value);
        }

        // 3. take average of the squared PPG values
        double average = (sumSquared) / numValues;

        // 4. Take square root of the averaged squared PPG values
        return Math.sqrt(average);
    }

    // Returns the minimum root mean square in X seconds window between "startIndex" and "endIndex"
    public double GetMinRMS(int startIndex, int endIndex, int secondsWindow)
    {
        double minRMSValue = Double.MAX_VALUE;
        int startMarker = startIndex;
        int endMarker = startIndex + (int) (secondsWindow * m_SampleRateHz);

        while (endMarker <= endIndex)
        {
            double rmsValue = GetRMSInRange(startMarker, endMarker);
            if (minRMSValue > rmsValue)
            {
                minRMSValue = rmsValue;
            }
            endMarker++;
            startMarker++;
        }

        return minRMSValue;
    }

}
