package com.vixiar.indicor.Data;

import java.util.ArrayList;
import java.util.List;

import static com.vixiar.indicor.Data.AppConstants.SAMPLES_PER_SECOND;

public class BeatProcessing
{
    private static final BeatProcessing ourInstance = new BeatProcessing();

    public static BeatProcessing getInstance()
    {
        return ourInstance;
    }

    // return the peaks and valleys that are contained within the two indices
    // the first item after the startIndex will be a peak
    // and the last item will be the last valley that is followed by a peak which is before the endIndex
    public PeaksAndValleys GetPeakAndValleyIndicesInRangeHarryMethod(int startIndex, int endIndex, PeaksAndValleys pvIn, ArrayList<RealtimeDataSample> dataSet)
    {
        PeaksAndValleys pv = new PeaksAndValleys();

        List<Integer> peaks = GetItemsBetween(startIndex, endIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pvIn);
        List<Integer> valleys = GetItemsBetween(startIndex, endIndex, RealtimePeakValleyDetect.eSlopeZero.VALLEY, pvIn);

        pv.peaks = peaks;
        pv.valleys = valleys;

        if (peaks.size() > 0 && valleys.size() > 0)
        {
            // find the first valley after the first peak
            while (peaks.size() > 0 && valleys.size() > 0 && valleys.get(0) < peaks.get(0))
            {
                valleys.remove(0);
            }

            // now make sure the last item is a peak
            while (peaks.size() > 0 && valleys.size() > 0 && peaks.get(peaks.size() - 1) < valleys.get(valleys.size() - 1))
            {
                valleys.remove(valleys.size() - 1);
            }

            // now remove the last peak
            peaks.remove(peaks.size() - 1);
        }

        return pv;
    }

    // return the peaks and valleys that are contained within the two indices
    public PeaksAndValleys GetPeakAndValleyIndicesInRangeCompleteMethod(int startIndex, int endIndex, PeaksAndValleys pvIn)
    {
        List<Integer> peaks = GetItemsBetween(startIndex, endIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pvIn);
        List<Integer> valleys = GetItemsBetween(startIndex, endIndex, RealtimePeakValleyDetect.eSlopeZero.VALLEY, pvIn);

        PeaksAndValleys pv = new PeaksAndValleys();
        pv.peaks = peaks;
        pv.valleys = valleys;

        return pv;
    }

    // returns the current number of peaks detected
    public int NumberOfPeaks(PeaksAndValleys pv)
    {
        return pv.peaks.size();
    }

    // Searches either the peak or valley array and returns the index of the peak or valley after
    // the dataIndex
    public int GetNextItem(int startIndex, RealtimePeakValleyDetect.eSlopeZero type, PeaksAndValleys pv)
    {
        List<Integer> peakOrValleyList;

        if (type == RealtimePeakValleyDetect.eSlopeZero.PEAK)
        {
            peakOrValleyList = pv.peaks;
        }
        else if (type == RealtimePeakValleyDetect.eSlopeZero.VALLEY)
        {
            peakOrValleyList = pv.valleys;
        }
        else
        {
            System.out.println("Error in " + getCurrentMethodName());
            return -1;
        }

        // start the scanning the desired list and find the data index where the next
        // peak or valley was found. If there are transitions in the list, MAX_VALUE will be
        // returned
        int currentPeakOrValleyIndex = -1;
        boolean bFound = false;
        int i = 0;

        while (i < peakOrValleyList.size() && !bFound)
        {
            currentPeakOrValleyIndex = peakOrValleyList.get(i);

            // peak or valley "just to the right" of the desired index was found, break out of here
            if (currentPeakOrValleyIndex > startIndex)
            {
                bFound = true;
            }
            i++;
        }

        // The end of the array was reached without finding a transition, inform the
        // calling function
        if (!bFound)
        {
            currentPeakOrValleyIndex = -1;
        }

        return currentPeakOrValleyIndex;
    }

