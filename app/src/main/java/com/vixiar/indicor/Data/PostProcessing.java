package com.vixiar.indicor.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.vixiar.indicor.Data.AppConstants.BASELINE_END_BEFORE_VSTART_SEC;
import static com.vixiar.indicor.Data.AppConstants.BASELINE_LENGTH_SEC;
import static com.vixiar.indicor.Data.AppConstants.SAMPLES_PER_SECOND;


public class PostProcessing
{
    private static final PostProcessing ourInstance = new PostProcessing();

    private final double desktopToolSoftwareVersion = 2.1;

    private final int NUM_TESTS = 3;
    private String filename;
    private double[] m_aEndPAR_PV_BL = new double[NUM_TESTS];
    private double[] m_aEndPA_Peak = new double[NUM_TESTS];
    private double[] m_aPA_Avg_BL = new double[NUM_TESTS];
    private double[] m_aMinPA_Peak = new double[NUM_TESTS];
    private double[] m_aMinPAR_PV_BL = new double[NUM_TESTS];
    private double[] m_aEndPA_Avg3 = new double[NUM_TESTS];
    private double[] m_aEndPAR_Avg3_BL = new double[NUM_TESTS];
    private double[] m_aMinPAR_Avg3_BL = new double[NUM_TESTS];
    private double[] m_aMinPA_Avg3 = new double[NUM_TESTS];
    private double[] m_aRMSEnd = new double[NUM_TESTS];
    private double[] m_aRMSMin = new double[NUM_TESTS];
    private double[] m_aRMSBL = new double[NUM_TESTS];
    private double[] m_aRMSEndPAR_BL = new double[NUM_TESTS];
    private double[] m_aRMSMinPAR_BL = new double[NUM_TESTS];
    private double[] m_aPh1PA_Peak = new double[NUM_TESTS];
    private double[] m_aMinPAR_PV_VM = new double[NUM_TESTS];
    private double[] m_aEndPAR_PV_VM = new double[NUM_TESTS];
    private double[] m_aPh1PA_Avg3 = new double[NUM_TESTS];
    private double[] m_aRMSMinPAR_PV = new double[NUM_TESTS];
    private double[] m_aRMSEndPAR_PV = new double[NUM_TESTS];
    private double[] m_aRMSPh1PAR_PV = new double[NUM_TESTS];
    private double[] m_aRMSBLPAR_PV = new double[NUM_TESTS];
    private double[] m_aRMS_Ph1 = new double[NUM_TESTS];
    private double[] m_aRMS_Ph4 = new double[NUM_TESTS];
    private double[] m_aEndPAR_Avg3_VM = new double[NUM_TESTS];
    private double[] m_aMinPAR_Avg3_VM = new double[NUM_TESTS];
    private double[] m_aRMSEndPAR_VM = new double[NUM_TESTS];
    private double[] m_aRMSMinPAR_VM = new double[NUM_TESTS];
    private double[] m_aBLPAR_PV = new double[NUM_TESTS];
    private double[] m_aPh1PAR_PV = new double[NUM_TESTS];
    private double[] m_aEndPAR_PV = new double[NUM_TESTS];
    private double[] m_aMinPAR_PV = new double[NUM_TESTS];
    private double[] m_aBLPAR_Avg3_PV = new double[NUM_TESTS];
    private double[] m_aPh1PAR_Avg3_PV = new double[NUM_TESTS];
    private double[] m_aEndPAR_Avg3_PV = new double[NUM_TESTS];
    private double[] m_aMinPAR_Avg3_PV = new double[NUM_TESTS];
    private double[] m_aSlope_Ph2 = new double[NUM_TESTS];
    private double[] m_aPh4PA_Peak = new double[NUM_TESTS];
    private double[] m_aPh4PA_Avg3 = new double[NUM_TESTS];
    private double[] m_aTime_Ph2 = new double[NUM_TESTS];
    private double[] m_aPh1PeakPA_MinPA = new double[NUM_TESTS];
    private double[] m_aMeanVM_pressure = new double[NUM_TESTS];
    private double[] m_aMedianVM_pressure = new double[NUM_TESTS];
    private double[] m_aBLHR_Avg = new double[NUM_TESTS];
    private double[] m_aPh2HR_Avg = new double[NUM_TESTS];
    private double[] m_aPh2HR_min = new double[NUM_TESTS];
    private double[] m_aPh2PA_avg = new double[NUM_TESTS];

    public static PostProcessing getInstance()
    {
        return ourInstance;
    }

