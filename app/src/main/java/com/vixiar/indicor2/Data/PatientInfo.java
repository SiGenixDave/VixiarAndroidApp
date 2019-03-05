package com.vixiar.indicor2.Data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.vixiar.indicor2.BLEInterface.IndicorBLEService;
import com.vixiar.indicor2.BLEInterface.IndicorBLEServiceInterface;
import com.vixiar.indicor2.BuildConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import static com.vixiar.indicor2.Data.AppConstants.LENGTH_OF_RESULTS_GRAPH;
import static com.vixiar.indicor2.Data.AppConstants.SAMPLES_PER_SECOND;
import static com.vixiar.indicor2.Data.AppConstants.SECONDS_BEFORE_T0_FOR_RESULTS_GRAPH;

/**
 * Created by Dave on 7/12/2017.
 */

public class PatientInfo
{
    private final static String TAG = PatientInfo.class.getSimpleName();

    private static final PatientInfo ourInstance = new PatientInfo();
    // Constants
    private static int SAMPLES_IN_TEN_SECONDS = (50 * 10);
    // calculated data
    private final int NUM_TESTS = 3;
    private String m_patientId;
    private int m_systolicBloodPressure;
    private int m_diastolicBloodPressure;
    private int m_height_Inches;
    private int m_weight_lbs;
    private int m_age_years;
    private int m_startTestBatteryLevel;
    private String m_applicationVersion;
    private String m_studyLocation;
    private String m_handheldSerialNumber;
    private String m_firmwareVersion;
    private String m_testDateTime;
    private String m_gender;
    private String m_notes;
    private RealtimeData rtd = new RealtimeData();
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
    private double[] m_aFileMinPAR = new double[NUM_TESTS];
    private int[] m_aLEDDriveLevels = new int[NUM_TESTS];

    public static PatientInfo getInstance()
    {
        return ourInstance;
    }

    public void Initialize()
    {
        m_patientId = "";
        m_systolicBloodPressure = 0;
        m_diastolicBloodPressure = 0;
        m_height_Inches = 0;
        m_weight_lbs = 0;
        m_age_years = 0;
        m_firmwareVersion = "";
        m_testDateTime = "";
        m_gender = "";
        m_notes = "";
        m_startTestBatteryLevel = 0;
        Arrays.fill(m_aCalcLVEDP, 0.0);
        Arrays.fill(m_aCalcMinHRVM, 0.0);
        Arrays.fill(m_aCalcMinPA, 0.0);
        Arrays.fill(m_aCalcMinPAR, 0.0);
        Arrays.fill(m_aCalcPAAvgRest, 0.0);
        Arrays.fill(m_aCalcPAAvgVM, 0.0);
        rtd.Initialize();
    }

    public void set_startTestBatteryLevel(int level)
    {
        this.m_startTestBatteryLevel = level;
    }

    public String get_studyLocation()
    {
        return m_studyLocation;
    }

    public void set_studyLocation(String location)
    {
        this.m_studyLocation = location;
    }

    public String get_applicationVersion()
    {
        return m_applicationVersion;
    }

    public void set_applicationVersion(String m_applicationVersion)
    {
        this.m_applicationVersion = m_applicationVersion;
    }

    public String get_firmwareRevision()
    {
        return m_firmwareVersion;
    }

    public void set_firmwareRevision(String m_firmwareVersion)
    {
        this.m_firmwareVersion = m_firmwareVersion;
    }

