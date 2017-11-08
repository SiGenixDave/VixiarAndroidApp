package com.vixiar.indicor;

import android.bluetooth.le.ScanResult;

/**
 * Created by gyurk on 11/7/2017.
 */

public interface IndicorBLEServiceInterface
{
    public void iBLEScanCallback(ScanResult result);
    public void iBLEConnected();
    public void iBLEDisconnected();
    public void iBLEServicesDiscovered();
    public void iBLEDataReceived(byte[] data);
}
