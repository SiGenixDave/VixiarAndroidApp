package com.vixiar.indicor.Activities;

import android.os.Handler;

public class GenericTimer
{
    private final Handler handler;
    private TimerCallback m_callback;
    private final int timerId;
    private boolean oneShot;
    private int periodMs;
    private boolean isRunning = false;

    private final Runnable runnable = new Runnable()
    {

        @Override
        public void run()
        {
            if (oneShot)
            {
                isRunning = false;
            }
            else
            {
                handler.postDelayed(runnable, periodMs);
            }
            m_callback.TimerExpired(timerId);
        }
    };

    public GenericTimer(final int timerId)
    {
        this.timerId = timerId;
        this.handler = new Handler();
    }

    public void Cancel()
    {
        handler.removeCallbacks(this.runnable);
        isRunning = false;
    }

    public void Reset()
    {
        handler.removeCallbacks(this.runnable);
        handler.postDelayed(this.runnable, periodMs);
        isRunning = true;
    }

    public void Start(TimerCallback callback, int periodMs, boolean oneShot)
    {
        this.m_callback = callback;
        this.oneShot = oneShot;
        this.periodMs = periodMs;
        handler.postDelayed(this.runnable, periodMs);
        isRunning = true;
    }

    public Boolean IsRunning()
    {
        return isRunning;
    }

    public void SetIsRunning(Boolean value)
    {
        isRunning = value;
    }
}
