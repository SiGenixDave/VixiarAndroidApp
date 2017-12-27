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
        Arrays.fill(m_aCalcEndPA, 0.0);
        Arrays.fill(m_aCalcEndPAR, 0.0);
        Arrays.fill(m_aCalcHRAvgRest, 0.0);
        Arrays.fill(m_aCalcHRAvgVM,0.0);
        Arrays.fill(m_aCalcLVEDP, 0.0);
        Arrays.fill(m_aCalcMinHR, 0.0);
        Arrays.fill(m_aCalcMinPA, 0.0);
        Arrays.fill(m_aCalcMinPAR, 0.0);
        Arrays.fill(m_aCalcPAAvgRest, 0.0);
        Arrays.fill(m_aCalcPAAvgVM, 0.0);
        rtd.ClearAllData();
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
            return m_aCalcLVEDP[testNumber];
        }
        else
        {
            return 0.0;
        }
    }

    public String getM_testDate()
    {
        return m_testDate;
    }

    public void set_testDate(String m_testDate)
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

    // test number is 0 relative
    public boolean CalculateResults(int testNumber)
    {
        TestMarkers tm = GetTestMarkers(testNumber);

        // make sure the test markers were found
        if (tm.endIndex != 0 && tm.startIndex != 0)
        {
            // get the avg PA during rest
            m_aCalcPAAvgRest[testNumber] = HeartRateInfo.getInstance().GetHistoricalAvgPA(tm.startIndex, 12);

            // get the avg HR during rest
            m_aCalcHRAvgRest[testNumber] = HeartRateInfo.getInstance().GetHistoricalAvgHR(tm.startIndex, 12);

            // get the avg PA during Valsalva
            m_aCalcPAAvgVM[testNumber] = HeartRateInfo.getInstance().GetAvgPAOverRange(tm.startIndex, tm.endIndex);

            // get the avg HR during Valsalva
            m_aCalcHRAvgVM[testNumber] = HeartRateInfo.getInstance().GetAvgHROverRange(tm.startIndex, tm.endIndex);

            // get the min PA during Valsalva
            m_aCalcMinPA[testNumber] = HeartRateInfo.getInstance().GetMinPAOverRange(tm.startIndex, tm.endIndex);

            // get the end PA during Valsalva
            m_aCalcEndPA[testNumber] = HeartRateInfo.getInstance().GetHistoricalAvgPA(tm.endIndex, 1);

            m_aCalcMinPAR[testNumber] = m_aCalcMinPA[testNumber] / m_aCalcPAAvgRest[testNumber];

            m_aCalcEndPAR[testNumber] = m_aCalcEndPA[testNumber] / m_aCalcPAAvgRest[testNumber];

            // get the end HR during valsalva
            m_aCalcMinHR[testNumber] = HeartRateInfo.getInstance().GetHistoricalAvgHR(tm.endIndex, tm.endIndex);

            m_aCalcLVEDP[testNumber] = -4.52409 + (21.25779 * m_aCalcMinPAR[testNumber]) + (0.03415 * m_height_Inches * 2.54) -
                    (0.20827 * m_diastolicBloodPressure) + (0.09374 * m_systolicBloodPressure) +
                    (0.16182 * m_aCalcMinHR[testNumber]) - (0.06949 * m_age_years);
            return true;
        }
        else
        {
            return false;
        }
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
            pw.flush();
            pw.close();
            fos.close();

            // now we need to force android to rescan the file system so the file will show up
            // if you want to load it via usb
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

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

    private boolean WriteCSVContents(PrintWriter writer)
    {
        writer.println("Test date time:, " + m_testDate);
        writer.println("Application version:, " + BuildConfig.VERSION_NAME);
        writer.println("Firmware version:, " + m_firmwareVersion);
        writer.println("Subject ID:, " + m_patientId);
        writer.println("Age:, " + m_age_years);
        writer.println("Gender:, " + m_gender);
        writer.println("Systolic blood pressure:, " + m_systolicBloodPressure);
        writer.println("Diastolic blood pressure:, " + m_diastolicBloodPressure);
        writer.println("Mean blood pressure:, " + (2 * m_diastolicBloodPressure + m_systolicBloodPressure) / 3);
        writer.println("Height (in.), " + m_height_Inches);
        writer.println("Weight (lbs.):, " + m_weight_lbs);

        // calculate some stuff for BMI
        double height_m = m_height_Inches * 0.0254;
        double weight_kg = m_weight_lbs * 0.4536;
        double bmi = (weight_kg / (height_m * height_m));
        writer.println("BMI (Kg/m^2):, " + FormatDoubleForPrint(bmi));

        writer.println("Notes:, " + m_notes);

        // print all of the calculated data
        writer.println("Calculated values-Trial:, 1, 2, 3");
        writer.println("PA avg rest:, " + FormatDoubleForPrint(m_aCalcPAAvgRest[0]) + ", " +
                FormatDoubleForPrint(m_aCalcPAAvgRest[1]) + ", " + FormatDoubleForPrint(m_aCalcPAAvgRest[2]));
        writer.println("HR avg (BPM) rest:, " + FormatDoubleForPrint(m_aCalcHRAvgRest[0]) + ", " +
                FormatDoubleForPrint(m_aCalcHRAvgRest[1]) + ", " + FormatDoubleForPrint(m_aCalcHRAvgRest[2]));
        writer.println("PA avg VM:, " + FormatDoubleForPrint(m_aCalcPAAvgVM[0]) + ", " +
                FormatDoubleForPrint(m_aCalcPAAvgVM[1]) + ", " + FormatDoubleForPrint(m_aCalcPAAvgVM[2]));
        writer.println("HR avg (BPM) VM:, " + FormatDoubleForPrint(m_aCalcHRAvgVM[0]) + ", " +
                FormatDoubleForPrint(m_aCalcHRAvgVM[1]) + ", " + FormatDoubleForPrint(m_aCalcHRAvgVM[2]));
        writer.println("Min PA:, " + FormatDoubleForPrint(m_aCalcMinPA[0]) + ", " +
                FormatDoubleForPrint(m_aCalcMinPA[1]) + ", " + FormatDoubleForPrint(m_aCalcMinPA[2]));
        writer.println("End PA:, " + FormatDoubleForPrint(m_aCalcEndPA[0]) + ", " +
                FormatDoubleForPrint(m_aCalcEndPA[1]) + ", " + FormatDoubleForPrint(m_aCalcEndPA[2]));
        writer.println("Min PAR:, " + FormatDoubleForPrint(m_aCalcMinPAR[0]) + ", " +
                FormatDoubleForPrint(m_aCalcMinPAR[1]) + ", " + FormatDoubleForPrint(m_aCalcMinPAR[2]));
        writer.println("End PAR:, " + FormatDoubleForPrint(m_aCalcEndPAR[0]) + ", " +
                FormatDoubleForPrint(m_aCalcEndPAR[1]) + ", " + FormatDoubleForPrint(m_aCalcEndPAR[2]));
        writer.println("Min HR:, " + FormatDoubleForPrint(m_aCalcMinHR[0]) + ", " +
                FormatDoubleForPrint(m_aCalcMinHR[1]) + ", " + FormatDoubleForPrint(m_aCalcMinHR[2]));
        writer.println("LVEDP:, " + FormatDoubleForPrint(m_aCalcLVEDP[0]) + ", " +
                FormatDoubleForPrint(m_aCalcLVEDP[1]) + ", " + FormatDoubleForPrint(m_aCalcLVEDP[2]));

        // print all of markers
        writer.println("Marker index, Type");
        for (int i = 0; i < rtd.GetDataMarkers().size(); i++)
        {
            writer.println(rtd.GetDataMarkers().get(i).dataIndex + ", " + rtd.GetDataMarkers().get(i).type);
        }

        // print all of the realtime data
        double t = 0.0;
        writer.println("Time (sec.), PPG, Pressure (mmHg)");
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