    // Searches either the peak or valley array and returns the index of the peak or valley prior to
    // the dataIndex
    public int GetPriorItem(int dataIndex, RealtimePeakValleyDetect.eSlopeZero type, PeaksAndValleys pv)
    {
        List<Integer> transitionList;

        if (type == RealtimePeakValleyDetect.eSlopeZero.PEAK)
        {
            transitionList = pv.peaks;
        }
        else if (type == RealtimePeakValleyDetect.eSlopeZero.VALLEY)
        {
            transitionList = pv.valleys;
        }
        else
        {
            System.out.println("Error in " + getCurrentMethodName());
            return -1;
        }

        // start the scanning the desired list and find the data index where the previous
        // peak or valley was found. If there are transitions in the list, MAX_VALUE will be
        // returned
        int dataTransitionIndex = Integer.MIN_VALUE;
        int transitionIndex = transitionList.size() - 1;

        while (transitionIndex >= 0)
        {
            dataTransitionIndex = transitionList.get(transitionIndex);
            // peak or valley "just to the left" of the desired Index was found, break out of here
            if (dataTransitionIndex < dataIndex)
            {
                break;
            }
            transitionIndex--;
        }

        // The beginning of the array was reached without finding a transition, inform the
        // calling function
        if (transitionIndex == -1)
        {
            System.out.println("Error in " + getCurrentMethodName());
            dataTransitionIndex = -1;
        }

        return dataTransitionIndex;
    }

    // returns the current number of peaks detected between indices
    public List<Integer> GetItemsBetween(int startIndex, int endIndex, RealtimePeakValleyDetect.eSlopeZero type, PeaksAndValleys pv)
    {
        List<Integer> transitionIndexList = new ArrayList<>();
        List<Integer> transitionList = new ArrayList<>();

        int numTransitions;
        if (type == RealtimePeakValleyDetect.eSlopeZero.PEAK)
        {
            transitionList = pv.peaks;
        }
        else if (type == RealtimePeakValleyDetect.eSlopeZero.VALLEY)
        {
            transitionList = pv.valleys;
        }

        numTransitions = transitionList.size();

        if (numTransitions != 0)
        {
            // when endIndex = -1, the caller wants to find peaks all the way to
            // the end of the list
            if (endIndex == -1)
            {
                endIndex = transitionList.get(numTransitions - 1);
            }

            for (int index = 0; index < numTransitions; index++)
            {
                int currentIndex = transitionList.get(index);
                if ((currentIndex >= startIndex) && (currentIndex <= endIndex))
                {
                    transitionIndexList.add(currentIndex);
                }
            }
        }

        return transitionIndexList;
    }

    // returns the current number of valleys detected
    public int NumberOfValleys(PeaksAndValleys pv)
    {
        return pv.valleys.size();
    }

    public double GetMinPAAvg3(int startIndex, int endIndex, PeaksAndValleys pv, ArrayList<RealtimeDataSample> dataSet)
    {
        double minPA = -1.0;

        ValueAndLocation vlMinPA;
        ValueAndLocation vlEndPA;

        // see if the minPA is endPA
        vlMinPA = GetMinPAInRange(startIndex, endIndex, pv, dataSet);
        vlEndPA = GetEndPA(endIndex, pv, dataSet);

        if (vlEndPA.location != -1 && vlMinPA.location != -1)
        {
            if (vlEndPA.location == vlMinPA.location)
            {
                // if the minPA and endPA are the same, we need to return the avg of the last 3 PAs at the end
                minPA = GetAvgPAHistorical(endIndex, 3, pv, dataSet);
            }
            else
            {
                // otherwise return the avg of the minPA, the beat before it, and the beat after it
                int previousPeakLocation = GetPriorItem(vlMinPA.location, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);
                int previousPreviousPeakLocation = GetPriorItem(previousPeakLocation, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);
                minPA = GetAvgPAFuture(previousPreviousPeakLocation, 3, pv, dataSet);
            }
        }
        return minPA;
    }

    public double GetPh1PAAvg3(int ph1PAPeakIndex, int vStartIndex, PeaksAndValleys pv, ArrayList<RealtimeDataSample> dataSet)
    {
        double ph1PAAvg3;

        int previousPeakIndex = GetPriorItem(ph1PAPeakIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);

        // see if the Ph1PA is first after vStart
        if (previousPeakIndex < vStartIndex)
        {
            // if the previous peak before ph1 peak is before vstart, then the ph1 peak is the first peak after vstart
            // get the avg of the next 3
            ph1PAAvg3 = GetAvgPAFuture(ph1PAPeakIndex, 3, pv, dataSet);
        }
        else
        {
            // otherwise get the avg of 3 starting with the one before the peak
            ph1PAAvg3 = GetAvgPAFuture(previousPeakIndex, 3, pv, dataSet);
        }
        return ph1PAAvg3;
    }

