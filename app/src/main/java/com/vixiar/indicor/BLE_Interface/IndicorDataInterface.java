package com.vixiar.indicor.BLE_Interface;

/**
 * Created by gyurk on 11/6/2017.
 */

public interface IndicorDataInterface
{
    public void iError(int e);
    public void iNotify();
    public void iConnected();
    public void iCharacteristicRead(Object o);
}