    public int FindVStart(int testNumber, ArrayList<RealtimeDataSample> dataSet)
    {
        TestMarkers tm;
        tm = PatientInfo.getInstance().GetTestMarkers(testNumber);
        int searchIndex = tm.startIndex;
        int startLimit = searchIndex - (10 * SAMPLES_PER_SECOND);  // 10 sec backward from T0--startIndex
        int vStart = -1;
        boolean vStartFound = false;

        // go backwards in time until the pressure goes below 2, then find when it goes over 2
        while (!vStartFound && (searchIndex > startLimit))
        {
            if (dataSet.get(searchIndex).m_pressure <= 2.0)
            {
                while (dataSet.get(searchIndex).m_pressure <= 2.0)
                {
                    searchIndex++;
                }
                vStart = searchIndex;
                vStartFound = true;
            }
            else
            {
                searchIndex--;
            }
        }
        return vStart;
    }

    public int FindVEnd(int testNumber, ArrayList<RealtimeDataSample> dataSet)
    {
        TestMarkers tm;
        tm = PatientInfo.getInstance().GetTestMarkers(testNumber);
        int endIndex = tm.endIndex;
        int endLimit = endIndex + (10 * 50);  // 10 sec forward from T10--endIndex
        double slope = 0.0;

        for (; endIndex != endLimit; endIndex++)
        {   // repeat loop until 10 sec forward from endIndex (T10)
            if (dataSet.get(endIndex).m_pressure < 2.0)
            {
                return endIndex;
            }
        }
        return -1;
    }

    public int FindBaselineStart(int testNumber, ArrayList<RealtimeDataSample> dataSet)
    {
        return (FindVStart(testNumber, dataSet) - (SAMPLES_PER_SECOND * (BASELINE_END_BEFORE_VSTART_SEC + BASELINE_LENGTH_SEC)));
    }

    public int FindBaselineEnd(int testNumber, ArrayList<RealtimeDataSample> dataSet)
    {
        return (FindBaselineStart(testNumber, dataSet) + (SAMPLES_PER_SECOND * BASELINE_LENGTH_SEC));
    }

    public int FindT0FromVStart(int vStart, ArrayList<RealtimeDataSample> dataSet)
    {
        // move from Vstart until pressure crosses 16
        int index = vStart;
        while (dataSet.get(index).m_pressure < 16.0)
        {
            index++;
        }
        return index;
    }

    private double CalculateMeanPressure(int startIndex, int endIndex, ArrayList<RealtimeDataSample> dataSet)
    {
        double mean = 0.0;
        for (int i = startIndex; i < endIndex; i++)
        {
            mean += dataSet.get(i).m_pressure;
        }
        mean /= ((endIndex - startIndex));
        return mean;
    }

    private double CalculateMedianPressure(int startIndex, int endIndex, ArrayList<RealtimeDataSample> dataSet)
    {
        List<Double> data = new ArrayList<>();

        for (int i = startIndex; i < endIndex; i++)
        {
            data.add(dataSet.get(i).m_pressure);
        }

        Collections.sort(data);

        double median = data.get(data.size() / 2);
        if (data.size() % 2 == 0)
        {
            median = (data.get(data.size() / 2) + data.get(data.size() / 2 - 1)) / 2;
        }

        return median;
    }

    private void PrintResultToConsole(String label, int testNumber, double value)
    {
        //System.out.println(label + " " + "[" + (testNumber + 1) + "]" + ": " + value);
    }

