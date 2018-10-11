package com.vixiar.indicor.Data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.vixiar.indicor.BuildConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Created by Dave on 7/12/2017.
 */

public class PatientInfo
{
    private final static String TAG = PatientInfo.class.getSimpleName();

    private static final PatientInfo ourInstance = new PatientInfo();
    private String m_patientId;
    private int m_systolicBloodPressure;
    private int m_diastolicBloodPressure;
    private int m_height_Inches;
    private int m_weight_lbs;
    private int m_age_years;
    private String m_applicationVersion;
    private String m_studyLocation;
    private String m_handheldSerialNumber;
    private String m_firmwareVersion;
    private String m_testDate;
    private String m_gender;
    private String m_notes;
    private String m_questionnaire_1;
    private String m_questionnaire_2;
    private String m_questionnaire_3;
    private String m_questionnaire_4;
    private String m_questionnaire_5;
    private String m_questionnaire_6;
    private RealtimeData rtd = new RealtimeData();

    private enum Algo { PVD, RMS }
    private Algo AlgoChoice = Algo.RMS;

    // Constants
    private static int SAMPLES_IN_TEN_SECONDS = (50 * 10);
    private static int SAMPLES_IN_SECONDS = SAMPLES_IN_TEN_SECONDS / 10;

    // calculated data
    private final int NUM_TESTS = 3;
    private double[] m_aCalcPAAvgRest = new double[NUM_TESTS];
    private double[] m_aCalcHRAvgRest = new double[NUM_TESTS];
    private double[] m_aCalcPAAvgVM = new double[NUM_TESTS];
    private double[] m_aCalcHRAvgVM = new double[NUM_TESTS];
    private double[] m_aCalcMinPA = new double[NUM_TESTS];
    private double[] m_aCalcEndPA = new double[NUM_TESTS];
    private double[] m_aCalcMinPAR = new double[NUM_TESTS];
    private double[] m_aCalcEndPAR = new double[NUM_TESTS];
    private double[] m_aCalcMinHRVM = new double[NUM_TESTS];
    private double[] m_aCalcLVEDP = new double[NUM_TESTS];
    private double[] m_aRMSEnd = new double[NUM_TESTS];
    private double[] m_aRMSMin = new double[NUM_TESTS];
    private double[] m_aRMSBL = new double[NUM_TESTS];
    private double[] m_aRMSMinPAR_PV = new double[NUM_TESTS];
    private double[] m_aRMSEndPAR_PV = new double[NUM_TESTS];
    private double[] m_aRMSPh1PAR_PV = new double[NUM_TESTS];
    private double[] m_aRMSBLPAR_PV = new double[NUM_TESTS];
    private double[] m_aRMS_Ph1 = new double[NUM_TESTS];
    private double[] m_aRMS_Ph4 = new double[NUM_TESTS];
    private double[] m_aRMSEndPAR_VM = new double[NUM_TESTS];
    private double[] m_aRMSMinPAR_VM = new double[NUM_TESTS];
    private double[] m_aRMSEndPAR_BL = new double[NUM_TESTS];
    private double[] m_aRMSMinPAR_BL = new double[NUM_TESTS];
    private double[] m_aRMS_LVEDP = new double[NUM_TESTS];

