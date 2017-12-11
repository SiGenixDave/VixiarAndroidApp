package com.vixiar.indicor.Data;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Dave on 7/12/2017.
 */

public class PatientInfo
{
    // TODO: Build a function to write the data to a CSV file
    // TODO: set firmware version from BLE interface

    private final static String TAG = PatientInfo.class.getSimpleName();

    private static final PatientInfo ourInstance = new PatientInfo();
    private String m_patientId;
    private int m_systolicBloodPressure;
    private int m_diastolicBloodPressure;
    private int m_height_Inches;
    private int m_weight_lbs;
    private int m_age_years;
    private String m_applicationVersion;
    private String m_firmwareVersion;
    private String m_testDate;
    private String m_gender;
    private String m_notes;
    private RealtimeData rtd = new RealtimeData();

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
    private double[] m_aCalcMinHR = new double[NUM_TESTS];
    private double[] m_aCalcLVEDP = new double[NUM_TESTS];

    public void set_applicationVersion(String m_applicationVersion)
    {
        this.m_applicationVersion = m_applicationVersion;
    }

    public void set_firmwareVersion(String m_firmwareVersion)
    {
        this.m_firmwareVersion = m_firmwareVersion;
    }

/*
    public double get_LVEDP(int testNumber)
    {
        if (testNumber <= NUM_TESTS)
        {
            return m_aCalcLVEDP;
        }
        else
        {
            return 0.0;
        }
    }
*/

    public String getM_testDate()
    {
        return m_testDate;
    }

    public void setM_testDate(String m_testDate)
    {
        this.m_testDate = m_testDate;
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

    public String getM_patientId()
    {
        return m_patientId;
    }

    public void setM_patientId(String m_patientId)
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

    public void set_eight_Inches(int m_height_Inches)
    {
        this.m_height_Inches = m_height_Inches;
    }

    public void set_weight_lbs(int m_weight_lbs)
    {
        this.m_weight_lbs = m_weight_lbs;
    }

    public void setM_age_years(int m_age_years)
    {
        this.m_age_years = m_age_years;
    }

/*
    public boolean CalculateResults(int testNumber)
    {
        TestMarkers tm = GetTestMarkers(testNumber);

        // make sure the test markers were found
        if (tm.endIndex != 0 && tm.startIndex != 0)
        {

        }
        else
        {
            return false;
        }
    }
*/

    private TestMarkers GetTestMarkers(int testNumber)
    {
        TestMarkers m = new TestMarkers();
        m.endIndex = 0;
        m.startIndex = 0;

        int testFound = 0;
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
                        testFound++;
                        if (testFound == testNumber)
                        {
                            finished = true;
                            m.startIndex = rtd.GetDataMarkers().get(currentIndex).dataIndex;
                            m.endIndex = rtd.GetDataMarkers().get(currentIndex+1).dataIndex;
                        }
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

    public boolean SaveCSVFile()
    {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = m_patientId + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File file = new File(filePath);

        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            WriteContents(pw);
            pw.flush();
            pw.close();
            fos.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you"
                    + " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    private boolean WriteContents(PrintWriter writer)
    {
        writer.println("Hi , How are you");
        writer.println("Hello");
        return true;
    }
}