    public void ClearAllCalculatedData()
    {
        Arrays.fill(m_aEndPAR_PV_BL, 0.0);
        Arrays.fill(m_aEndPA_Peak, 0.0);
        Arrays.fill(m_aPA_Avg_BL, 0.0);
        Arrays.fill(m_aMinPA_Peak, 0.0);
        Arrays.fill(m_aMinPAR_PV_BL, 0.0);
        Arrays.fill(m_aEndPA_Avg3, 0.0);
        Arrays.fill(m_aEndPAR_Avg3_BL, 0.0);
        Arrays.fill(m_aMinPAR_Avg3_BL, 0.0);
        Arrays.fill(m_aMinPA_Avg3, 0.0);
        Arrays.fill(m_aRMSEnd, 0.0);
        Arrays.fill(m_aRMSMin, 0.0);
        Arrays.fill(m_aRMSBL, 0.0);
        Arrays.fill(m_aRMSEndPAR_BL, 0.0);
        Arrays.fill(m_aRMSMinPAR_BL, 0.0);
        Arrays.fill(m_aPh1PA_Peak, 0.0);
        Arrays.fill(m_aMinPAR_PV_VM, 0.0);
        Arrays.fill(m_aEndPAR_PV_VM, 0.0);
        Arrays.fill(m_aPh1PA_Avg3, 0.0);
        Arrays.fill(m_aRMSMinPAR_PV, 0.0);
        Arrays.fill(m_aRMSEndPAR_PV, 0.0);
        Arrays.fill(m_aRMSPh1PAR_PV, 0.0);
        Arrays.fill(m_aRMSBLPAR_PV, 0.0);
        Arrays.fill(m_aRMS_Ph1, 0.0);
        Arrays.fill(m_aRMS_Ph4, 0.0);
        Arrays.fill(m_aEndPAR_Avg3_VM, 0.0);
        Arrays.fill(m_aMinPAR_Avg3_VM, 0.0);
        Arrays.fill(m_aRMSEndPAR_VM, 0.0);
        Arrays.fill(m_aRMSMinPAR_VM, 0.0);
        Arrays.fill(m_aBLPAR_PV, 0.0);
        Arrays.fill(m_aPh1PAR_PV, 0.0);
        Arrays.fill(m_aEndPAR_PV, 0.0);
        Arrays.fill(m_aMinPAR_PV, 0.0);
        Arrays.fill(m_aBLPAR_Avg3_PV, 0.0);
        Arrays.fill(m_aPh1PAR_Avg3_PV, 0.0);
        Arrays.fill(m_aEndPAR_Avg3_PV, 0.0);
        Arrays.fill(m_aMinPAR_Avg3_PV, 0.0);
        Arrays.fill(m_aSlope_Ph2, 0.0);
        Arrays.fill(m_aPh4PA_Peak, 0.0);
        Arrays.fill(m_aPh4PA_Avg3, 0.0);
        Arrays.fill(m_aTime_Ph2, 0.0);
        Arrays.fill(m_aPh1PeakPA_MinPA, 0.0);
        Arrays.fill(m_aMeanVM_pressure, 0.0);
        Arrays.fill(m_aMedianVM_pressure, 0.0);
        Arrays.fill(m_aBLHR_Avg, 0.0);
        Arrays.fill(m_aPh2HR_Avg, 0.0);
        Arrays.fill(m_aPh2HR_min, 0.0);
        Arrays.fill(m_aPh2PA_avg, 0.0);
    }

