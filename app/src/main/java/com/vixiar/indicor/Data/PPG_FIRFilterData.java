/**
 * @file PPG_FIRFilterData.java
 * @brief Contains the coefficients for the FIR filter for filtering the PPG signal
 * @copyright Copyright 2018 Vixiar Inc.. All rights reserved.
 */

package com.vixiar.indicor.Data;


/**
 * FIR filter designed with
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
public class PPG_FIRFilterData
{
    public final int NUM_TAPS = 15;
    double[] history = new double[NUM_TAPS];
    int last_index;
    double filter_taps[] = {
            -0.011720065131297001,
            -0.03498447461368515,
            -0.04377957852197897,
            -0.023945924515193045,
            0.04660636916904729,
            0.14804717183574886,
            0.24190846602550148,
            0.27913095590918874,
            0.24190846602550148,
            0.14804717183574886,
            0.04660636916904729,
            -0.023945924515193045,
            -0.04377957852197897,
            -0.03498447461368515,
            -0.011720065131297001
    };
}
