package com.vixiar.indicor.Data;

/**
 * Created by gyurk on 12/13/2017.
 */

public class FIRFilter
{
    public void Initialize(PPG_FIRFilterData f)
    {
        for (int i = 0; i < f.NUM_TAPS; ++i)
        {
            f.history[i] = 0;
        }
        f.last_index = 0;
    }

    public void PutSample(PPG_FIRFilterData f, double input)
    {
        f.history[f.last_index++] = input;
        if (f.last_index == f.NUM_TAPS)
        {
            f.last_index = 0;
        }
    }

    public double GetOutput(PPG_FIRFilterData f)
    {
        double acc = 0;
        int index = f.last_index;
        for (int i = 0; i < f.NUM_TAPS; ++i)
        {
            index = index != 0 ? index - 1 : f.NUM_TAPS - 1;
            acc += f.history[index] * f.filter_taps[i];
        }
        ;
        return acc;
    }
}