    public double GetPh4PAAvg3(int ph4PAPeakIndex, int vEndIndex, PeaksAndValleys pv, ArrayList<RealtimeDataSample> dataSet)
    {
        double ph4PAAvg3;

        int previousPeakIndex = GetPriorItem(ph4PAPeakIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);
        int nextPeakIndex = GetNextItem(ph4PAPeakIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);
        int thisValleyIndex = GetNextItem(ph4PAPeakIndex, RealtimePeakValleyDetect.eSlopeZero.VALLEY, pv);
        int nextValleyIndex = GetNextItem(nextPeakIndex, RealtimePeakValleyDetect.eSlopeZero.VALLEY, pv);
        int vEndPlus2Point5Seconds = vEndIndex + (int) (SAMPLES_PER_SECOND * 2.5);
        int vEndPlus12Point5Seconds = vEndIndex + (int) (SAMPLES_PER_SECOND * 12.5);

        if ((ph4PAPeakIndex > vEndPlus2Point5Seconds) && (previousPeakIndex < vEndPlus2Point5Seconds))
        {
            // see if the Ph4 PA peak is first peak after vEnd + 2.5 sec,
            // then average with next 2 beats
            ph4PAAvg3 = GetAvgPAFuture(ph4PAPeakIndex, 3, pv, dataSet);
        }
        else if ((ph4PAPeakIndex < vEndPlus12Point5Seconds) && (thisValleyIndex < vEndPlus12Point5Seconds)
                && ((nextValleyIndex == -1) || (nextValleyIndex > vEndPlus12Point5Seconds)))
        {
            // if the Ph4 PA peak is the last beat before vEnd + 12.5 sec
            // then average with the previous 2 beats
            ph4PAAvg3 = GetAvgPAHistorical(ph4PAPeakIndex, 3, pv, dataSet);
        }
        else
        {
            // otherwise average Ph4 peak with previous and next beats
            ph4PAAvg3 = GetAvgPAFuture(previousPeakIndex, 3, pv, dataSet);
        }
        return ph4PAAvg3;
    }

