package com.vixiar.indicor.Data;

/**
 * Created by gyurk on 12/13/2017.
 */

/*

FIR filter designed with
 http://t-filter.appspot.com

sampling frequency: 50 Hz

* 0 Hz - 5 Hz
  gain = 1
  desired ripple = 2 dB
  actual ripple = 1.3333794968150365 dB

* 10 Hz - 25 Hz
  gain = 0
  desired attenuation = -40 dB
  actual attenuation = -41.24374481860345 dB

*/

public class Pressure_FIRFilterTaps implements I_FIRFilterTap
{
    double filter_taps[] = {
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            1.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0};

    @Override
    public double[] GetTaps()
    {
        return filter_taps;
    }
}
