package com.vixiar.indicor2.Application;

import android.app.Application;
import android.content.Context;

import com.vixiar.indicor2.Upload_Interface.UploadServiceInterface;

/**
 * Created by gyurk on 12/19/2017.
 */

public class NavigatorApplication extends Application
{
    private static Context m_context;

    public void onCreate() {
        super.onCreate();
        m_context = getApplicationContext();

        UploadServiceInterface.getInstance().initialize(this);
    }

    public static Context getAppContext() {
        return m_context;
    }}
