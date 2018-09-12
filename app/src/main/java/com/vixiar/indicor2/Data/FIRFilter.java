package com.vixiar.indicor2.Data;

/**
 * Created by gyurk on 12/13/2017.
 */


public class FIRFilter
{

    private final int m_NumTaps;
    private final double m_FilterTaps[];
    private double[] m_History;
    private int m_LastIndex;


    public FIRFilter(final double[] filter_taps)
    {
        super();
        m_FilterTaps = filter_taps;
        m_NumTaps = m_FilterTaps.length;
        m_History = new double[m_NumTaps];
    }

    public void Initialize()
    {
        for (int i = 0; i < m_NumTaps; ++i)
        {
            m_History[i] = 0;
        }
        m_LastIndex = 0;
    }

    public void PutSample(double input)
    {
        m_History[m_LastIndex++] = input;
        if (m_LastIndex == m_NumTaps)
        {
            m_LastIndex = 0;
        }
    }

    public double GetOutput()
    {
        double acc = 0;
        int index = m_LastIndex;
        for (int i = 0; i < m_NumTaps; ++i)
        {
            index = index != 0 ? index - 1 : m_NumTaps - 1;
            acc += m_History[index] * m_FilterTaps[i];
        }
        return acc;
    }
}
