package com.vixiar.indicor.BLEInterface;

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
    void iRestart();
}
