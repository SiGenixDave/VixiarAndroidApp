package com.example.sigenix.indicor;

/**
 * Created by Dave on 7/12/2017.
 */

class PatientInfo {
    private static final PatientInfo ourInstance = new PatientInfo();

    static PatientInfo getInstance() {
        return ourInstance;
    }

    private PatientInfo() {
    }

    public String getSubjectInfo() {
        return subjectInfo;
    }

    public void setSubjectInfo(String subjectInfo) {
        this.subjectInfo = subjectInfo;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSystolicBloodPressure() {
        return systolicBloodPressure;
    }

    public void setSystolicBloodPressure(String systolicBloodPressure) {
        this.systolicBloodPressure = systolicBloodPressure;
    }

    public String getDiastolicBloodPressure() {
        return diastolicBloodPressure;
    }

    public void setDiastolicBloodPressure(String diastolicBloodPressure) {
        this.diastolicBloodPressure = diastolicBloodPressure;
    }

    public String getHeightFeet() {
        return heightFeet;
    }

    public void setHeightFeet(String heightFeet) {
        this.heightFeet = heightFeet;
    }

    public String getHeightInches() {
        return heightInches;
    }

    public void setHeightInches(String heightInches) {
        this.heightInches = heightInches;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getBirthdateMonth() {
        return birthdateMonth;
    }

    public void setBirthdateMonth(String birthdateMonth) {
        this.birthdateMonth = birthdateMonth;
    }

    public String getBirthdateDay() {
        return birthdateDay;
    }

    public void setBirthdateDay(String birthdateDay) {
        this.birthdateDay = birthdateDay;
    }

    public String getBirthdateYear() {
        return birthdateYear;
    }

    public void setBirthdateYear(String birthdateYear) {
        this.birthdateYear = birthdateYear;
    }

    private String subjectInfo;
    private String subjectId;
    private String systolicBloodPressure;
    private String diastolicBloodPressure;
    private String heightFeet;
    private String heightInches;
    private String weight;
    private String birthdateMonth;
    private String birthdateDay;
    private String birthdateYear;
}
