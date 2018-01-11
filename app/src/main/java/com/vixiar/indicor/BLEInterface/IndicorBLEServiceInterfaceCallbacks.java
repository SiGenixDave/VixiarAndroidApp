package com.vixiar.indicor.BLEInterface;

/**
 * Created by gyurk on 11/6/2017.
 */

public interface IndicorBLEServiceInterfaceCallbacks
{
    public void iError(int e);
    public void iRealtimeDataNotification();
    public void iFullyConnected();
    public void iDisconnected();
    public void iBatteryLevelRead(int level);
    public void iRestart();
}
