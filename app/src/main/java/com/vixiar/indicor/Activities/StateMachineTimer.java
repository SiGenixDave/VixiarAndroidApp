package com.vixiar.indicor.Activities;

import android.os.Handler;

//TODO nned runnable Timers and TimerTasks are prone to memory leaks
public class StateMachineTimer
{
    private Handler handler;
    TimerCallback stateMachine;
    int timerId;
    boolean oneShot;
    int periodMs;
    private boolean isRunning = false;

    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            if (oneShot)
            {
                isRunning = false;
            }
            else
            {
                handler.postDelayed (runnable, periodMs);
            }
            stateMachine.TimerExpired(timerId);
        }
    };

    public StateMachineTimer(final int timerId) {
        this.timerId = timerId;
        this.handler = new Handler();
    }

    public void Cancel () {
        handler.removeCallbacks (this.runnable);
        isRunning = false;
    }

    public void Start (TimerCallback stateMachine, int periodMs, boolean oneShot) {
        this.stateMachine = stateMachine;
        this.oneShot = oneShot;
        this.periodMs = periodMs;
        handler.postDelayed (this.runnable, periodMs);
        isRunning = true;
    }

    public Boolean IsRunning() {
        return isRunning;
    }

    public void SetIsRunning(Boolean value) {
        isRunning = value;
    }
}
