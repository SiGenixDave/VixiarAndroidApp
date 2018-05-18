/**
 *
 * @file
 * @brief
 * @copyright Copyright 2018 Vixiar Inc.. All rights reserved.
 */

package com.vixiar.indicor.Data;

/**
 * Created by gyurk on 11/15/2017.
 */

public class PPG_PressureSample
{
    public PPG_PressureSample(int PPG, double pressure)
    {
        m_PPG = PPG;
        m_pressure = pressure;
    }
    public double m_pressure;
    public int m_PPG;
}