    // Returns the root mean square between "startIndex" and "endIndex"
    // -1.0 is returned if "startIndex" or "endIndex" are out of bounds
    public double GetRMSInRange(int startIndex, int endIndex, ArrayList<RealtimeDataSample> dataSet)
    {
        // 1. find number of PPG values between start and end indices
        int numValues = endIndex - startIndex;

        // 2. square all the PPG values within a window (startIndex and endIndex window)
        double sumSquared = 0;
        double avgData = 0;

        for (int i = 0; i < numValues; i++)
        {
            int value = dataSet.get(startIndex + i).m_PPG;
            if (value == -1)
            {
                System.out.println("Error in " + getCurrentMethodName());
                return -1.0;  // 10 second window is out of bounds
            }

            // 1.5 calculate average of data points.
            avgData += ((double) value / (double) numValues);
        }

        for (int i = 0; i < numValues; i++)
        {
            int value = dataSet.get(startIndex + i).m_PPG;
            if (value == -1)
            {
                System.out.println("Error in " + getCurrentMethodName());
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
    public double GetMinRMS(int startIndex, int endIndex, int secondsWindow, ArrayList<RealtimeDataSample> dataSet)
    {
        double minRMSValue = Double.MAX_VALUE;
        int startMarker = startIndex;
        int endMarker = startIndex + (secondsWindow * SAMPLES_PER_SECOND);

        while (endMarker <= endIndex)
        {
            double rmsValue = GetRMSInRange(startMarker, endMarker, dataSet);
            if (minRMSValue > rmsValue)
            {
                minRMSValue = rmsValue;
            }
            endMarker++;
            startMarker++;
        }
        return minRMSValue;
    }

    public ValueAndLocation GetEndPA(int endIndex, PeaksAndValleys pvIn, ArrayList<RealtimeDataSample> dataSet)
    {
        ValueAndLocation vl = new ValueAndLocation();

        vl.location = -1;
        vl.value = -1.0;

        // get all the peaks and valleys 5 seconds before the end index
        int startIndex = endIndex - (SAMPLES_PER_SECOND * 5);

        PeaksAndValleys pv = GetPeakAndValleyIndicesInRangeHarryMethod(startIndex, endIndex, pvIn, dataSet);

        if (pv.peaks.size() > 0 && pv.valleys.size() > 0)
        {
            double endPA = dataSet.get(pv.peaks.get(pv.peaks.size() - 1)).m_PPG - dataSet.get(pv.valleys.get(pv.valleys.size() - 1)).m_PPG;

            vl.value = endPA;
            vl.location = pv.peaks.get(pv.peaks.size() - 1);
        }
        return vl;
    }


    // Method calculates and returns the minimum peak to valley amplitude seen between "first" and
    // "last" sample indexes. If "last" = -1, then all of the peaks and valleys collected from "first"
    // until when this method is called will be used.
    // the location returned will be the peak of the minPA pulse cycle
    public ValueAndLocation GetMinPAInRange(int startIndex, int endIndex, PeaksAndValleys pvIn, ArrayList<RealtimeDataSample> dataSet)
    {
        ValueAndLocation vl = new ValueAndLocation();
        PeaksAndValleys pv = GetPeakAndValleyIndicesInRangeHarryMethod(startIndex, endIndex, pvIn, dataSet);

        // Verify that there is at least 1 peak and 1 valley
        if ((pv.valleys.size() == 0) || (pv.peaks.size() == 0))
        {
            System.out.println("Error in " + getCurrentMethodName());
            vl.value = -1.0;
            vl.location = -1;
            return vl;
        }

        // Set the minimum peak to valley amplitude the maximum value so that the first comparison will be the initial
        // minimum peak to valley amplitude
        double minPeakToValley = Double.MAX_VALUE;

        int nPeakIndex = 0;
        int nValleyIndex = 0;
        int peak, valley;

        // Verify the respective indexes don't exceed the array sizes
        while ((nPeakIndex < pv.peaks.size()) && (nValleyIndex < pv.valleys.size()))
        {
            peak = pv.peaks.get(nPeakIndex);
            valley = pv.valleys.get(nValleyIndex);

            // Verify valley occurs after peak; just comparing sample times here
            int diff = valley - peak;
            if (diff > 0)
            {
                // Now get the PPG data at the two points in time
                int peakData = dataSet.get(peak).m_PPG;
                int valleyData = dataSet.get(valley).m_PPG;
                // Calculate the
                int diffData = peakData - valleyData;
                if (diffData < minPeakToValley)
                {
                    minPeakToValley = diffData;
                    vl.location = peak;
                }
            }
            else
            {
                System.out.println("Error in " + getCurrentMethodName());
                vl.value = -2.0;
                vl.location = -1;
                return vl;
            }

            // Prepare to get the next set of data
            nPeakIndex++;
            nValleyIndex++;
        }
        vl.value = minPeakToValley;
        return vl;
    }

    // Method calculates and returns the peak to valley amplitude of a maximum peak seen between "first" and
    // "last" sample indexes. If "last" = -1, then all of the peaks and valleys collected from "first"
    // until when this method is called will be used.
    public ValueAndLocation GetMaxPAInRange(int startIndex, int endIndex, PeaksAndValleys pvIn, ArrayList<RealtimeDataSample> dataSet)
    {
        ValueAndLocation vl = new ValueAndLocation();
        PeaksAndValleys pv = GetPeakAndValleyIndicesInRangeHarryMethod(startIndex, endIndex, pvIn, dataSet);

        // Verify that there is at least 1 peak and 1 valley
        if ((pv.valleys.size() == 0) || (pv.peaks.size() == 0))
        {
            System.out.println("Error in " + getCurrentMethodName());
            vl.value = -1.0;
            return vl;
        }

        // Set the max peak to valley amplitude the min value so that the first comparison will be the initial
        // max peak to valley amplitude
        double maxAmplitude = Double.MIN_VALUE;

        int nPeakIndex = 0;
        int nValleyIndex = 0;
        int peak, valley;

        // Verify the respective indexes don't exceed the array sizes
        while ((nPeakIndex < pv.peaks.size()) && (nValleyIndex < pv.valleys.size()))
        {
            peak = pv.peaks.get(nPeakIndex);
            valley = pv.valleys.get(nValleyIndex);

            // Verify valley occurs after peak; just comparing sample times here
            int diff = valley - peak;
            if (diff > 0)
            {
                // Now get the PPG data at the two points in time
                int peakAmplitude = dataSet.get(peak).m_PPG;
                int valleyAmplitude = dataSet.get(valley).m_PPG;

                // Calculate the amplitude
                int amplitude = peakAmplitude - valleyAmplitude;
                if (amplitude > maxAmplitude)
                {
                    maxAmplitude = amplitude;
                    vl.location = peak;
                }
            }
            else
            {
                System.out.println("Error in " + getCurrentMethodName());
                vl.value = -2.0;
                return vl;
            }

            // Prepare to get the next set of data
            nPeakIndex++;
            nValleyIndex++;
        }
        vl.value = maxAmplitude;
        return vl;
    }

    // calculate the average of 3 peaks after VStart
    // if the Phase1 Peak
    public double GetPhase1PAAvg3(int vStart)
    {

        return 0.0;
    }

    // Method calculates and returns the peak to valley amplitude average seen between "first" and
    // "last" sample indexes. If "last" = -1, then all of the peaks and valleys collected from "first"
    // until when this method is called will be used.
    public double GetAvgPAInRange(int firstIndex, int lastIndex, PeaksAndValleys pvIn, ArrayList<RealtimeDataSample> dataSet)
    {
        PeaksAndValleys pv = GetPeakAndValleyIndicesInRangeHarryMethod(firstIndex, lastIndex, pvIn, dataSet);

        int numPeaks = pv.peaks.size();
        int numValleys = pv.valleys.size();

        if ((numPeaks == 0) || (numValleys == 0))
        {
            System.out.println("Error in " + getCurrentMethodName());
            return -1.0;
        }

        int peakHandle = 0;
        int valleyHandle = 0;
        int peakIndex = pv.peaks.get(peakHandle);
        int valleyIndex = pv.valleys.get(valleyHandle);

        if (valleyIndex < peakIndex)
        {
            valleyHandle++;
        }

        double pulseAmplitudeSum = 0.0;
        int peakToValleysCount = 0;

        while ((peakHandle < numPeaks) && (valleyHandle < numValleys))
        {
            peakIndex = pv.peaks.get(peakHandle);
            valleyIndex = pv.valleys.get(valleyHandle);
            // Verify valley occurs after peak
            int diff = valleyIndex - peakIndex;
            if (diff > 0)
            {
                // Now get the PPG data at the two points in time
                int peakData = dataSet.get(peakIndex).m_PPG;
                int valleyData = dataSet.get(valleyIndex).m_PPG;
                int diffData = peakData - valleyData;
                pulseAmplitudeSum += diffData;
                peakToValleysCount++;
            }
            else
            {
                System.out.println("Error in " + getCurrentMethodName());
                return -2.0;
            }
            peakHandle++;
            valleyHandle++;
        }
        return pulseAmplitudeSum / peakToValleysCount;
    }

    // Returns the average heart rage from "endIndex" looking back in time "numBeats" heart beats
    // -1.0 is returned if there are were no heart beats detected from "endIndex" looking back
    public double GetAvgHRHistorical(int endIndex, int numBeats, PeaksAndValleys pv, ArrayList<RealtimeDataSample> dataSet)
    {
        if (endIndex < 0)
        {
            endIndex = RealtimePeakValleyDetect.getInstance().AmountOfData() - 1;
        }

        boolean atLeastTwoPeaks = false;
        int timeMarkerN;
        int timeMarkerNPlus1 = endIndex;
        double heartRateAvgSum = 0.0;
        int numHeartbeats = 0;

        // Check if there less peaks (heart beats) than requested to check
        while (numBeats > 0)
        {
            timeMarkerN = GetPriorItem(timeMarkerNPlus1, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);

            // timeMarkerN becomes equal to Integer.MAX_VALUE when there are no more historical peaks
            if (timeMarkerN == Integer.MAX_VALUE)
            {
                break;
            }

            // Make sure we have at least 2 peaks before calculating heart rate
            if (!atLeastTwoPeaks)
            {
                atLeastTwoPeaks = true;
            }
            else
            {
                double heartRate = HeartRateInfo.getInstance().HeartRate(timeMarkerN, timeMarkerNPlus1, 1);
                heartRateAvgSum += heartRate;
                numHeartbeats++;
                numBeats--;
            }
            timeMarkerNPlus1 = timeMarkerN;
        }

        double heartBeatAvg = -1.0;
        if (numHeartbeats > 0)
        {
            heartBeatAvg = heartRateAvgSum / numHeartbeats;
        }

        return heartBeatAvg;
    }

    public double GetAvgPAHistorical(int endIndex, int numBeatsOfHistory, PeaksAndValleys pvIn, ArrayList<RealtimeDataSample> dataSet)
    {
        int startIndex = GetIndexOfNthPriorPeak(numBeatsOfHistory, endIndex, pvIn);

        PeaksAndValleys pv = GetPeakAndValleyIndicesInRangeCompleteMethod(startIndex, endIndex, pvIn);
        int nTotal = 0;

        if (pv.peaks.size() < numBeatsOfHistory || pv.valleys.size() < numBeatsOfHistory)
        {
            System.out.println("Error in " + getCurrentMethodName());
            return -1.0;
        }
        for (int j = 0; j < numBeatsOfHistory; j++)
        {
            nTotal += dataSet.get(pv.peaks.get(j)).m_PPG - dataSet.get(pv.valleys.get(j)).m_PPG;
        }
        return nTotal / numBeatsOfHistory;
    }

    public double GetAvgPAFuture(int startIndex, int numBeatsOfFuture, PeaksAndValleys pvIn, ArrayList<RealtimeDataSample> dataSet)
    {
        int endIndex = startIndex;

        for (int i = 0; i < numBeatsOfFuture; i++)
        {
            endIndex = GetNextItem(endIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pvIn);
            if (endIndex == -1)
            {
                System.out.println("#1 Error in " + getCurrentMethodName());
                return -1.0;
            }
        }


        PeaksAndValleys pv = GetPeakAndValleyIndicesInRangeCompleteMethod(startIndex, endIndex, pvIn);

        if (pv.peaks.size() >= numBeatsOfFuture && pv.valleys.size() >= numBeatsOfFuture)
        {
            int nTotal = 0;
            for (int j = 0; j < numBeatsOfFuture; j++)
            {
                nTotal += dataSet.get(pv.peaks.get(j)).m_PPG - dataSet.get(pv.valleys.get(j)).m_PPG;
            }
            return nTotal / numBeatsOfFuture;
        }
        else
        {
            System.out.println("#2 Error in " + getCurrentMethodName());
            System.out.println("peak count = " + pv.peaks.size() + "   valley count = " + pv.valleys.size());
            return -1.0;
        }
    }

    // return the index of the peak nBeats ahead of the lastSampleIndex
    // where the last valley falls before a peak which falls before lastSampleIndex
    public int GetIndexOfNthPriorPeak(int nBeats, int lastSampleIndex, PeaksAndValleys pv)
    {
        int tmpIndex = lastSampleIndex;

        // it turns out that if the last item is either a peak or valley, you still need to go back 1 more peak than
        // the caller wants in order to find the correct peak
        for (int i = 0; i < nBeats + 1; i++)
        {
            tmpIndex = GetPriorItem(tmpIndex, RealtimePeakValleyDetect.eSlopeZero.PEAK, pv);
            if (tmpIndex == -1)
            {
                System.out.println("Error in " + getCurrentMethodName());
                break;
            }
        }
        return tmpIndex;
    }

    public static String getCurrentMethodName()
    {
        return Thread.currentThread().getStackTrace()[2].getClassName() + "." + Thread.currentThread().getStackTrace()[2].getMethodName();
    }
}