    // test number is 0 relative
    public boolean CalculatePostProcessingResults(int testNumber, PeaksAndValleys pv, ArrayList<RealtimeDataSample> dataSet)
    {
        TestMarkers tm;
        tm = PatientInfo.getInstance().GetTestMarkers(testNumber);
        int minPAPeakIndex, ph1PAPeakIndex, ph4PAPeakIndex;

        // make sure the test markers were found
        if (tm.endIndex != 0 && tm.startIndex != 0)
        {
            try
            {
                // placeholder for some functions return data
                ValueAndLocation vl;

                int vStartIndex = FindVStart(testNumber, dataSet);
                int vEndIndex = FindVEnd(testNumber, dataSet);
                PrintResultToConsole("VStart", testNumber, vStartIndex);
                PrintResultToConsole("VEnd", testNumber, vEndIndex);

                int t0Index = FindT0FromVStart(vStartIndex, dataSet);
                int t10Index = t0Index + (SAMPLES_PER_SECOND * 10);
                PrintResultToConsole("t0", testNumber, t0Index);
                PrintResultToConsole("t10", testNumber, t10Index);

                int baselineStartIndex = FindBaselineStart(testNumber, dataSet);
                int baselineEndIndex = baselineStartIndex + (SAMPLES_PER_SECOND * BASELINE_LENGTH_SEC);
                PrintResultToConsole("Baseline Start", testNumber, baselineStartIndex);
                PrintResultToConsole("Baseline End", testNumber, baselineEndIndex);

                // 1 DPR-3.3.34
                // End PA - peak
                vl = BeatProcessing.getInstance().GetEndPA(t10Index, pv, dataSet);
                m_aEndPA_Peak[testNumber] = vl.value;
                PrintResultToConsole("End PA - peak", testNumber, m_aEndPA_Peak[testNumber]);

                // 2 DPR-3.3.26
                // BL PA - avg
                m_aPA_Avg_BL[testNumber] = BeatProcessing.getInstance().GetAvgPAInRange(baselineStartIndex, baselineEndIndex, pv, dataSet);
                PrintResultToConsole("BL PA - avg", testNumber, m_aPA_Avg_BL[testNumber]);

                // 3 DPR-3.3.35
                // Min PA - peak
                vl = BeatProcessing.getInstance().GetMinPAInRange(t0Index, t10Index, pv, dataSet);
                m_aMinPA_Peak[testNumber] = vl.value;
                minPAPeakIndex = vl.location;
                PrintResultToConsole("Min PA - peak", testNumber, m_aMinPA_Peak[testNumber]);

                // 4 DPR-3.3.1
                // End PAR - peak values-BL
                m_aEndPAR_PV_BL[testNumber] = m_aEndPA_Peak[testNumber] / m_aPA_Avg_BL[testNumber];
                PrintResultToConsole("End PAR - peak values-BL", testNumber, m_aEndPAR_PV_BL[testNumber]);

                // 5 DPR-3.3.2
                // Min PAR - peak values-BL
                m_aMinPAR_PV_BL[testNumber] = m_aMinPA_Peak[testNumber] / m_aPA_Avg_BL[testNumber];
                PrintResultToConsole("Min PAR - peak values-BL", testNumber, m_aMinPAR_PV_BL[testNumber]);

                // 6 DPR-3.3.36
                // End PA - avg 3
                m_aEndPA_Avg3[testNumber] = BeatProcessing.getInstance().GetAvgPAHistorical(t10Index, 3, pv, dataSet);
                PrintResultToConsole("End PA - avg 3", testNumber, m_aEndPA_Avg3[testNumber]);

                // 7 DPR-3.3.3
                // End PAR - avg of 3 values-BL
                m_aEndPAR_Avg3_BL[testNumber] = m_aEndPA_Avg3[testNumber] / m_aPA_Avg_BL[testNumber];
                PrintResultToConsole("End PAR - avg of 3 values-BL", testNumber, m_aEndPAR_Avg3_BL[testNumber]);

                // 8 DPR-3.3.37
                // Min PA - avg 3
                m_aMinPA_Avg3[testNumber] = BeatProcessing.getInstance().GetMinPAAvg3(t0Index, t10Index, pv, dataSet);
                PrintResultToConsole("Min PA - avg3", testNumber, m_aMinPA_Avg3[testNumber]);

                // 9 DPR-3.3.4
                // Min PAR - avg of 3 values-BL
                m_aMinPAR_Avg3_BL[testNumber] = m_aMinPA_Avg3[testNumber] / m_aPA_Avg_BL[testNumber];
                PrintResultToConsole("Min PAR - avg of 3 values-BL", testNumber, m_aMinPAR_Avg3_BL[testNumber]);

                // 10 DPR-3.3.38
                // RMS end
                m_aRMSEnd[testNumber] = BeatProcessing.getInstance().GetRMSInRange(t10Index - (SAMPLES_PER_SECOND * 3), t10Index, dataSet);
                PrintResultToConsole("RMS end", testNumber, m_aRMSEnd[testNumber]);

                // 11 DPR-3.3.39
                // RMS min
                m_aRMSMin[testNumber] = BeatProcessing.getInstance().GetMinRMS(t0Index, t10Index, 3, dataSet);
                PrintResultToConsole("RMS min", testNumber, m_aRMSMin[testNumber]);

                // 12 DPR-3.3.27
                // RMS BL
                m_aRMSBL[testNumber] = BeatProcessing.getInstance().GetRMSInRange(vStartIndex - (SAMPLES_PER_SECOND * 15), vStartIndex - (SAMPLES_PER_SECOND * 5), dataSet);
                PrintResultToConsole("RMS BL", testNumber, m_aRMSBL[testNumber]);

                // 13 DPR-3.3.5
                // RMS end PAR-BL
                m_aRMSEndPAR_BL[testNumber] = m_aRMSEnd[testNumber] / m_aRMSBL[testNumber];
                PrintResultToConsole("RMS end PAR-BL", testNumber, m_aRMSEndPAR_BL[testNumber]);

                // 14 DPR-3.3.6
                // RMS min PAR-BL
                m_aRMSMinPAR_BL[testNumber] = m_aRMSMin[testNumber] / m_aRMSBL[testNumber];
                PrintResultToConsole("RMS min PAR-BL", testNumber, m_aRMSMinPAR_BL[testNumber]);

                // 15 DPR-3.3.28
                // Ph1 PA - peak
                vl = BeatProcessing.getInstance().GetMaxPAInRange(vStartIndex, vStartIndex + (SAMPLES_PER_SECOND * 3), pv, dataSet);
                m_aPh1PA_Peak[testNumber] = vl.value;
                ph1PAPeakIndex = vl.location;
                PrintResultToConsole("Ph1 PA-peak", testNumber, m_aPh1PA_Peak[testNumber]);

                // 16 DPR-3.3.7
                // End PAR - peak values-VM
                m_aEndPAR_PV_VM[testNumber] = m_aEndPA_Peak[testNumber] / m_aPh1PA_Peak[testNumber];
                PrintResultToConsole("End PAR - peak values-VM", testNumber, m_aEndPAR_PV_VM[testNumber]);

                // 17 DPR-3.3.8
                // Min PAR - peak values-VM
                m_aMinPAR_PV_VM[testNumber] = m_aMinPA_Peak[testNumber] / m_aPh1PA_Peak[testNumber];
                PrintResultToConsole("Min PAR - peak values-VM", testNumber, m_aMinPAR_PV_VM[testNumber]);

                // 18 DPR-3.3.30
                // RMS Ph1
                m_aRMS_Ph1[testNumber] = BeatProcessing.getInstance().GetRMSInRange(vStartIndex, vStartIndex + (SAMPLES_PER_SECOND * 3), dataSet);
                PrintResultToConsole("RMS Ph1", testNumber, m_aRMS_Ph1[testNumber]);

                // 19 DPR-3.3.33
                // RMS Ph4
                int ph4Index = vEndIndex + ((int) (SAMPLES_PER_SECOND * 2.5));
                m_aRMS_Ph4[testNumber] = BeatProcessing.getInstance().GetRMSInRange(ph4Index, ph4Index + (SAMPLES_PER_SECOND * 10), dataSet);
                PrintResultToConsole("RMS Ph4", testNumber, m_aRMS_Ph4[testNumber]);

                // 20 3.3.21
                // RMS BL PAR-PV
                m_aRMSBLPAR_PV[testNumber] = m_aRMSBL[testNumber] / m_aRMS_Ph4[testNumber];
                PrintResultToConsole("RMS BL PAR-PV", testNumber, m_aRMSBLPAR_PV[testNumber]);

                // 21 3.3.22
                // RMS Ph1 PAR-PV
                m_aRMSPh1PAR_PV[testNumber] = m_aRMS_Ph1[testNumber] / m_aRMS_Ph4[testNumber];
                PrintResultToConsole("RMS Ph1 PAR-PV", testNumber, m_aRMSPh1PAR_PV[testNumber]);

                // 22 3.3.23
                // RMS End PAR-PV
                m_aRMSEndPAR_PV[testNumber] = m_aRMSEnd[testNumber] / m_aRMS_Ph4[testNumber];
                PrintResultToConsole("RMS End PAR-PV", testNumber, m_aRMSEndPAR_PV[testNumber]);

                // 23 3.3.24
                // RMS Min PAR-PV
                m_aRMSMinPAR_PV[testNumber] = m_aRMSMin[testNumber] / m_aRMS_Ph4[testNumber];
                PrintResultToConsole("RMS Min PAR-PV", testNumber, m_aRMSMinPAR_PV[testNumber]);

                // 24 DPR-3.3.29
                // Ph1 PA - avg 3
                m_aPh1PA_Avg3[testNumber] = BeatProcessing.getInstance().GetPh1PAAvg3(ph1PAPeakIndex, vStartIndex, pv, dataSet);
                PrintResultToConsole("Ph1 PA - avg 3", testNumber, m_aPh1PA_Avg3[testNumber]);

                // 25 DPR-3.3.9
                // End PAR - avg of 3 values-VM
                m_aEndPAR_Avg3_VM[testNumber] = m_aEndPA_Avg3[testNumber] / m_aPh1PA_Avg3[testNumber];
                PrintResultToConsole("End PAR - avg of 3 values-VM", testNumber, m_aEndPAR_Avg3_VM[testNumber]);

                // 26 DPR-3.3.10
                // Min PAR avg of 3 values-VM
                m_aMinPAR_Avg3_VM[testNumber] = m_aMinPA_Avg3[testNumber] / m_aPh1PA_Avg3[testNumber];
                PrintResultToConsole("Min PAR avg of 3 values-VM", testNumber, m_aMinPAR_Avg3_VM[testNumber]);

                // 27 DPR-3.3.11
                // RMS End PAR-VM
                m_aRMSEndPAR_VM[testNumber] = m_aRMSEnd[testNumber] / m_aRMS_Ph1[testNumber];
                PrintResultToConsole("RMS End PAR-VM", testNumber, m_aRMSEndPAR_VM[testNumber]);

                // 28 DPR-3.3.12
                // RMS Min PAR-VM
                m_aRMSMinPAR_VM[testNumber] = m_aRMSMin[testNumber] / m_aRMS_Ph1[testNumber];
                PrintResultToConsole("RMS Min PAR-VM", testNumber, m_aRMSMinPAR_VM[testNumber]);

                // 38 DPR-3.3.31
                // Ph4 PA - peak
                vl = BeatProcessing.getInstance().GetMaxPAInRange(vEndIndex + (int) ((SAMPLES_PER_SECOND * 2.5)), vEndIndex + (int) ((SAMPLES_PER_SECOND * 12.5)), pv, dataSet);
                m_aPh4PA_Peak[testNumber] = vl.value;
                ph4PAPeakIndex = vl.location;
                PrintResultToConsole("Ph4 PA - peak", testNumber, m_aPh4PA_Peak[testNumber]);

                // 29 DPR-3.3.13
                // BL PAR - peak values-PV
                m_aBLPAR_PV[testNumber] = m_aPA_Avg_BL[testNumber] / m_aPh4PA_Peak[testNumber];
                PrintResultToConsole("BL PAR - peak values-PV", testNumber, m_aBLPAR_PV[testNumber]);

                // 30 DPR-3.3.14
                // Ph1 PAR - peak values-PV
                m_aPh1PAR_PV[testNumber] = m_aPh1PA_Peak[testNumber] / m_aPh4PA_Peak[testNumber];
                PrintResultToConsole("Ph1 PAR - peak values-PV", testNumber, m_aPh1PAR_PV[testNumber]);

                // 31 DPR-3.3.15
                // End PAR - peak values-PV
                m_aEndPAR_PV[testNumber] = m_aEndPA_Peak[testNumber] / m_aPh4PA_Peak[testNumber];
                PrintResultToConsole("End PAR - peak values-PV", testNumber, m_aEndPAR_PV[testNumber]);

                // 32 DPR-3.3.16
                // Min PAR - peak values-PV
                m_aMinPAR_PV[testNumber] = m_aMinPA_Peak[testNumber] / m_aPh4PA_Peak[testNumber];
                PrintResultToConsole("Min PAR - peak values-PV", testNumber, m_aMinPAR_PV[testNumber]);

                // 39 DPR-3.3.32
                // Ph4 PA - avg 3
                m_aPh4PA_Avg3[testNumber] = BeatProcessing.getInstance().GetPh4PAAvg3(ph4PAPeakIndex, vEndIndex, pv, dataSet);
                PrintResultToConsole("Ph4 PA - avg 3", testNumber, m_aPh4PA_Avg3[testNumber]);

                // 33 DPR-3.3.17
                // BL PAR - avg of 3 values -PV
                m_aBLPAR_Avg3_PV[testNumber] = m_aPA_Avg_BL[testNumber] / m_aPh4PA_Avg3[testNumber];
                PrintResultToConsole("BL PAR - avg of 3 values -PV", testNumber, m_aBLPAR_Avg3_PV[testNumber]);

                // 34 DPR-3.3.18
                // Ph1 PAR - avg of 3 values-PV
                m_aPh1PAR_Avg3_PV[testNumber] = m_aPh1PA_Avg3[testNumber] / m_aPh4PA_Avg3[testNumber];
                PrintResultToConsole("Ph1 PAR - avg of 3 values-PV", testNumber, m_aPh1PAR_Avg3_PV[testNumber]);

                // 35 DPR-3.3.19
                // End PAR - avg of 3 values-PV
                m_aEndPAR_Avg3_PV[testNumber] = m_aEndPA_Avg3[testNumber] / m_aPh4PA_Avg3[testNumber];
                PrintResultToConsole("End PAR - avg of 3 values-PV", testNumber, m_aEndPAR_Avg3_PV[testNumber]);

                // 36 DPR-3.3.20
                // Min PAR - avg of 3 values-PV
                m_aMinPAR_Avg3_PV[testNumber] = m_aMinPA_Avg3[testNumber] / m_aPh4PA_Avg3[testNumber];
                PrintResultToConsole("Min PAR - avg of 3 values-PV", testNumber, m_aMinPAR_Avg3_PV[testNumber]);

                // 40 DPR-3.3.40
                // Time for Phase 2
                m_aTime_Ph2[testNumber] = (double) (minPAPeakIndex - ph1PAPeakIndex) / SAMPLES_PER_SECOND;
                PrintResultToConsole("Time for Phase 2", testNumber, m_aTime_Ph2[testNumber]);

                // 37 DPR-3.3.25
                // Slope of Phase 2
                m_aSlope_Ph2[testNumber] = m_aMinPAR_PV_VM[testNumber] / m_aTime_Ph2[testNumber];
                PrintResultToConsole("Slope of Phase 2", testNumber, m_aSlope_Ph2[testNumber]);

                // 41 DPR-3.3.41
                // Ph1 peak PA - min PA
                int ph1Peak = dataSet.get(ph1PAPeakIndex).m_PPG;
                int minPAPeak = dataSet.get(minPAPeakIndex).m_PPG;
                m_aPh1PeakPA_MinPA[testNumber] = ph1Peak - minPAPeak;
                PrintResultToConsole("Ph1 peak PA - min PA", testNumber, m_aPh1PeakPA_MinPA[testNumber]);

                // 42 DPR-3.3.42
                // Mean VM pressure
                m_aMeanVM_pressure[testNumber] = CalculateMeanPressure(t0Index, t10Index, dataSet);
                PrintResultToConsole("Mean VM pressure", testNumber, m_aMeanVM_pressure[testNumber]);

                // 43 DPR-3.3.43
                // Median VM pressure
                m_aMedianVM_pressure[testNumber] = CalculateMedianPressure(t0Index, t10Index, dataSet);
                PrintResultToConsole("Median VM pressure", testNumber, m_aMedianVM_pressure[testNumber]);

                // 44 DPR-3.3.44
                // BL HR - avg
                m_aBLHR_Avg[testNumber] = HeartRateInfo.getInstance().GetAvgHRInRange(baselineStartIndex, baselineEndIndex, pv, dataSet);
                PrintResultToConsole("BL HR - avg", testNumber, m_aBLHR_Avg[testNumber]);

                // 45 DPR-3.3.45
                // Ph2 HR - avg
                m_aPh2HR_Avg[testNumber] = HeartRateInfo.getInstance().GetAvgHRInRange(t0Index, t10Index, pv, dataSet);
                PrintResultToConsole("Ph2 HR - avg", testNumber, m_aPh2HR_Avg[testNumber]);

                // 46 DPR-3.3.46
                // Ph2 HR min
                m_aPh2HR_min[testNumber] = HeartRateInfo.getInstance().MinimumHeartRate(t0Index, t10Index, 1, pv);
                PrintResultToConsole("Ph2 HR min", testNumber, m_aPh2HR_min[testNumber]);

                // 47 DPR-3.3.47
                // Ph2 PA avg
                m_aPh2PA_avg[testNumber] = BeatProcessing.getInstance().GetAvgPAInRange(t0Index, t10Index, pv, dataSet);
                PrintResultToConsole("Ph2 PA avg", testNumber, m_aPh2PA_avg[testNumber]);

            } catch (Exception e)
            {

            }

            return true;
        }
        else
        {
            return false;
        }
    }


