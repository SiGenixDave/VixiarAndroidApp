/**
 * The generic timer can be used to create periodic or one-time events.  When the timer expires
 * a user provided callback is called.
 * @file GenericTimer.java
 * @brief Defines the GenericTimer class
 * @copyright Copyright 2018 Vixiar Inc.. All rights reserved.
 */

package com.vixiar.indicor.Activities;

import android.os.Handler;

/**
 * The GenericTimer class can be used to create periodic or one-time events.  When the timer expires
 * a user provided callback is called.
 */
public class GenericTimer
{

    private Handler m_handler;              ///< Used to keep track of the timer handler
    private TimerCallback m_callback;       ///< Saves the callback function that the user provided
    private int m_timerId;                  ///< Saves the ID that was given by the user when the timer was created
    private boolean m_oneShot;              ///< Saves the type of timer
    private int m_periodMs;                 ///< Ssves the period
    private boolean m_bIsRunning = false;   ///< State variable

    private Runnable m_runnable = new Runnable()    ///< The Runnable does all the work
    {

        @Override
        public void run()
        {
            if (m_oneShot)
            {
                m_bIsRunning = false;
            }
            else
            {
                m_handler.postDelayed(m_runnable, m_periodMs);
            }
            m_callback.TimerExpired(m_timerId);
        }
    };

    /**
     * Constructor for the class
     * @param timerId Use this as a reference to know what timer the callback is coming from
     */
    public GenericTimer(final int timerId)
    {
        this.m_timerId = timerId;
        this.m_handler = new Handler();
    }

    /**
     * Cancel any pending events for this timer
     */
    public void Cancel()
    {
        m_handler.removeCallbacks(this.m_runnable);
        m_bIsRunning = false;
    }

    /**
     * Reset and restart the timer
     */
    public void Reset()
    {
        m_handler.removeCallbacks(this.m_runnable);
        m_handler.postDelayed(this.m_runnable, m_periodMs);
        m_bIsRunning = true;
    }

    /**
     * Start the timer
     * @param callback Callback function
     * @param periodMs ms for the timer expiration
     * @param oneShot True to only activate once; otherwise will expire every periodMs time
     */
    public void Start(TimerCallback callback, int periodMs, boolean oneShot)
    {
        this.m_callback = callback;
        this.m_oneShot = oneShot;
        this.m_periodMs = periodMs;
        m_handler.postDelayed(this.m_runnable, periodMs);
        m_bIsRunning = true;
    }

    /**
     * Determine if the timer is running
     * @return True if it is running
     */
    public Boolean IsRunning()
    {
        return m_bIsRunning;
    }
}