    public void ClearAllPatientData()
    {
        m_patientId = "";
        m_systolicBloodPressure = 0;
        m_diastolicBloodPressure = 0;
        m_height_Inches = 0;
        m_weight_lbs = 0;
        m_age_years = 0;
        m_firmwareVersion = "";
        m_testDate = "";
        m_gender = "";
        m_notes = "";
        m_questionnaire_1 = "";
        m_questionnaire_2 = "";
        m_questionnaire_3 = "";
        m_questionnaire_4 = "";
        m_questionnaire_5 = "";
        m_questionnaire_6 = "";
        Arrays.fill(m_aCalcEndPA, 0.0);
        Arrays.fill(m_aCalcEndPAR, 0.0);
        Arrays.fill(m_aCalcHRAvgRest, 0.0);
        Arrays.fill(m_aCalcHRAvgVM, 0.0);
        Arrays.fill(m_aCalcLVEDP, 0.0);
        Arrays.fill(m_aCalcMinHRVM, 0.0);
        Arrays.fill(m_aCalcMinPA, 0.0);
        Arrays.fill(m_aCalcMinPAR, 0.0);
        Arrays.fill(m_aCalcPAAvgRest, 0.0);
        Arrays.fill(m_aCalcPAAvgVM, 0.0);
        Arrays.fill(m_aRMSEnd, 0.0);
        Arrays.fill(m_aRMSMin, 0.0);
        Arrays.fill(m_aRMSBL, 0.0);
        Arrays.fill(m_aRMSEndPAR_BL, 0.0);
        Arrays.fill(m_aRMSMinPAR_BL, 0.0);
        Arrays.fill(m_aRMSMinPAR_PV, 0.0);
        Arrays.fill(m_aRMSEndPAR_PV, 0.0);
        Arrays.fill(m_aRMSPh1PAR_PV, 0.0);
        Arrays.fill(m_aRMSBLPAR_PV, 0.0);
        Arrays.fill(m_aRMS_Ph1, 0.0);
        Arrays.fill(m_aRMS_Ph4, 0.0);
        Arrays.fill(m_aRMSEndPAR_VM, 0.0);
        Arrays.fill(m_aRMSMinPAR_VM, 0.0);
        Arrays.fill(m_aRMS_LVEDP, 0.0);
        rtd.ClearAllData();
    }

    public void set_studyLocation(String location)
    {
        this.m_studyLocation = location;
    }

    public String get_studyLocation()
    {
        return m_studyLocation;
    }

    public void set_applicationVersion(String m_applicationVersion)
    {
        this.m_applicationVersion = m_applicationVersion;
    }

    public void set_firmwareRevision(String m_firmwareVersion)
    {
        this.m_firmwareVersion = m_firmwareVersion;
    }

    public double get_LVEDP(int testNumber)
    {
        if (testNumber <= NUM_TESTS)
        {
            if(AlgoChoice == Algo.PVD)
            {
                return m_aCalcLVEDP[testNumber];
            }
            else if (AlgoChoice == Algo.RMS)
            {
                return m_aRMS_LVEDP[testNumber];
            }
            else
            {
                return 0.0;
            }
        }
        else
        {
            return 0.0;
        }
    }

    public String get_testDate()
    {
        return m_testDate;
    }

    public void set_testDate(String m_testDate)
    {
        this.m_testDate = m_testDate;
    }

    public String get_handheldSerialNumber()
    {
        return m_handheldSerialNumber;
    }

    public void set_handheldSerialNumber(String m_handheldSerialNumber)
    {
        this.m_handheldSerialNumber = m_handheldSerialNumber;
    }

    public static PatientInfo getInstance()
    {
        return ourInstance;
    }

    public void set_gender(String m_gender)
    {
        this.m_gender = m_gender;
    }

    public void set_notes(String m_notes)
    {
        this.m_notes = m_notes;
    }

    public RealtimeData getRealtimeData()
    {
        return rtd;
    }

    public String get_patientId()
    {
        return m_patientId;
    }

    public void set_patientId(String m_patientId)
    {
        this.m_patientId = m_patientId;
    }

    public void set_systolicBloodPressure(int m_systolicBloodPressure)
    {
        this.m_systolicBloodPressure = m_systolicBloodPressure;
    }

    public void set_diastolicBloodPressure(int m_diastolicBloodPressure)
    {
        this.m_diastolicBloodPressure = m_diastolicBloodPressure;
    }

    public void set_height_Inches(int m_height_Inches)
    {
        this.m_height_Inches = m_height_Inches;
    }

    public void set_weight_lbs(int m_weight_lbs)
    {
        this.m_weight_lbs = m_weight_lbs;
    }

    public void set_age_years(int m_age_years)
    {
        this.m_age_years = m_age_years;
    }