    public int[] get_LEDDriveLevels()
    {
        return m_aLEDDriveLevels;
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

    public String get_testDateTime()
    {
        return m_testDateTime;
    }

    public void set_testDateTime(String m_testDateTime)
    {
        this.m_testDateTime = m_testDateTime;
    }

    public String get_handheldSerialNumber()
    {
        return m_handheldSerialNumber;
    }

    public void set_handheldSerialNumber(String m_handheldSerialNumber)
    {
        this.m_handheldSerialNumber = m_handheldSerialNumber;
    }

    public String get_gender()
    {
        return m_gender;
    }

    public void set_gender(String m_gender)
    {
        this.m_gender = m_gender;
    }

    public String get_notes()
    {
        return m_notes;
    }

    public void set_notes(String m_notes)
    {
        this.m_notes = m_notes;

        // parse the notes for the LED drive levels
        String noWhitespaceNotes = m_notes.replaceAll("\\s","");
        String values[] = noWhitespaceNotes.split(",");

        for (int i = 0; i < NUM_TESTS; i++)
        {
            int value;
            try
            {
                value = Integer.parseInt(values[i]);
                if (value < 0)
                {
                    value = 0;
                }
                if (value > 100)
                {
                    value = 100;
                }
            }
            catch (Exception e)
            {
                value = 0;
            }
            m_aLEDDriveLevels[i] = value;
        }
    }

    public RealtimeData getRealtimeData()
    {
        return rtd;
    }

    public void set_fileMinPAR(int testNumber, double minPAR)
    {
        if (testNumber < NUM_TESTS)
        {
            m_aFileMinPAR[testNumber] = minPAR;
        }
    }

    public double get_fileMinPAR(int testNumber)
    {
        if (testNumber < NUM_TESTS)
        {
            return m_aFileMinPAR[testNumber];
        }
        else
        {
            return 0.0;
        }
    }

    public String get_patientId()
    {
        return m_patientId;
    }

    public void set_patientId(String m_patientId)
    {
        this.m_patientId = m_patientId;
    }

    public int get_systolicBloodPressure()
    {
        return m_systolicBloodPressure;
    }

    public void set_systolicBloodPressure(int m_systolicBloodPressure)
    {
        this.m_systolicBloodPressure = m_systolicBloodPressure;
    }

    public int get_diastolicBloodPressure()
    {
        return m_diastolicBloodPressure;
    }

    public void set_diastolicBloodPressure(int m_diastolicBloodPressure)
    {
        this.m_diastolicBloodPressure = m_diastolicBloodPressure;
    }

    public int get_height_Inches()
    {
        return m_height_Inches;
    }

    public void set_height_Inches(int m_height_Inches)
    {
        this.m_height_Inches = m_height_Inches;
    }

    public int get_weight_lbs()
    {
        return m_weight_lbs;
    }

    public void set_weight_lbs(int m_weight_lbs)
    {
        this.m_weight_lbs = m_weight_lbs;
    }

    public int get_age_years()
    {
        return m_age_years;
    }

    public void set_age_years(int m_age_years)
    {
        this.m_age_years = m_age_years;
    }

    // test number is 0 relative
    public boolean CalculateResults(int testNumber)
    {
        // run Harry's peak detection on the highpass/lowpass filtered data
        PeaksAndValleys pv = PostPeakValleyDetect.getInstance().HarrySilberPeakDetection(testNumber, PatientInfo.getInstance().getRealtimeData().GetHPLPFilteredData(), false);

        // pv contains the peak and valley locations in the filtered data
        // the peaks and valleys don't necessarily line up with the raw data peaks and valleys
        // this function will search around the potential peak and valley and find the true one

        PeaksAndValleys pvFIR = PostPeakValleyDetect.getInstance().TransferPeaksAndValleysToOtherData(pv, PatientInfo.getInstance().getRealtimeData().GetLPFilteredData());

        PostProcessing.getInstance().CalculatePostProcessingResults(testNumber, pvFIR, PatientInfo.getInstance().getRealtimeData().GetLPFilteredData(), false);

        m_aCalcLVEDP[testNumber] = PostProcessing.getInstance().getLVEDP(testNumber, m_height_Inches, m_diastolicBloodPressure, m_systolicBloodPressure, m_age_years);

        return true;
    }

    // returns an array of PPG and pressure values for 30 seconds wih the center being the
    // start of the Valsalva maneuver
    // testNumber is 0 relative
    public ArrayList<RealtimeDataSample> GetSummaryChartData(int testNumber)
    {
        ArrayList<RealtimeDataSample> results = new ArrayList<>();

        TestMarkers tm = GetTestMarkers(testNumber);

        int startIndex = tm.startIndex;

        // make sure there's enough data before the start of the test index

        if (startIndex <= SAMPLES_PER_SECOND * SECONDS_BEFORE_T0_FOR_RESULTS_GRAPH)
        {
            startIndex = 0;
        }
        else
        {
            startIndex -= (SAMPLES_PER_SECOND * SECONDS_BEFORE_T0_FOR_RESULTS_GRAPH);
        }

        int endIndex = startIndex + (SAMPLES_PER_SECOND * LENGTH_OF_RESULTS_GRAPH);

        if (endIndex >= PatientInfo.getInstance().getRealtimeData().GetLPFilteredData().size())
        {
            endIndex = PatientInfo.getInstance().getRealtimeData().GetLPFilteredData().size();
        }

        for (int i = startIndex; i < endIndex; i++)
        {
            RealtimeDataSample data;
            data = PatientInfo.getInstance().getRealtimeData().GetLPFilteredData().get(i);
            results.add(data);
        }

        return results;
    }


    // get the start and end of valsalva markers for a certain test
    // both markers will be 0 if the test is not found
    public TestMarkers GetTestMarkers(int testNumber)
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

    public boolean SaveCSVFileAndroid(Context context)
    {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = m_patientId + "-" + m_testDateTime + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File file = new File(filePath);

        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            WriteCSVContentsAndroid(pw);
            file.setWritable(true);
            pw.flush();
            pw.close();
            fos.close();

            // now we need to force android to rescan the file system so the file will show up
            // if you want to load it via usb
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you" + " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    private boolean WriteCSVContentsAndroid(PrintWriter writer)
    {
        writer.println("Test date time, " + m_testDateTime);
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

        // calculate some stuff for BMI
        double height_m = m_height_Inches * 0.0254;
        double weight_kg = m_weight_lbs * 0.4536;
        double bmi = (weight_kg / (height_m * height_m));
        writer.println("BMI (Kg/m^2):, " + FormatDoubleForPrint(bmi));

        writer.println("Notes:, " + m_notes);

        // print all of the calculated data
        writer.println("Calculated values-Trial, 1, 2, 3");
        writer.println("BL PA - avg, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getBLPA_Avg(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getBLPA_Avg(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getBLPA_Avg(2)));
        writer.println("BL HR - avg, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getBLHR_Avg(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getBLHR_Avg(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getBLHR_Avg(2)));
        writer.println("Ph2 PA - avg, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2PA_Avg(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2PA_Avg(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2PA_Avg(2)));
        writer.println("Ph2 HR - avg, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2HR_Avg(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2HR_Avg(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2HR_Avg(2)));
        writer.println("Min PA - peak, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getMinPA_Peak(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getMinPA_Peak(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getMinPA_Peak(2)));
        writer.println("End PA - peak, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getEndPA_Peak(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getEndPA_Peak(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getEndPA_Peak(2)));
        writer.println("Min PAR - peak values - BL, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getMinPAR_PV_BL(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getMinPAR_PV_BL(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getMinPAR_PV_BL(2)));
        writer.println("End PAR - peak values - BL, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getEndPAR_PV_BL(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getEndPAR_PV_BL(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getEndPAR_PV_BL(2)));
        writer.println("Ph2 HR - min, " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2HR_min(0)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2HR_min(1)) + ", " +
                FormatDoubleForPrint(PostProcessing.getInstance().getPh2HR_min(2)));
        writer.println("LVEDP, " +
                FormatDoubleForPrint(m_aCalcLVEDP[0]) + ", " +
                FormatDoubleForPrint(m_aCalcLVEDP[1]) + ", " +
                FormatDoubleForPrint(m_aCalcLVEDP[2]));

        // print the LED level for each test
        // if the levels are 0, they weren't set, so print out the last level, which may be the default
        int [] aActualLEDLevel = new int[3];
        if (m_aLEDDriveLevels[0] == 0)
        {
            aActualLEDLevel[0] = 50;
        }
        else
        {
            aActualLEDLevel[0] = m_aLEDDriveLevels[0];
        }
        if (m_aLEDDriveLevels[1] == 0)
        {
            aActualLEDLevel[1] = aActualLEDLevel[0];
        }
        else
        {
            aActualLEDLevel[1] = m_aLEDDriveLevels[1];
        }
        if (m_aLEDDriveLevels[2] == 0)
        {
            aActualLEDLevel[2] = aActualLEDLevel[1];
        }
        else
        {
            aActualLEDLevel[2] = m_aLEDDriveLevels[2];
        }
        writer.println("PPG LED Level, " +
                FormatDoubleForPrint(aActualLEDLevel[0]) + "%, " +
                FormatDoubleForPrint(aActualLEDLevel[1]) + "%, " +
                FormatDoubleForPrint(aActualLEDLevel[2]) + "%");

        writer.println("Battery Level, " + IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel() + "%");

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
            writer.println(FormatDoubleForPrint(t) + ", " + rtd.GetRawData().get(i).m_PPG + ", " + FormatDoubleForPrint(rtd.GetRawData().get(i).m_pressure));
            t += 0.02;
        }

        // print all of the 5Hz and high-pass filtered realtime data
        t = 0.0;
        writer.println("Time (sec.), PPG (5Hz filter), Pressure (mmHg)");
        for (int i = 0; i < rtd.GetLPFilteredData().size(); i++)
        {
            writer.println(FormatDoubleForPrint(t) + ", " + rtd.GetLPFilteredData().get(i).m_PPG + ", " + FormatDoubleForPrint(rtd.GetLPFilteredData().get(i).m_pressure));
            t += 0.02;
        }

        // print all of the 5Hz and high-pass filtered realtime data
        t = 0.0;
        writer.println("Time (sec.), PPG (5Hz FIR + highpass filter), Pressure (mmHg)");
        for (int i = 0; i < rtd.GetHPLPFilteredData().size(); i++)
        {
            writer.println(FormatDoubleForPrint(t) + ", " + rtd.GetHPLPFilteredData().get(i).m_PPG + ", " + FormatDoubleForPrint(rtd.GetHPLPFilteredData().get(i).m_pressure));
            t += 0.02;
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
