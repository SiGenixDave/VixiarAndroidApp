package com.vixiar.indicor2.BLEInterface;

/**
 * Created by gyurk on 11/6/2017.
 */

public interface IndicorBLEServiceInterfaceCallbacks
{
    void iError(int e);
    void iRealtimeDataNotification();
    void iFullyConnected();
    void iDisconnected();
    void iBatteryLevelRead(int level);
    void iLEDLevelRead(int level);
    void iRestart();
}