    public boolean SaveCSVFile(String fileName)
    {
        filename = fileName;
        String baseDir = System.getProperty("user.home");
        String desktop = "/Desktop/Desktop Tool Output Data";
        String directoryName = baseDir + desktop;
        String filePath = directoryName + File.separator + fileName + ".csv";

        File directory = new File(directoryName);
        if (!directory.exists())
        {   // if directory doesn't exist, create one
            directory.mkdir();
        }

        File file = new File(filePath);

        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            WriteCSVContents(pw);
            file.setWritable(true);
            pw.flush();
            pw.close();
            fos.close();

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            /*Log.i(TAG, "******* File not found. Did you"
                    + " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");*/
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    private void WriteCSVContents(PrintWriter writer)
    {
        writer.println("Filename, " + filename + ", " + "Desktop Tool Software Version, " + desktopToolSoftwareVersion);
        writer.println("Subject ID, " + PatientInfo.getInstance().get_patientId() + ", Date, " + PatientInfo.getInstance().get_testDateTime());
        writer.println("Calculated values-Trial, 1, 2, 3, Mean");

        PrintVal(writer,"End PAR - peak values-BL", m_aEndPAR_PV_BL);
        PrintVal(writer, "min PAR - peak values-BL", m_aMinPAR_PV_BL);
        PrintVal(writer, "End PAR - avg of 3 values-BL", m_aEndPAR_Avg3_BL);
        PrintVal(writer, "min PAR - avg of 3 values-BL", m_aMinPAR_Avg3_BL);
        PrintVal(writer, "RMS end PAR-BL", m_aRMSEndPAR_BL);
        PrintVal(writer, "RMS min PAR-BL", m_aRMSMinPAR_BL);
        PrintVal(writer, "End PAR - peak values-VM", m_aEndPAR_PV_VM);
        PrintVal(writer, "min PAR - peak values-VM", m_aMinPAR_PV_VM);
        PrintVal(writer, "End PAR - avg of 3 values-VM", m_aEndPAR_Avg3_VM);
        PrintVal(writer, "min PAR - avg of 3 values-VM", m_aMinPAR_Avg3_VM);
        PrintVal(writer, "RMS end PAR-VM", m_aRMSEndPAR_VM);
        PrintVal(writer, "RMS min PAR-VM", m_aRMSMinPAR_VM);
        PrintVal(writer, "BL PAR - peak values-PV", m_aBLPAR_PV);
        PrintVal(writer, "Ph1 PAR - peak values-PV", m_aPh1PAR_PV);
        PrintVal(writer, "End PAR - peak values-PV", m_aEndPAR_PV);
        PrintVal(writer, "min PAR - peak values-PV", m_aMinPAR_PV);
        PrintVal(writer, "BL PAR - avg of 3 values-PV", m_aBLPAR_Avg3_PV);
        PrintVal(writer, "Ph1 PAR - avg of 3 values-PV", m_aPh1PAR_Avg3_PV);
        PrintVal(writer, "End PAR - avg of 3 values-PV", m_aEndPAR_Avg3_PV);
        PrintVal(writer, "min PAR - avg of 3 values-PV", m_aMinPAR_Avg3_PV);
        PrintVal(writer, "RMS BL PAR-PV", m_aRMSBLPAR_PV);
        PrintVal(writer, "RMS Ph1 PAR-PV", m_aRMSPh1PAR_PV);
        PrintVal(writer, "RMS end PAR-PV", m_aRMSEndPAR_PV);
        PrintVal(writer, "RMS min PAR-PV", m_aRMSMinPAR_PV);
        PrintVal(writer, "Slope of Phase 2", m_aSlope_Ph2);
        PrintVal(writer, "BL PA - avg", m_aPA_Avg_BL);
        PrintVal(writer, "RMS BL", m_aRMSBL);
        PrintVal(writer, "Ph1 PA - peak", m_aPh1PA_Peak);
        PrintVal(writer, "Ph1 PA - avg 3", m_aPh1PA_Avg3);
        PrintVal(writer, "RMS Ph1", m_aRMS_Ph1);
        PrintVal(writer, "Post VM PA - peak", m_aPh4PA_Peak);
        PrintVal(writer, "Post VM PA - avg 3", m_aPh4PA_Avg3);
        PrintVal(writer, "RMS Post VM", m_aRMS_Ph4);
        PrintVal(writer, "end PA - peak", m_aEndPA_Peak);
        PrintVal(writer, "min PA - peak", m_aMinPA_Peak);
        PrintVal(writer, "end PA - avg 3", m_aEndPA_Avg3);
        PrintVal(writer, "min PA - avg 3", m_aMinPA_Avg3);
        PrintVal(writer, "RMS end", m_aRMSEnd);
        PrintVal(writer, "RMS min", m_aRMSMin);
        PrintVal(writer, "Time for phase 2", m_aTime_Ph2);
        PrintVal(writer, "Ph1 peak PA - Ph2 min PA", m_aPh1PeakPA_MinPA);
        PrintVal(writer, "Mean VM Pressure", m_aMeanVM_pressure);
        PrintVal(writer, "Median VM Pressure", m_aMedianVM_pressure);
        PrintVal(writer, "BL HR - avg", m_aBLHR_Avg);
        PrintVal(writer, "Ph2 HR avg", m_aPh2HR_Avg);
        PrintVal(writer, "Ph2 HR min", m_aPh2HR_min);
        PrintVal(writer, "Ph2 PA avg", m_aPh2PA_avg);
    }
    
    private void PrintVal(PrintWriter writer, String label, double[] val)
    {
        double total = 0;
        double count = 0;

        writer.print(label + ", ");

        if (val[0] < 0.0)
        {
            writer.print("ERROR, ");
        }
        else
        {
            writer.print(FormatDoubleForPrint(val[0]) + ", ");
            total += val[0];
            count++;
        }

        if (val[1] < 0.0)
        {
            writer.print("ERROR, ");
        }
        else
        {
            writer.print(FormatDoubleForPrint(val[1]) + ", ");
            total += val[1];
            count++;
        }

        if (val[2] < 0.0)
        {
            writer.print("ERROR, ");
        }
        else
        {
            writer.print(FormatDoubleForPrint(val[2]) + ", ");
            total += val[2];
            count++;
        }

        if (count > 0)
        {
            writer.println(FormatDoubleForPrint(total/count));
        }
        else
        {
            writer.println("ERROR");
        }

    }

    private String FormatDoubleForPrint(double value)
    {
        String result = String.format("%1$,.4f", value);

        // get rid of the commas cause it's going to a csv file
        String clean = result.replaceAll(",", "");

        return clean;
    }
}