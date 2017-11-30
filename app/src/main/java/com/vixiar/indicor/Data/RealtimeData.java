package com.vixiar.indicor.Data;

import java.util.ArrayList;

/**
 * Created by gyurk on 11/15/2017.
 */

/* scaling info
pressure sensor Vout = VS*[(0.1533*P) + 0.053]  ...  Vs = 5.0V, P is pressure in kPa
1 kPa = 0.133322368 mmHg
circuit board 0V at pressure sensor = 3V at A/D. 5V at pressure sensor = 0V at A/D
A/D 3.3V = 2048 counts
doing the math, p(mmHg) = (-0.0263 * counts) + 46.335

*/
public class RealtimeData
{
    private ArrayList<PPG_PressureSample> data = new ArrayList<PPG_PressureSample>();
    private ArrayList<RealtimeDataMarker> markers = new ArrayList<RealtimeDataMarker>();

    public void AppendNewSample(byte[] new_data)
    {
        // extract the data...the first byte is the sequence number
        // followed by two bytes of PPG then pressure repetitively
        double pressure_value = 0.0;
        int pressure_counts = 0;
        int ppg_value = 0;
        for (int i = 1; i < new_data.length; i += 4)
        {
            // convert the a/d counts from the handheld to pressure in mmHg
            pressure_counts = (256 * (int) (new_data[i + 2] & 0xFF)) + (new_data[i + 3] & 0xFF);
            pressure_value = ((double) pressure_counts * (-0.0263)) + 46.726;
            if (pressure_value < 0.0)
            {
                pressure_value = 0.0;
            }

            ppg_value = (256 * (int) (new_data[i] & 0xFF)) + (new_data[i + 1] & 0xFF);

            PPG_PressureSample pd = new PPG_PressureSample(ppg_value, pressure_value);
            data.add(pd);
        }
    }

    public void CreateMarker(RealtimeDataMarker.Marker_Type type, int index)
    {
        RealtimeDataMarker marker = new RealtimeDataMarker(type, index);
        markers.add(marker);
    }

    public ArrayList<PPG_PressureSample> GetData()
    {
        return data;
    }

    public void ClearAllSamples()
    {
        data.clear();
    }
}