    public void set_questionnaire(String m_q1, String m_q2, String m_q3, String m_q4, String m_q5, String m_q6)
    {
        this.m_questionnaire_1 = m_q1;
        this.m_questionnaire_2 = m_q2;
        this.m_questionnaire_3 = m_q3;
        this.m_questionnaire_4 = m_q4;
        this.m_questionnaire_5 = m_q5;
        this.m_questionnaire_6 = m_q6;
    }

    // test number is 0 relative
    public boolean CalculateResults(int testNumber)
    {
        TestMarkers tm = GetTestMarkers(testNumber);

        // make sure the test markers were found
        if (tm.endIndex != 0 && tm.startIndex != 0)
        {
            // make sure there's at least 10 seconds worth of samples before the start of valsalva
            if (tm.startIndex > SAMPLES_IN_TEN_SECONDS)
            {
                // get the avg PA during rest (by definition, rest ends 10 seconds before the patient hits the 16mm pressure point)
                m_aCalcPAAvgRest[testNumber] = HeartRateInfo.getInstance().GetHistoricalAvgPA((tm.startIndex - SAMPLES_IN_TEN_SECONDS), 12);

                // get the avg HR during rest
                m_aCalcHRAvgRest[testNumber] = HeartRateInfo.getInstance().GetHistoricalAvgHR((tm.startIndex - SAMPLES_IN_TEN_SECONDS), 12);
            }
            else
            {
                m_aCalcPAAvgRest[testNumber] = 0;
                m_aCalcHRAvgRest[testNumber] = 0;
            }

            // get the avg PA during Valsalva
            m_aCalcPAAvgVM[testNumber] = HeartRateInfo.getInstance().GetAvgPAOverRange(tm.startIndex, tm.endIndex);

            // get the avg HR during Valsalva
            m_aCalcHRAvgVM[testNumber] = HeartRateInfo.getInstance().GetAvgHROverRange(tm.startIndex, tm.endIndex);

            // get the min PA during Valsalva
            m_aCalcMinPA[testNumber] = HeartRateInfo.getInstance().GetMinPAOverRange(tm.startIndex, tm.endIndex);

            // get the end PA during Valsalva
            m_aCalcEndPA[testNumber] = HeartRateInfo.getInstance().GetHistoricalAvgPA(tm.endIndex, 1);

            // make sure that the PA Avg rest isn't 0
            if (m_aCalcPAAvgRest[testNumber] > 0)
            {
                m_aCalcMinPAR[testNumber] = m_aCalcMinPA[testNumber] / m_aCalcPAAvgRest[testNumber];
                m_aCalcEndPAR[testNumber] = m_aCalcEndPA[testNumber] / m_aCalcPAAvgRest[testNumber];
            }
            else
            {
                m_aCalcMinPAR[testNumber] = 0;
                m_aCalcEndPAR[testNumber] = 0;
            }

            // get the end HR during valsalva
            m_aCalcMinHRVM[testNumber] = HeartRateInfo.getInstance().MinimumHeartRate(tm.startIndex, tm.endIndex, 1);

            // if something went wrong, indicate that by setting the LVEDP to 0
            if (m_aCalcMinPAR[testNumber] != 0)
            {
                m_aCalcLVEDP[testNumber] = -4.52409 + (21.25779 * m_aCalcMinPAR[testNumber]) + (0.03415 * m_height_Inches * 2.54) -
                        (0.20827 * m_diastolicBloodPressure) + (0.09374 * m_systolicBloodPressure) +
                        (0.16182 * m_aCalcHRAvgRest[testNumber]) - (0.06949 * m_age_years);
            }
            else
            {
                m_aCalcLVEDP[testNumber] = 0;
            }

            DoRMSCalculations(testNumber);
            return true;
        }
        else
        {
            return false;
        }
    }

