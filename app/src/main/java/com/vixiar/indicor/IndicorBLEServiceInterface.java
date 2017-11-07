package com.vixiar.indicor;

/**
 * Created by gyurk on 11/7/2017.
 */

public interface IndicorBLEServiceInterface
{
    public void BLEScanCallback();
    public void BLEConnected();
    public void BLEDisconnected();
    public void BLEServicesDiscovered();
    public void BLEDataReceived();
}
