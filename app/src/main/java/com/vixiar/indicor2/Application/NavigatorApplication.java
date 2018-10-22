package com.vixiar.indicor2.Application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Error;
import com.vixiar.indicor2.Data.PatientInfo;
import com.vixiar.indicor2.Upload_Interface.UploadServiceInterface;

/**
 * Created by gyurk on 12/19/2017.
 */

public class NavigatorApplication extends Application
{
    private static Context m_context;

    public void onCreate()
    {
        super.onCreate();
        Bugsnag.init(this);
        Bugsnag.beforeNotify(new BeforeNotify()
        {
            @Override
            public boolean run(Error error)
            {
                if (PatientInfo.getInstance().get_handheldSerialNumber() != null)
                {
                    error.addToTab("user", "Handheld SN", PatientInfo.getInstance().get_handheldSerialNumber());
                }
                else
                {
                    error.addToTab("user","Handheld SN", "N/A");
                }

                if (PatientInfo.getInstance().get_patientId() != null)
                {
                    error.addToTab("user", "Patient ID", PatientInfo.getInstance().get_patientId());
                }
                else
                {
                    error.addToTab("user", "Patient ID", "NA");
                }

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NavigatorApplication.getAppContext());
                String subFolder = sp.getString("study_location", "Vixiar_Internal-Testing");
                error.addToTab("user", "Study Location", subFolder);

                return true;
            }
        });

        m_context = getApplicationContext();

        UploadServiceInterface.getInstance().initialize(this);
    }

    public static Context getAppContext()
    {
        return m_context;
    }
}
