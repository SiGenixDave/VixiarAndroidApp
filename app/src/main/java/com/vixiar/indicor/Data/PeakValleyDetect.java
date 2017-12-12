
package com.vixiar.indicor.Data;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeakValleyDetect {

    // //////////////////////////////////////////////////////////////////////////
    // / Constructors
    // //////////////////////////////////////////////////////////////////////////
    public static PeakValleyDetect getInstance() {
        return ourInstance;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Enumerations
    // //////////////////////////////////////////////////////////////////////////
    public enum ePVDStates {
        DETECTING_PEAK, DETECTING_VALLEY
    };

    public enum eSlopeZero {
        PEAK, VALLEY
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Attributes
    // //////////////////////////////////////////////////////////////////////////
    // Used to maintain a single instance of the PeakValleyDetect class
    private static final PeakValleyDetect ourInstance = new PeakValleyDetect();

    // Stores the indexes in "localData" where the peaks where detected
    private List<Integer> m_PeaksIndexes = new ArrayList<>();

    // Stores the indexes in "localData" where the valleys where detected
    private List<Integer> m_ValleysIndexes = new ArrayList<>();

    // External code pumps data into the list via AddToDataArray(); Execute()
    // pulls this data out and places into a local data list for processing
    // after which the data in this list is removed. Separating data and
    // local data allows multi-threading if desired to pump data into and
    // process the data concurrently
    private ConcurrentLinkedQueue<Integer> m_Data = new ConcurrentLinkedQueue<>();

    // Stores the entire copy of data that was pumped into the data list
    private List<Integer> m_LocalData = new ArrayList<>();

    // Amount of change from the max peak detected in order to begin "looking"
    // for the next valley. This value determines the max amount of noise
    // allowed on the high part of the wave
    private int m_DeltaPeak;

    // Amount of change from the min valley detected in order to begin "looking"
    // for the next peak. This value determines the max amount of noise allowed
    // on the low part of the wave
    private int m_DeltaValley;

    // Determines whether to begin looking for a peak or valley at the beginning
    // of the
    // processing cycle
    private ePVDStates m_DetectFirst;

    // Determines whether a peak or valley is being detected
    private ePVDStates m_State;

    // Current highest value while detecting a peak
    private int m_Peak;

    // Current lowest value while detecting a valley
    private int m_Valley;

    // Most recent index in localData where the detected peak is located
    private int m_PeakIndex;

    // Most recent index in localData where the detected valley is located
    private int m_ValleyIndex;

    // Used to determine how much data to process during an execution cycle.
    // Compared against the size of the local data list
    private int m_LocalDataIndex;

    // //////////////////////////////////////////////////////////////////////////
    // / Setters
    // //////////////////////////////////////////////////////////////////////////
    public void setDeltaPeak(int deltaPeak) {
        this.m_DeltaPeak = deltaPeak;
    }

    public void setDeltaValley(int deltaValley) {
        this.m_DeltaPeak = deltaValley;
    }

    public void setDetectFirst(ePVDStates detectFirst) {
        this.m_DetectFirst = detectFirst;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Getters
    // //////////////////////////////////////////////////////////////////////////
    public double getDeltaPeak() {
        return m_DeltaPeak;
    }

    public double getDeltaValley() {
        return m_DeltaValley;
    }

    public ePVDStates getDetectFirst() {
        return m_DetectFirst;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Public Methods
    // //////////////////////////////////////////////////////////////////////////
    public void Initialize(int deltaPeak, int deltaValley, Boolean detectPeakFirst) {
        this.m_DeltaPeak = deltaPeak;
        this.m_DeltaValley = deltaValley;
        // Default value of
        this.m_DetectFirst = ePVDStates.DETECTING_PEAK;
        if (!detectPeakFirst) {
            this.m_DetectFirst = ePVDStates.DETECTING_VALLEY;
        }

    }

    // returns the amount of data in the list currently being analyzed
    public int AmountOfData() {
        return m_LocalData.size();
    }

    // returns the ADC value at the provided offset, -1 if boundary exceeded
    public int GetData(int offset) {
        if (offset >= AmountOfData()) {
            return -1;
        }
        return m_LocalData.get(offset);
    }

    // returns the current number of peaks detected
    public int NumberOfPeaks() {
        return m_PeaksIndexes.size();
    }

    // Searches either the peak or valley array and returns the index of the peak or valley prior to
    // the dataIndex
    public int GetPriorDetect(int dataIndex, eSlopeZero type) {

        List<Integer> transitionList;

        if (type == eSlopeZero.PEAK) {
            transitionList = m_PeaksIndexes;
        } else if (type == eSlopeZero.VALLEY) {
            transitionList = m_ValleysIndexes;
        } else {
            return -1;
        }

        // start the scanning the desired list and find the data index where the previous
        // peak or valley was found. If there are transitions in the list, MAX_VALUE will be
        // returned
        int dataTransitionIndex = Integer.MIN_VALUE;
        int transitionIndex = transitionList.size() - 1;


        while (transitionIndex >= 0) {
            dataTransitionIndex = transitionList.get(transitionIndex);
            // peak or valley "just to the left" of the dsiredIndex was found, break out of here
            if (dataTransitionIndex < dataIndex) {
                break;
            }
            transitionIndex--;
        }

        // The beginning of the array was reached without finding a transition, inform the
        // calling function
        if (transitionIndex == -1) {
            dataTransitionIndex = Integer.MAX_VALUE;
        }

        return dataTransitionIndex;
    }

    // returns the current number of peaks detected between indices
    public List<Integer> GetIndexesBetween(int startIndex, int endIndex, eSlopeZero type) {

        List<Integer> transitionIndexList = new ArrayList<>();
        List<Integer> transitionList;

        int numTransitions;
        if (type == eSlopeZero.PEAK) {
            transitionList = m_PeaksIndexes;
        } else if (type == eSlopeZero.VALLEY) {
            transitionList = m_ValleysIndexes;
        } else {
            return null;
        }

        numTransitions = transitionList.size();

        if (numTransitions != 0) {
            // when endIndex = -1, the caller wants to find peaks all the way to
            // the end of the list
            if (endIndex == -1) {
                endIndex = transitionList.get(numTransitions - 1);
            }

            for (int index = 0; index < numTransitions; index++) {
                int currentIndex = transitionList.get(index);
                if ((currentIndex >= startIndex) && (currentIndex <= endIndex)) {
                    transitionIndexList.add(currentIndex);
                }
            }
        }

        return transitionIndexList;
    }

    // returns the current number of valleys detected
    public int NumberOfValleys() {
        return m_ValleysIndexes.size();
    }

    // returns the index into localData where a peak was detected
    public int GetPeakIndex(int index) {
        if (index >= NumberOfPeaks()) {
            return -1;
        }
        return m_PeaksIndexes.get(index);
    }

    // returns the index into localData where a valley was detected
    public int GetValleyIndex(int index) {
        if (index >= NumberOfValleys()) {
            return -1;
        }
        return m_ValleysIndexes.get(index);
    }

    // Used to add data to the list that is to be copied locally and then
    // processed
    public void AddToDataArray(int data) {
        this.m_Data.add(data);
    }

    // Used to reset all parameters so the algorithm can start fresh.
    public void ResetAlgorithm() {
        m_Peak = Integer.MIN_VALUE;
        m_Valley = Integer.MAX_VALUE;
        m_ValleyIndex = 0;
        m_PeakIndex = 0;
        m_LocalDataIndex = 0;

        m_State = m_DetectFirst;

        m_PeaksIndexes.clear();
        m_ValleysIndexes.clear();
        m_Data.clear();
        m_LocalData.clear();
    }

    // Called to copy all data from the list and then start processing it
    // locally
    public void Execute() {

        // Get all of the real time data and populate locally
        while (m_Data.size() != 0) {
            m_LocalData.add(m_Data.poll());
            // remove each data item as its copied to the local list
            m_Data.remove(0);
        }

        while (m_LocalDataIndex < m_LocalData.size()) {

            // System.out.println(m_LocalDataIndex);
            // System.out.println(m_LocalData.size());

            int currData = m_LocalData.get(m_LocalDataIndex);

            // Always keep running track of the max and min values no matter
            // whether or not
            // searching for a peak or valley
            if (currData > m_Peak) {
                // Store the peak index and data
                m_PeakIndex = m_LocalDataIndex;
                m_Peak = currData;
            }
            if (currData < m_Valley) {
                // Store the valley index and data
                m_ValleyIndex = m_LocalDataIndex;
                m_Valley = currData;
            }

            switch (m_State) {
                default:
                    break;

                case DETECTING_PEAK:
                    if (currData < (m_Peak - m_DeltaPeak)) {
                        // getting here indicates that the peak has been detected
                        // and the data is declining to the point where the value
                        // is below the peak noise (hysteresis) threshold.

                        // Save the index where the most recent peak was detected
                        m_PeaksIndexes.add(m_PeakIndex);
                        m_State = ePVDStates.DETECTING_VALLEY;

                        // Reset the data index 1 increment behind the peak
                        m_LocalDataIndex = m_PeakIndex - 1;

                        // store the current peak data in the valley in
                        // preparation for detecting a valley
                        m_Valley = m_LocalData.get(m_PeakIndex);
                        m_ValleyIndex = m_PeakIndex;
                    }
                    break;

                case DETECTING_VALLEY:
                    if (currData > (m_Valley + m_DeltaValley)) {
                        // getting here indicates that the valley has been detected
                        // and the data is AScending in value to the point where the
                        // value
                        // is above the valley noise (hysteresis) threshold.

                        // Save the index where the most recent valley was detected
                        m_ValleysIndexes.add(m_ValleyIndex);
                        m_State = ePVDStates.DETECTING_PEAK;

                        // Reset the data index 1 increment behind the valley
                        m_LocalDataIndex = m_ValleyIndex - 1;

                        // store the current valley data in the peak in
                        // preparation for detecting a valley
                        m_Peak = m_LocalData.get(m_ValleyIndex);
                        m_PeakIndex = m_ValleyIndex;
                    }
                    break;

            }

            m_LocalDataIndex++;
        }

    }

}
