package com.vixiar.indicor2.Data;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeakValleyDetect
{
    // //////////////////////////////////////////////////////////////////////////
    // / Tuning constants
    // //////////////////////////////////////////////////////////////////////////
    private static final double P_TO_P_FILTER_CONST_OLD_VALUE = 0.8;
    private static final double LEVEL_TO_DROP_FOR_PEAK_VALID = 0.2;
    private static final double LEVEL_TO_INCREASE_FOR_VALLEY_VALID = 0.2;
    private static final int DEFAULT_SAMPLES_NO_PEAK_LIMIT = 120;
    private static final int LIMIT_FOR_NO_PEAKS = 3;
    public static final int CYCLES_BEFORE_RESETTING_HYSTERESIS = 2;
    public static final double WAIT_TIME_AS_FACTOR_OF_PREVIOUS_PV_TIME = 0.5;

    // //////////////////////////////////////////////////////////////////////////
    // / Constructors
    // //////////////////////////////////////////////////////////////////////////
    public static PeakValleyDetect getInstance()
    {
        return ourInstance;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Enumerations
    // //////////////////////////////////////////////////////////////////////////
    public enum ePVDStates
    {
        VALIDATING_PEAK, VALIDATING_VALLEY, WAIT_BEFORE_LOOKING_FOR_VALLEY
    }

    public enum eSlopeZero
    {
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
    private int m_PeakHysteresisLevel;

    // Amount of change from the min valley detected in order to begin "looking"
    // for the next peak. This value determines the max amount of noise allowed
    // on the low part of the wave
    private int m_ValleyHysteresisLevel;

    // Keep a copy of the original delta peak value in case the hysteresis
    // algorithm needs to be reset
    private int m_defaultDeltaPeak;
    private int m_defaultDeltaValley;

    // Determines whether to begin looking for a peak or valley at the beginning
    // of the
    // processing cycle
    private ePVDStates m_DetectFirst;

    // Determines whether a peak or valley is being detected
    private ePVDStates m_State;

    // Current highest value while detecting a peak
    private int m_CurrentHighestSample;

    // Current lowest value while detecting a valley
    private int m_CurrentLowestSample;

    // Most recent index in localData where the detected peak is located
    private int m_LastPotentialPeakIndex;

    // Most recent index in localData where the detected valley is located
    private int m_LastPotentialValleyIndex;

    // Used to determine how much data to process during an execution cycle.
    // Compared against the size of the local data list
    private int m_DataIndex;

    private int m_HysteresisAdjustPeakCount;
    private long m_highestPeak;
    private long m_lowestValley;
    private int m_samplesWithNoPeaks;

    private int m_MaxSamplesWithNoPeaksBeforeReset;

    private int m_lastPeakIndex = 0;
    private int m_lastValleyIndex = 0;
    private int m_lastPVCount = 0;

    private int m_lastMaxPeakToPeak = 0;


    // if this many samples go by and there are no peaks detected,
    // reset the hysteresis delta numbers
    // this is based on the lowest heart rate of 50BPM
    // so, at 50BPM there should be about 42 samples so if we don't see
    // a peak in 100 samples, to be safe, reset the hysteresis
    private final int SAMPLE_COUNT_TO_RESET_HYSTERESIS = 100;

    // //////////////////////////////////////////////////////////////////////////
    // / Setters
    // //////////////////////////////////////////////////////////////////////////
    public void setDeltaPeak(int deltaPeak)
    {
        this.m_PeakHysteresisLevel = deltaPeak;
    }

    public void setDeltaValley(int deltaValley)
    {
        this.m_PeakHysteresisLevel = deltaValley;
    }

    public void setDetectFirst(ePVDStates detectFirst)
    {
        this.m_DetectFirst = detectFirst;
    }

    public List<Integer> getPeaksIndexes()
    {
        return m_PeaksIndexes;
    }

    public List<Integer> getValleysIndexes()
    {
        return m_ValleysIndexes;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Getters
    // //////////////////////////////////////////////////////////////////////////
    public double getDeltaPeak()
    {
        return m_PeakHysteresisLevel;
    }

    public double getDeltaValley()
    {
        return m_ValleyHysteresisLevel;
    }

    public ePVDStates getDetectFirst()
    {
        return m_DetectFirst;
    }

    // //////////////////////////////////////////////////////////////////////////
    // / Public Methods
    // //////////////////////////////////////////////////////////////////////////
    public void Initialize(int deltaPeak, int deltaValley, Boolean detectPeakFirst)
    {
        this.m_PeakHysteresisLevel = deltaPeak;
        this.m_ValleyHysteresisLevel = deltaValley;
        this.m_defaultDeltaPeak = deltaPeak;
        this.m_defaultDeltaValley = deltaValley;
        this.m_samplesWithNoPeaks = 0;

        // Default value of
        this.m_DetectFirst = ePVDStates.VALIDATING_PEAK;
        if (!detectPeakFirst)
        {
            this.m_DetectFirst = ePVDStates.VALIDATING_VALLEY;
        }

    }

    // returns the amount of data in the list currently being analyzed
    public int AmountOfData()
    {
        return m_LocalData.size();
    }

    // returns the ADC value at the provided offset, -1 if boundary exceeded
    public int GetData(int offset)
    {
        if (offset >= AmountOfData())
        {
            return -1;
        }
        return m_LocalData.get(offset);
    }

    // returns the current number of peaks detected
    public int NumberOfPeaks()
    {
        return m_PeaksIndexes.size();
    }

    // Searches either the peak or valley array and returns the index of the peak or valley prior to
    // the dataIndex
    public int GetPriorDetect(int dataIndex, eSlopeZero type)
    {

        List<Integer> transitionList;

        if (type == eSlopeZero.PEAK)
        {
            transitionList = m_PeaksIndexes;
        }
        else if (type == eSlopeZero.VALLEY)
        {
            transitionList = m_ValleysIndexes;
        }
        else
        {
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
            // peak or valley "just to the left" of the dsiredIndex was found, break out of here
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
            dataTransitionIndex = Integer.MAX_VALUE;
        }

        return dataTransitionIndex;
    }

    // returns the current number of peaks detected between indices
    public List<Integer> GetIndexesBetween(int startIndex, int endIndex, eSlopeZero type)
    {

        List<Integer> transitionIndexList = new ArrayList<>();
        List<Integer> transitionList;

        int numTransitions;
        if (type == eSlopeZero.PEAK)
        {
            transitionList = m_PeaksIndexes;
        }
        else if (type == eSlopeZero.VALLEY)
        {
            transitionList = m_ValleysIndexes;
        }
        else
        {
            return null;
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
    public int NumberOfValleys()
    {
        return m_ValleysIndexes.size();
    }

    // returns the index into localData where a peak was detected
    public int GetPeakIndex(int index)
    {
        if (index >= NumberOfPeaks())
        {
            return -1;
        }
        return m_PeaksIndexes.get(index);
    }

    // returns the index into localData where a valley was detected
    public int GetValleyIndex(int index)
    {
        if (index >= NumberOfValleys())
        {
            return -1;
        }
        return m_ValleysIndexes.get(index);
    }

    // Used to add data to the list that is to be copied locally and then
    // processed
    public void AddToDataArray(int data)
    {
        this.m_Data.add(data);
    }

    // Used to reset all parameters so the algorithm can start fresh.
    public void ResetAlgorithm()
    {
        m_CurrentHighestSample = Integer.MIN_VALUE;
        m_CurrentLowestSample = Integer.MAX_VALUE;
        m_LastPotentialValleyIndex = 0;
        m_LastPotentialPeakIndex = 0;
        m_DataIndex = 0;

        m_State = m_DetectFirst;

        m_PeaksIndexes.clear();
        m_ValleysIndexes.clear();
        m_Data.clear();
        m_LocalData.clear();

        m_HysteresisAdjustPeakCount = 0;
        m_highestPeak = 0;
        m_lowestValley = 65535;
    }

    // Called to copy all data from the list and then start processing it
    // locally
    public void Execute()
    {

        // Get all of the real time data and populate locally
        while (m_Data.size() != 0)
        {
            m_LocalData.add(m_Data.poll());
            // remove each data item as its copied to the local list
            m_Data.remove(0);
        }

        while (m_DataIndex < m_LocalData.size())
        {

            // System.out.println(m_LocalData.size());

            int currentSample = m_LocalData.get(m_DataIndex);
            //System.out.println(m_DataIndex + ", " + currentSample);

            // Always keep running track of the max and min values no matter
            // whether or not
            // searching for a peak or valley
            if (currentSample > m_CurrentHighestSample)
            {
                // Store the peak index and data
                m_LastPotentialPeakIndex = m_DataIndex;
                m_CurrentHighestSample = currentSample;
                //System.out.println("New high - " + m_CurrentHighestSample + " @ - " + (m_DataIndex * 0.02));
            }
            if (currentSample < m_CurrentLowestSample)
            {
                if (m_State != ePVDStates.WAIT_BEFORE_LOOKING_FOR_VALLEY)
                {
                    // Store the valley index and data
                    m_LastPotentialValleyIndex = m_DataIndex;
                    m_CurrentLowestSample = currentSample;
                    //System.out.println("New low - " + m_CurrentLowestSample + " @ - " + (m_DataIndex * 0.02));
                }
            }

            switch (m_State)
            {
                default:
                    break;

                case VALIDATING_PEAK:
                    if (currentSample < (m_CurrentHighestSample - m_PeakHysteresisLevel))
                    {
                        //System.out.println(" P - " + currentSample + " " + m_CurrentHighestSample + " " + m_PeakHysteresisLevel);
                        // getting here indicates that the peak has been detected
                        // and the data is declining to the point where the value
                        // is below the peak noise (hysteresis) threshold.
                        m_lastPeakIndex = m_LastPotentialPeakIndex;


                        //HeartRateFromPeaks(m_LastPotentialPeakIndex);
                        // Save the index where the most recent peak was detected
                        if (m_PeaksIndexes.size() > 0)
                        {
                            // make sure this one is different than the last one
                            if (m_LastPotentialPeakIndex != m_PeaksIndexes.get(m_PeaksIndexes.size() - 1))
                            {
                                m_PeaksIndexes.add(m_LastPotentialPeakIndex);
                            }
                        }
                        else
                        {
                            m_PeaksIndexes.add(m_LastPotentialPeakIndex);
                        }
                        m_State = ePVDStates.WAIT_BEFORE_LOOKING_FOR_VALLEY;

                        // Reset the data index 1 increment behind the peak
                        m_DataIndex = m_LastPotentialPeakIndex - 1;

                        // store the current peak data in the valley in
                        // preparation for detecting a valley
                        m_CurrentLowestSample = m_LocalData.get(m_LastPotentialPeakIndex);
                        m_LastPotentialValleyIndex = m_LastPotentialPeakIndex;

                        // reset our peak detection counter
                        m_samplesWithNoPeaks = 0;

                        if (m_CurrentHighestSample > m_highestPeak)
                        {
                            m_highestPeak = m_CurrentHighestSample;
                            //System.out.println("Max at " + m_CurrentHighestSample);
                        }
                        if (m_HysteresisAdjustPeakCount++ > CYCLES_BEFORE_RESETTING_HYSTERESIS)
                        {
                            AdjustHysteresis();
                            m_HysteresisAdjustPeakCount = 0;
                            m_highestPeak = 0;
                            m_lowestValley = 65535;
                        }
                        //System.out.println();
                        //System.out.println("FOUND PEAK Starting WAITING  T = " + (lastPeakIndex * 0.02));
                        //System.out.println("LS, HS, CS, HY " + m_CurrentLowestSample + "," + m_CurrentHighestSample + "," + currentSample + ", " + m_PeakHysteresisLevel);
                    }
                    else
                    {
                        m_samplesWithNoPeaks++;
                        //System.out.println(currentSample + " --- " + m_samplesWithNoPeaks);
                    }
                    break;

                case WAIT_BEFORE_LOOKING_FOR_VALLEY:
                    // wait enough time to miss any extra peak-valleys in the real signal
                    if (m_DataIndex > (m_LastPotentialPeakIndex + (m_lastPVCount * WAIT_TIME_AS_FACTOR_OF_PREVIOUS_PV_TIME)))
                    {
                        //System.out.println();
                        //System.out.println("WAIT is over LOOKING FOR VALLEY  T = " + (m_DataIndex * 0.02));
                        //System.out.println("LS, HS, CS " + m_CurrentLowestSample + "," + m_CurrentHighestSample + "," + currentSample);
                        m_State = ePVDStates.VALIDATING_VALLEY;
                    }
                    else
                    {
                        m_samplesWithNoPeaks++;
                        //System.out.println(currentSample + " --- " + m_samplesWithNoPeaks);
                    }
                    break;

                case VALIDATING_VALLEY:
                    if (currentSample > (m_CurrentLowestSample + m_ValleyHysteresisLevel))
                    {
                        //System.out.println("V - " + currentSample + " " + m_CurrentLowestSample + " " + m_ValleyHysteresisLevel);
                        // getting here indicates that the valley has been detected
                        // and the data is Ascending in value to the point where the
                        // value
                        // is above the valley noise (hysteresis) threshold.

                        m_lastValleyIndex = m_LastPotentialValleyIndex;
                        m_lastPVCount = m_lastValleyIndex - m_lastPeakIndex;
                        //System.out.println("Last p-v time = " + lastPVCount + " Time = " + (m_LastPotentialValleyIndex * 0.02));

                        //HeartRateFromValleys(m_LastPotentialValleyIndex);
                        // Save the index where the most recent valley was detected
                        if (m_ValleysIndexes.size() > 0)
                        {
                            // make sure this one is different than the last one
                            if (m_LastPotentialValleyIndex != m_ValleysIndexes.get(m_ValleysIndexes.size() - 1))
                            {
                                // also make sure this valley is different than the last peak
                                if (m_LastPotentialValleyIndex != m_PeaksIndexes.get(m_PeaksIndexes.size() - 1))
                                {
                                    m_ValleysIndexes.add(m_LastPotentialValleyIndex);
                                }
                            }
                        }
                        else
                        {
                            m_ValleysIndexes.add(m_LastPotentialValleyIndex);
                        }
                        m_State = ePVDStates.VALIDATING_PEAK;

                        // Reset the data index 1 increment behind the valley
                        m_DataIndex = m_LastPotentialValleyIndex - 1;

                        // store the current valley data in the peak in
                        // preparation for detecting a valley
                        m_CurrentHighestSample = m_LocalData.get(m_LastPotentialValleyIndex);
                        m_LastPotentialPeakIndex = m_LastPotentialValleyIndex;

                        if (m_CurrentLowestSample < m_lowestValley)
                        {
                            m_lowestValley = m_CurrentLowestSample;
                            //System.out.println("Min at " + m_CurrentLowestSample);
                        }
                        //System.out.println();
                        //System.out.println("FOUND VALLEY LOOKING FOR PEAK   T = " + (lastValleyIndex * 0.02));
                        //System.out.println("LS, HS, CS, HY " + m_CurrentLowestSample + "," + m_CurrentHighestSample + "," + currentSample + ", " + m_ValleyHysteresisLevel);
                    }
                    else
                    {
                        m_samplesWithNoPeaks++;
                        //System.out.println(currentSample + " --- " + m_samplesWithNoPeaks);
                    }
                    break;

            }
            m_DataIndex++;

            // figure out how many samples we can allow without peaks before resetting everything
            if (m_lastPVCount > 0)
            {
                m_MaxSamplesWithNoPeaksBeforeReset = m_lastPVCount * LIMIT_FOR_NO_PEAKS;
            }
            else
            {
                m_MaxSamplesWithNoPeaksBeforeReset = DEFAULT_SAMPLES_NO_PEAK_LIMIT;
            }
            if (m_samplesWithNoPeaks >= m_MaxSamplesWithNoPeaksBeforeReset)
            {
                // if too much time has elapsed without seeing a peak, reset the hysteresis and the detection state
                m_PeakHysteresisLevel = m_defaultDeltaPeak;
                m_ValleyHysteresisLevel = m_defaultDeltaValley;
                m_State = ePVDStates.VALIDATING_PEAK;
                m_lastPVCount = 0;
                m_samplesWithNoPeaks = 0;
                //System.out.println();
                //System.out.println("NO PEAKS - " + m_samplesWithNoPeaks + " threshold = " + m_MaxSamplesWithNoPeaksBeforeReset);
                //System.out.println("TIMEOUT - Switching to VALIDATING_PEAK  T = " + (m_DataIndex * 0.02));
                //System.out.println("LS, HS, CS " + m_CurrentLowestSample + "," + m_CurrentHighestSample + "," + currentSample);
            }
        }
    }

    private void AdjustHysteresis()
    {
        int maxPeakToPeak = (int) (m_highestPeak - m_lowestValley);
        int filteredPToP = (int) ((maxPeakToPeak * (1.0 - P_TO_P_FILTER_CONST_OLD_VALUE)) + (m_lastMaxPeakToPeak * P_TO_P_FILTER_CONST_OLD_VALUE));
        m_lastMaxPeakToPeak = maxPeakToPeak;
        m_PeakHysteresisLevel = (int) (filteredPToP * LEVEL_TO_DROP_FOR_PEAK_VALID);
        m_ValleyHysteresisLevel = (int) (filteredPToP * LEVEL_TO_INCREASE_FOR_VALLEY_VALID);

        //System.out.println("Max p-p = " + maxPeakToPeak);
        //System.out.println("Peak Hyseteresis = " + m_PeakHysteresisLevel + "  Valley Hyseteresis = " + m_ValleyHysteresisLevel);
    }
}