    public static int FindVStart(int testNumber)
    {
        TestMarkers tm;
        tm = PatientInfo.getInstance().GetTestMarkers(testNumber);
        int searchIndex = tm.startIndex;
        int startLimit = searchIndex - (SAMPLES_IN_TEN_SECONDS);  // 10 sec backward from T0--startIndex
        int vStart = -1;
        boolean vStartFound = false;

        // go backwards in time until the pressure goes below 2, then find when it goes over 2
        while (!vStartFound && (searchIndex > startLimit))
        {
            if (PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(searchIndex).m_pressure <= 2.0)
            {
                while (PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(searchIndex).m_pressure <= 2.0)
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

    public static int FindVEnd(int testNumber)
    {
        TestMarkers tm;
        tm = PatientInfo.getInstance().GetTestMarkers(testNumber);
        int endIndex = tm.endIndex;
        int endLimit = endIndex + (10 * 50);  // 10 sec forward from T10--endIndex
        double slope = 0.0;

        for (; endIndex != endLimit; endIndex++)
        {   // repeat loop until 10 sec forward from endIndex (T10)
            if (PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(endIndex).m_pressure < 16.0)
            {
                return endIndex;
            }
        }
        return -1;
    }

    private static int FindT0FromVStart(int vStart)
    {
        // move from Vstart until pressure crosses 16
        int index = vStart;
        while (PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(index).m_pressure < 16.0)
        {
            index++;
        }
        return index;
    }

    private static void PrintResult(String label, int testNumber, double value)
    {
        System.out.println(label + " " + "[" + (testNumber + 1) + "]" + ": " + value);
    }

    private boolean DoRMSCalculations(int testNumber) {

        int vStartIndex = FindVStart(testNumber);
        int vEndIndex = FindVEnd(testNumber);
        PrintResult("VStart", testNumber, vStartIndex);
        PrintResult("VEnd", testNumber, vEndIndex);

        int t0Index = FindT0FromVStart(vStartIndex);
        int t10Index = t0Index + SAMPLES_IN_TEN_SECONDS;
        PrintResult("t0", testNumber, t0Index);
        PrintResult("t10", testNumber, t10Index);
        // 10 DPR-3.3.38
        // RMS end
        m_aRMSEnd[testNumber] = HeartRateInfo.getInstance().GetRMSInRange(t10Index - (SAMPLES_IN_SECONDS * 3), t10Index);
        PrintResult("RMS end", testNumber, m_aRMSEnd[testNumber]);

        // 11 DPR-3.3.39
        // RMS min
        m_aRMSMin[testNumber] = HeartRateInfo.getInstance().GetMinRMS(t0Index, t10Index, 3);
        PrintResult("RMS min", testNumber, m_aRMSMin[testNumber]);

        // 12 DPR-3.3.27
        // RMS BL
        m_aRMSBL[testNumber] = HeartRateInfo.getInstance().GetRMSInRange(vStartIndex-(SAMPLES_IN_SECONDS*15), vStartIndex-(SAMPLES_IN_SECONDS*5));
        PrintResult("RMS BL", testNumber, m_aRMSBL[testNumber]);

        // 13 DPR-3.3.5
        // RMS end PAR-BL
        m_aRMSEndPAR_BL[testNumber] = m_aRMSEnd[testNumber] / m_aRMSBL[testNumber];
        PrintResult("RMS end PAR-BL", testNumber, m_aRMSEndPAR_BL[testNumber]);

        // 14 DPR-3.3.6
        // RMS min PAR-BL
        m_aRMSMinPAR_BL[testNumber] = m_aRMSMin[testNumber] / m_aRMSBL[testNumber];
        PrintResult("RMS min PAR-BL", testNumber, m_aRMSMinPAR_BL[testNumber]);

        // 18 DPR-3.3.30
        // RMS Ph1
        m_aRMS_Ph1[testNumber] = HeartRateInfo.getInstance().GetRMSInRange(vStartIndex, vStartIndex + (SAMPLES_IN_SECONDS * 3));
        PrintResult("RMS Ph1", testNumber, m_aRMS_Ph1[testNumber]);

        // 19 DPR-3.3.33
        // RMS Ph4
        int ph4Index = vEndIndex + ((int) (SAMPLES_IN_SECONDS * 2.5));
        m_aRMS_Ph4[testNumber] = HeartRateInfo.getInstance().GetRMSInRange(ph4Index, ph4Index + (SAMPLES_IN_TEN_SECONDS));
        PrintResult("RMS Ph4", testNumber, m_aRMS_Ph4[testNumber]);

        // 20 3.3.21
        // RMS BL PAR-PV
        m_aRMSBLPAR_PV[testNumber] = m_aRMSBL[testNumber] / m_aRMS_Ph4[testNumber];
        PrintResult("RMS BL PAR-PV", testNumber, m_aRMSBLPAR_PV[testNumber]);

        // 21 3.3.22
        // RMS Ph1 PAR-PV
        m_aRMSPh1PAR_PV[testNumber] = m_aRMS_Ph1[testNumber] / m_aRMS_Ph4[testNumber];
        PrintResult("RMS Ph1 PAR-PV", testNumber, m_aRMSPh1PAR_PV[testNumber]);

        // 22 3.3.23
        // RMS End PAR-PV
        m_aRMSEndPAR_PV[testNumber] = m_aRMSEnd[testNumber] / m_aRMS_Ph4[testNumber];
        PrintResult("RMS End PAR-PV", testNumber, m_aRMSEndPAR_PV[testNumber]);

        // 23 3.3.24
        // RMS Min PAR-PV
        m_aRMSMinPAR_PV[testNumber] = m_aRMSMin[testNumber] / m_aRMS_Ph4[testNumber];
        PrintResult("RMS Min PAR-PV", testNumber, m_aRMSMinPAR_PV[testNumber]);

        // 27 DPR-3.3.11
        // RMS End PAR-VM
        m_aRMSEndPAR_VM[testNumber] = m_aRMSEnd[testNumber] / m_aRMS_Ph1[testNumber];
        PrintResult("RMS End PAR-VM", testNumber, m_aRMSEndPAR_VM[testNumber]);

        // 28 DPR-3.3.12
        // RMS Min PAR-VM
        m_aRMSMinPAR_VM[testNumber] = m_aRMSMin[testNumber] / m_aRMS_Ph1[testNumber];
        PrintResult("RMS Min PAR-VM", testNumber, m_aRMSMinPAR_VM[testNumber]);

        m_aRMS_LVEDP[testNumber] = -4.52409 + (21.25779 * m_aRMSEndPAR_BL[testNumber]) + (0.03415 * m_height_Inches * 2.54) -
                (0.20827 * m_diastolicBloodPressure) + (0.09374 * m_systolicBloodPressure) +
                (0.16182 * m_aCalcHRAvgRest[testNumber]) - (0.06949 * m_age_years);

        return true;
    }

    // get the start and end of valsalva markers for a certain test
    // both markers will be 0 if the test is not found
    private TestMarkers GetTestMarkers(int testNumber)
    {
        TestMarkers m = new TestMarkers();
        m.endIndex = 0;
        m.startIndex = 0;

        int testNumberFound = 0;
        int currentIndex = 0;
        boolean finished = false;

        while (currentIndex < rtd.GetDataMarkers().size() && !finished)
        {
            // loop through the markers till we find a start valsalva
            if (rtd.GetDataMarkers().get(currentIndex).type == RealtimeDataMarker.Marker_Type.MARKER_START_VALSALVA)
            {
                // make sure there's another marker to see if it's the end
                if (rtd.GetDataMarkers().size() > currentIndex + 1)
                {
                    // make sure the next marker is an end valsalva
                    if (rtd.GetDataMarkers().get(currentIndex + 1).type == RealtimeDataMarker.Marker_Type.MARKER_END_VALSALVA)
                    {
                        if (testNumberFound == testNumber)
                        {
                            finished = true;
                            m.startIndex = rtd.GetDataMarkers().get(currentIndex).dataIndex;
                            m.endIndex = rtd.GetDataMarkers().get(currentIndex + 1).dataIndex;
                        }
                        testNumberFound++;
                    }
                }
                else // there's a start test and not an end test
                {
                    // finished cause there's nothing left
                    finished = true;
                }
            }
            currentIndex++;
        }
        return m;
    }

    private class TestMarkers
    {
        private int startIndex;
        private int endIndex;
    }

    public boolean SaveCSVFile(Context context)
    {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = m_patientId + "-" + m_testDate + ".csv";
        String filePath = baseDir + File.separator + fileName;
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

            // now we need to force android to rescan the file system so the file will show up
            // if you want to load it via usb
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you"
                    + " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    private boolean WriteCSVContents(PrintWriter writer)
    {
        writer.println("Test date time, " + m_testDate);
        writer.println("Application version, " + BuildConfig.VERSION_NAME);
        writer.println("Handheld serial number," + m_handheldSerialNumber);
        writer.println("Firmware version, " + m_firmwareVersion);
        writer.println("Study location, " + m_studyLocation);
        writer.println("Subject ID, " + m_patientId);
        writer.println("Age, " + m_age_years);
        writer.println("Gender, " + m_gender);
        writer.println("Systolic blood pressure, " + m_systolicBloodPressure);
        writer.println("Diastolic blood pressure, " + m_diastolicBloodPressure);
        writer.println("Mean blood pressure, " + (2 * m_diastolicBloodPressure + m_systolicBloodPressure) / 3);
        writer.println("Height (in.), " + m_height_Inches);
        writer.println("Weight (lbs.), " + m_weight_lbs);
        if(m_questionnaire_1 != "" || m_questionnaire_1 != null) writer.println("Questionnaire 1, " + m_questionnaire_1);
        if(m_questionnaire_2 != "" || m_questionnaire_2 != null) writer.println("Questionnaire 2, " + m_questionnaire_2);
        if(m_questionnaire_3 != "" || m_questionnaire_3 != null) writer.println("Questionnaire 3, " + m_questionnaire_3);
        if(m_questionnaire_4 != "" || m_questionnaire_4 != null) writer.println("Questionnaire 4, " + m_questionnaire_4);
        if(m_questionnaire_5 != "" || m_questionnaire_5 != null) writer.println("Questionnaire 5, " + m_questionnaire_5);
        if(m_questionnaire_6 != "" || m_questionnaire_6 != null) writer.println("Questionnaire 6, " + m_questionnaire_6);

        // calculate some stuff for BMI
        double height_m = m_height_Inches * 0.0254;
        double weight_kg = m_weight_lbs * 0.4536;
        double bmi = (weight_kg / (height_m * height_m));
        writer.println("BMI (Kg/m^2):, " + FormatDoubleForPrint(bmi));

        writer.println("Notes:, " + m_notes);

        // print all of the calculated data
        writer.println("Calculated values-Trial, 1, 2, 3");
        writer.println("PA avg rest, " + FormatDoubleForPrint(m_aCalcPAAvgRest[0]) + ", " +
                FormatDoubleForPrint(m_aCalcPAAvgRest[1]) + ", " + FormatDoubleForPrint(m_aCalcPAAvgRest[2]));
        writer.println("HR avg (BPM) rest, " + FormatDoubleForPrint(m_aCalcHRAvgRest[0]) + ", " +
                FormatDoubleForPrint(m_aCalcHRAvgRest[1]) + ", " + FormatDoubleForPrint(m_aCalcHRAvgRest[2]));
        writer.println("PA avg VM, " + FormatDoubleForPrint(m_aCalcPAAvgVM[0]) + ", " +
                FormatDoubleForPrint(m_aCalcPAAvgVM[1]) + ", " + FormatDoubleForPrint(m_aCalcPAAvgVM[2]));
        writer.println("HR avg (BPM) VM, " + FormatDoubleForPrint(m_aCalcHRAvgVM[0]) + ", " +
                FormatDoubleForPrint(m_aCalcHRAvgVM[1]) + ", " + FormatDoubleForPrint(m_aCalcHRAvgVM[2]));
        writer.println("Min PA, " + FormatDoubleForPrint(m_aCalcMinPA[0]) + ", " +
                FormatDoubleForPrint(m_aCalcMinPA[1]) + ", " + FormatDoubleForPrint(m_aCalcMinPA[2]));
        writer.println("End PA, " + FormatDoubleForPrint(m_aCalcEndPA[0]) + ", " +
                FormatDoubleForPrint(m_aCalcEndPA[1]) + ", " + FormatDoubleForPrint(m_aCalcEndPA[2]));
        writer.println("Min PAR, " + FormatDoubleForPrint(m_aCalcMinPAR[0]) + ", " +
                FormatDoubleForPrint(m_aCalcMinPAR[1]) + ", " + FormatDoubleForPrint(m_aCalcMinPAR[2]));
        writer.println("End PAR, " + FormatDoubleForPrint(m_aCalcEndPAR[0]) + ", " +
                FormatDoubleForPrint(m_aCalcEndPAR[1]) + ", " + FormatDoubleForPrint(m_aCalcEndPAR[2]));
        writer.println("Min HR, " + FormatDoubleForPrint(m_aCalcMinHRVM[0]) + ", " +
                FormatDoubleForPrint(m_aCalcMinHRVM[1]) + ", " + FormatDoubleForPrint(m_aCalcMinHRVM[2]));
        writer.println("LVEDP, " + FormatDoubleForPrint(m_aCalcLVEDP[0]) + ", " +
                FormatDoubleForPrint(m_aCalcLVEDP[1]) + ", " + FormatDoubleForPrint(m_aCalcLVEDP[2]));
        writer.println("RMS End, " + FormatDoubleForPrint(m_aRMSEnd[0]) + ", " +
                FormatDoubleForPrint(m_aRMSEnd[1]) + ", " + FormatDoubleForPrint(m_aRMSEnd[2]));
        writer.println("RMS Min, " + FormatDoubleForPrint(m_aRMSMin[0]) + ", " +
                FormatDoubleForPrint(m_aRMSMin[1]) + ", " + FormatDoubleForPrint(m_aRMSMin[2]));
        writer.println("RMS BL, " + FormatDoubleForPrint(m_aRMSBL[0]) + ", " +
                FormatDoubleForPrint(m_aRMSBL[1]) + ", " + FormatDoubleForPrint(m_aRMSBL[2]));
        writer.println("RMS end PAR-BL, " + FormatDoubleForPrint(m_aRMSEndPAR_BL[0]) + ", " +
                FormatDoubleForPrint(m_aRMSEndPAR_BL[1]) + ", " + FormatDoubleForPrint(m_aRMSEndPAR_BL[2]));
        writer.println("RMS min PAR-BL, " + FormatDoubleForPrint(m_aRMSMinPAR_BL[0]) + ", " +
                FormatDoubleForPrint(m_aRMSMinPAR_BL[1]) + ", " + FormatDoubleForPrint(m_aRMSMinPAR_BL[2]));
        writer.println("RMS Ph1, " + FormatDoubleForPrint(m_aRMS_Ph1[0]) + ", " +
                FormatDoubleForPrint(m_aRMS_Ph1[1]) + ", " + FormatDoubleForPrint(m_aRMS_Ph1[2]));
        writer.println("RMS Ph4, " + FormatDoubleForPrint(m_aRMS_Ph4[0]) + ", " +
                FormatDoubleForPrint(m_aRMS_Ph4[1]) + ", " + FormatDoubleForPrint(m_aRMS_Ph4[2]));
        writer.println("RMS BL PAR-PV, " + FormatDoubleForPrint(m_aRMSBLPAR_PV[0]) + ", " +
                FormatDoubleForPrint(m_aRMSBLPAR_PV[1]) + ", " + FormatDoubleForPrint(m_aRMSBLPAR_PV[2]));
        writer.println("RMS Ph1 PAR-PV, " + FormatDoubleForPrint(m_aRMSPh1PAR_PV[0]) + ", " +
                FormatDoubleForPrint(m_aRMSPh1PAR_PV[1]) + ", " + FormatDoubleForPrint(m_aRMSPh1PAR_PV[2]));
        writer.println("RMS End PAR-PV, " + FormatDoubleForPrint(m_aRMSEndPAR_PV[0]) + ", " +
                FormatDoubleForPrint(m_aRMSEndPAR_PV[1]) + ", " + FormatDoubleForPrint(m_aRMSEndPAR_PV[2]));
        writer.println("RMS Min PAR-PV, " + FormatDoubleForPrint(m_aRMSMinPAR_PV[0]) + ", " +
                FormatDoubleForPrint(m_aRMSMinPAR_PV[1]) + ", " + FormatDoubleForPrint(m_aRMSMinPAR_PV[2]));
        writer.println("RMS End PAR-VM, " + FormatDoubleForPrint(m_aRMSEndPAR_VM[0]) + ", " +
                FormatDoubleForPrint(m_aRMSEndPAR_VM[1]) + ", " + FormatDoubleForPrint(m_aRMSEndPAR_VM[2]));
        writer.println("RMS Min PAR-VM, " + FormatDoubleForPrint(m_aRMSMinPAR_VM[0]) + ", " +
                FormatDoubleForPrint(m_aRMSMinPAR_VM[1]) + ", " + FormatDoubleForPrint(m_aRMSMinPAR_VM[2]));
        writer.println("RMS LVEDP, " + FormatDoubleForPrint(m_aRMS_LVEDP[0]) + ", " +
                FormatDoubleForPrint(m_aRMS_LVEDP[1]) + ", " + FormatDoubleForPrint(m_aRMS_LVEDP[2]));

        // print all of markers
        writer.println("Marker index, Type");
        for (int i = 0; i < rtd.GetDataMarkers().size(); i++)
        {
            writer.println(rtd.GetDataMarkers().get(i).dataIndex + ", " + rtd.GetDataMarkers().get(i).type);
        }

        // print all of the raw realtime data
        double t = 0.0;
        writer.println("Time (sec.), PPG (raw), Pressure (mmHg)");
        for (int i = 0; i < rtd.GetRawData().size(); i++)
        {
            writer.println(FormatDoubleForPrint(t) + ", " + rtd.GetRawData().get(i).m_PPG + ", " +
                    FormatDoubleForPrint(rtd.GetRawData().get(i).m_pressure));
            t += 0.02;
        }

        // print all of the 5Hz filtered realtime data
        t = 0.0;
        writer.println("Time (sec.), PPG (5Hz filter), Pressure (mmHg)");
        for (int i = 0; i < rtd.GetFilteredData().size(); i++)
        {
            writer.println(FormatDoubleForPrint(t) + ", " + rtd.GetFilteredData().get(i).m_PPG + ", " +
                    FormatDoubleForPrint(rtd.GetRawData().get(i).m_pressure));
            t += 0.02;
        }

        // print all of the detected peaks
        writer.println("Peak positions (sec), PPG");
        for (int i = 0; i < PeakValleyDetect.getInstance().getPeaksIndexes().size(); i++)
        {
            double pos = PeakValleyDetect.getInstance().getPeaksIndexes().get(i) * 0.02;
            writer.println(FormatDoubleForPrint(pos) + ", " + rtd.GetFilteredData().get(PeakValleyDetect.getInstance().getPeaksIndexes().get(i)).m_PPG);
        }

        // print all of the detected valleys
        writer.println("Valley positions (sec), PPG");
        for (int i = 0; i < PeakValleyDetect.getInstance().getValleysIndexes().size(); i++)
        {
            double pos = PeakValleyDetect.getInstance().getValleysIndexes().get(i) * 0.02;
            writer.println(FormatDoubleForPrint(pos) + ", " + rtd.GetFilteredData().get(PeakValleyDetect.getInstance().getValleysIndexes().get(i)).m_PPG);
        }

        return true;
    }

    private String FormatDoubleForPrint(double value)
    {
        String result = String.format("%1$,.2f", value);

        // get rid of the commas cause it's going to a csv file
        String clean = result.replaceAll(",", "");

        return clean;
    }
}
