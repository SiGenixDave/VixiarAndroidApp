package com.vixiar.indicor;

import android.bluetooth.le.ScanResult;

import java.util.ArrayList;

/**
 * Created by gyurk on 11/7/2017.
 */

public interface IndicorBLEServiceInterface
{
    public void iBLEScanCallback(ScanResult result);
    public void iBLEConnected();
    public void iBLEDisconnected();
    public void iBLEServicesDiscovered();
    public void iBLEDataReceived(String data);
}
