package com.vixiar.indicor2.Data;

import java.util.ArrayList;

public class DataMath
{
    private static final DataMath ourInstance = new DataMath();

    public static DataMath getInstance()
    {
        return ourInstance;
    }

    public double CalculateMean(int startIndex, int endIndex, ArrayList<RealtimeDataSample> dataSet)
    {
        int dataSum = 0;
        for (int i = startIndex; i < endIndex; i++)
        {
            dataSum += dataSet.get(i).m_PPG;
        }
        return dataSum / (endIndex - startIndex);
    }

    public double CalculateStdev(int startIndex, int endIndex, ArrayList<RealtimeDataSample> dataSet)
    {
        double diffSumSquared = 0.0;
        double mean = CalculateMean(startIndex, endIndex, dataSet);

        for (int i = startIndex; i < endIndex; i++)
        {
            diffSumSquared += Math.pow((dataSet.get(i).m_PPG - mean), 2);
        }
        return Math.sqrt(diffSumSquared / (endIndex - startIndex));
    }
}
