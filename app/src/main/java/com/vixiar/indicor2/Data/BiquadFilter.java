package com.vixiar.indicor2.Data;

public class BiquadFilter
{
    private double m_WHistory[] = {0.0, 0.0, 0.0};
    private final double AVALUES[] = {1, -0.848147893895537241526483285269932821393, 0};
    private final double BVALUES[] = {2, -1.999924795368799745887145036249421536922, 0};
    private final double FFGAIN = 0.151852106104462758473516714730067178607;
    private final double OUTPUTGAIN = 4.021467908271104896300585096469148993492;

    public double filter(double x)
    {
        double w = (x * AVALUES[0]) - (m_WHistory[0] * AVALUES[1]) - (m_WHistory[1] * AVALUES[2]) * FFGAIN;
        double y = (((w * BVALUES[0]) + (m_WHistory[0] * BVALUES[1]) + (m_WHistory[1] * BVALUES[2])) * OUTPUTGAIN);

        m_WHistory[1] = m_WHistory[0];
        m_WHistory[0] = w;

        return y;
    }
}
