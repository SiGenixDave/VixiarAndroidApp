package com.vixiar.indicor;

import java.util.ArrayList;

/**
 * Created by gyurk on 11/15/2017.
 */

/* scaling info
pressure sensor Vout = VS*[(0.1533*P) + 0.053]  ...  Vs = 5.0V, P is pressure in kPa
1 mmHg = 0.133322368 kPa
circuit board 0V at pressure sensor = 3V at A/D. 5V at pressure sensor = 0V at A/D
A/D 3.3V = 2048 counts

public class RealTimeData
{
    private ArrayList<PPG_PressureData> data;
    public void AppendData(byte [] data)
    {
        // extract the data...the first byte is the sequence number
        // followed by two bytes of PPG then pressure repetitively
        double pressure_value = 0.0;
        int ppg_value = 0;
        for (int i = 1; i < data.length; i += 4)
        {
            ppg_value = (256 * (int)(data[i] & 0xFF)) + (data[i+1] & 0xFF);

        }

    };
}
