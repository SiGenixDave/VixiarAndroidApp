package com.vixiar.indicor.Data;

import java.util.Date;

/**
 * Created by Dave on 7/12/2017.
 */

public class PatientInfo
{
    private static final PatientInfo ourInstance = new PatientInfo();
    private String PatientId;
    private int systolicBloodPressure;
    private int diastolicBloodPressure;
    private int height_Inches;
    private int weight_lbs;
    private int age_years;
    private String testDate;
    private String gender;
    private String notes;
    private RealtimeData rtd = new RealtimeData();

    public String getTestDate()
    {
        return testDate;
    }

    public void setTestDate(String testDate)
    {
        this.testDate = testDate;
    }

    public static PatientInfo getInstance()
    {
        return ourInstance;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    private PatientInfo()
    {
    }

    public RealtimeData GetRealtimeData()
    {
        return rtd;
    }

    public String getPatientId()
    {
        return PatientId;
    }

    public void setPatientId(String patientId)
    {
        this.PatientId = patientId;
    }

    public int getSystolicBloodPressure()
    {
        return systolicBloodPressure;
    }

    public void setSystolicBloodPressure(int systolicBloodPressure)
    {
        this.systolicBloodPressure = systolicBloodPressure;
    }

    public int getDiastolicBloodPressure()
    {
        return diastolicBloodPressure;
    }

    public void setDiastolicBloodPressure(int diastolicBloodPressure)
    {
        this.diastolicBloodPressure = diastolicBloodPressure;
    }

    public int getHeight_Inches()
    {
        return height_Inches;
    }

    public void setHeight_Inches(int height_Inches)
    {
        this.height_Inches = height_Inches;
    }

    public int getWeight_lbs()
    {
        return weight_lbs;
    }

    public void setWeight_lbs(int weight_lbs)
    {
        this.weight_lbs = weight_lbs;
    }

    public int getAge_years()
    {
        return age_years;
    }

    public void setAge_years(int age_years)
    {
        this.age_years = age_years;
    }

}
