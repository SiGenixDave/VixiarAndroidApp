package com.vixiar.indicor2.Data;

/**
 * Created by gyurk on 11/15/2017.
 */

public class RealtimeDataSample
{
    public double m_pressure;
    public int m_PPG;
    public RealtimeDataSample(int PPG, double pressure)
    {
        m_PPG = PPG;
        m_pressure = pressure;
    }
}
