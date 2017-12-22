package com.vixiar.indicor.Application;

import android.app.Application;
import android.content.Context;

/**
 * Created by gyurk on 12/19/2017.
 */

public class NavigatorApplication extends Application
{
    private static Context m_context;

    public void onCreate() {
        super.onCreate();
        m_context = getApplicationContext();
    }

    public static Context getAppContext() {
        return m_context;
    }}
