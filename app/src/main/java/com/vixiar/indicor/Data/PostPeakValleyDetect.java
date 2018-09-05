package com.vixiar.indicor.Data;

import java.util.ArrayList;

import static com.vixiar.indicor.Data.TestConstants.SAMPLES_PER_SECOND;

public class PostPeakValleyDetect
{
    // Used to maintain a single instance of the com.vixiar.desktoptool.RealtimePeakValleyDetect class
    private static final PostPeakValleyDetect ourInstance = new PostPeakValleyDetect();

    // //////////////////////////////////////////////////////////////////////////
    // / Constructors
    // //////////////////////////////////////////////////////////////////////////
    public static PostPeakValleyDetect getInstance()
    {
        return ourInstance;
    }

    private final double BASELINE_SCALE_FACTOR = 0.7;
    private final double VALSALVA_SCALE_FACTOR = 0.8;
    private final double POST_VALSALVA_SCALE_FACTOR = 1.0;

    // this is a peak detection method from a paper titled:
    // A Robust Algorithm for Real-Time Peak
    // Detection of Photoplethysmograms using
    // a Personal Computer Mouse
    // by: Thang Viet Tran, Student member, IEEE, and Wan-Young Chung, Member, IEEE
    // This implementation is not quite working yet
    public PeaksAndValleys ThangChungPeakDetection()
    {
        double dADT;
        double K = 1000.0;
        int lastMaxValue = 0;
        int lastMaxIndex = 0;
        int lastMinValue = 0;
        int lastMinIndex = 0;
        double currentHeartRate = 0.0;
        int SAMPLING_FREQUENCY = 50;
        ePostPeakDetection ePPDSTate = ePostPeakDetection.LOOKING_FOR_PEAK;
        PeaksAndValleys thangChungPeaksAndValleys = new PeaksAndValleys();
        thangChungPeaksAndValleys.peaks = new ArrayList<>();
        thangChungPeaksAndValleys.valleys = new ArrayList<>();

        int currentIndex = 0;

        while (currentIndex < PatientInfo.getInstance().getRealtimeData().GetFilteredData().size())
        {
            int currentSample = PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(currentIndex).m_PPG;
            dADT = UpdateADT(currentHeartRate, currentIndex, lastMaxIndex, lastMinIndex, SAMPLING_FREQUENCY, K);

            switch (ePPDSTate)
            {
                case LOOKING_FOR_PEAK:
                    if (currentSample > lastMaxValue)
                    {
                        lastMaxValue = currentSample;
                        lastMaxIndex = currentIndex;
                    }
                    int dist = lastMaxValue - currentSample;
                    if (dist > dADT)
                    {
                        // this is a new peak
                        thangChungPeaksAndValleys.peaks.add(currentIndex);
                        System.out.println("Peak @ " + currentIndex);
                        ePPDSTate = ePostPeakDetection.LOOKING_FOR_VALLEY;
                        currentHeartRate = UpdateHeartRate(thangChungPeaksAndValleys, SAMPLING_FREQUENCY);
                    }
                    break;

                case LOOKING_FOR_VALLEY:
                    if (currentSample < lastMinValue)
                    {
                        lastMinValue = currentSample;
                        lastMinIndex = currentIndex;
                    }
                    dist = lastMinValue + currentSample;
                    if (dist < dADT)
                    {
                        // this is a new valley
                        System.out.println("Valley @ " + currentIndex);
                        thangChungPeaksAndValleys.valleys.add(currentIndex);
                        ePPDSTate = ePostPeakDetection.LOOKING_FOR_PEAK;
                        K = 0.8 * (lastMaxValue - lastMinValue);
                        lastMaxValue = Integer.MIN_VALUE;
                        lastMinValue = Integer.MAX_VALUE;
                    }
                    break;
            }
            currentIndex++;
        }
        return thangChungPeaksAndValleys;
    }

    private double UpdateHeartRate(PeaksAndValleys pv, int samplingFrequency)
    {
        double currentHeartRate;
        if (pv.peaks.size() < 2)
        {
            currentHeartRate = 0.0;
        }
        else
        {
            double t = pv.peaks.get(pv.peaks.size() - 1) - pv.peaks.get(pv.peaks.size() - 2);
            t /= samplingFrequency;
            currentHeartRate = 60.0 / t;
        }
        return currentHeartRate;
    }

    private double UpdateADT(double currentHeartRate, int currentSampleIndex, int lastMaxIndex, int lastMinIndex, int samplingFrequency, double K)
    {
        // figure out which was the last extreme point
        int distanceFromLast;
        if (lastMaxIndex > lastMinIndex)
        {
            distanceFromLast = currentSampleIndex - lastMaxIndex;
        }
        else
        {
            distanceFromLast = currentSampleIndex - lastMinIndex;
        }

        return (K * (1 - ((distanceFromLast * currentHeartRate) / (30 * samplingFrequency))));
    }


    // This is the peak detection algorithm by Dr. Harry Silber which he implemented in an excel spreadsheet.
    // This code mimics the algorithm from the spreadsheet : Auto-Analyzer-2018-08-04.xlsx
    public PeaksAndValleys HarrySilberPeakDetection(int testNumber, ArrayList<RealtimeDataSample> dataSet)
    {
        PeaksAndValleys pvBaseline, pvValsalva, pvPostValsalva;
        PeaksAndValleys allPV = new PeaksAndValleys();
        allPV.peaks = new ArrayList<>();
        allPV.valleys = new ArrayList<>();

        // Find the baseline peaks and valleys
        int baselineStart = PostProcessing.getInstance().FindBaselineStart(testNumber, dataSet);
        int baselineEnd = PostProcessing.getInstance().FindBaselineEnd(testNumber, dataSet);
        pvBaseline = DetectPeaksAndValleysForRegionHarryMethod(baselineStart, baselineEnd, BASELINE_SCALE_FACTOR, dataSet, eHarryPeakDetectionType.BASELINE);

        // Find the Valsalva peaks and valleys
        int vStart = PostProcessing.getInstance().FindVStart(testNumber, dataSet);
        int vEnd = PostProcessing.getInstance().FindVEnd(testNumber, dataSet);
        pvValsalva = DetectPeaksAndValleysForRegionHarryMethod(vStart, vEnd, VALSALVA_SCALE_FACTOR, dataSet, eHarryPeakDetectionType.NON_BASELINE);

        // Find the post Valsalva peaks and valleys
        int postVMStart = vEnd + (int)(SAMPLES_PER_SECOND * 2.5);
        int postVMEnd = postVMStart + (SAMPLES_PER_SECOND * 10);
        pvPostValsalva = DetectPeaksAndValleysForRegionHarryMethod(postVMStart, postVMEnd, POST_VALSALVA_SCALE_FACTOR, dataSet, eHarryPeakDetectionType.NON_BASELINE);

        allPV.peaks.addAll(pvBaseline.peaks);
        allPV.valleys.addAll(pvBaseline.valleys);
        allPV.peaks.addAll(pvValsalva.peaks);
        allPV.valleys.addAll(pvValsalva.valleys);
        allPV.peaks.addAll(pvPostValsalva.peaks);
        allPV.valleys.addAll(pvPostValsalva.valleys);

        return allPV;
    }

    private PeaksAndValleys DetectPeaksAndValleysForRegionHarryMethod(int startIndex, int endIndex, double scaleFactor, ArrayList<RealtimeDataSample> dataSet, eHarryPeakDetectionType detectionType)
    {
        PeaksAndValleys pv = new PeaksAndValleys();
        pv.peaks = new ArrayList<>();
        pv.valleys = new ArrayList<>();
        int currentIndex;
        int currentSample = 0;
        double mean = 0.0;
        double stdev = 0.0;
        ArrayList<ValueAndLocation> potentialPeaks = new ArrayList<>();

        if (detectionType == eHarryPeakDetectionType.BASELINE)
        {
            mean = CalculateMean(startIndex, endIndex, dataSet);
            stdev = CalculateStdev(startIndex, endIndex, dataSet);
        }

        // find everything that qualifies as a peak
        for (currentIndex = startIndex; currentIndex < endIndex; currentIndex++)
        {
            int previousSample = currentSample;
            currentSample = dataSet.get(currentIndex).m_PPG;

            // depending on the phase of the test, the detector changes where it gets mean and stdev from
            if (detectionType == eHarryPeakDetectionType.NON_BASELINE)
            {
                // calculate the mean and stdev based on where we are
                if (currentIndex < startIndex + (SAMPLES_PER_SECOND * 2))
                {
                    // in the first two seconds they are just the first two seconds
                    mean = CalculateMean(startIndex, startIndex + (SAMPLES_PER_SECOND * 2), dataSet);
                    stdev = CalculateStdev(startIndex, startIndex + (SAMPLES_PER_SECOND * 2), dataSet);
                }
                else if (currentIndex > endIndex - (SAMPLES_PER_SECOND * 2))
                {
                    // in the last two seconds, they are just the last two seconds
                    mean = CalculateMean(endIndex - (SAMPLES_PER_SECOND * 2), endIndex, dataSet);
                    stdev = CalculateStdev(endIndex - (SAMPLES_PER_SECOND * 2), endIndex, dataSet);
                }
                else
                {
                    // otherwise they are one second before and one second after the current time
                    mean = CalculateMean(currentIndex - (SAMPLES_PER_SECOND * 1), currentIndex + (SAMPLES_PER_SECOND * 1), dataSet);
                    stdev = CalculateStdev(currentIndex - (SAMPLES_PER_SECOND * 1), currentIndex + (SAMPLES_PER_SECOND * 1), dataSet);
                }
            }

            // calculate the delta from the last point
            int delta = currentSample - previousSample;

            // see if this qualifies as a potential peak
            if ((currentSample > (mean + (stdev * scaleFactor))) && (delta > 0))
            {
                // thjis is a potential peak...save it
                ValueAndLocation vl = new ValueAndLocation();
                vl.location = currentIndex;
                vl.value = currentSample;
                potentialPeaks.add(vl);
            }
        }

        // go through the potential peaks and make sure there are no dips which show up as
        // a series of peaks in sequential locations and then one that is skipped
        for (currentIndex = 0; currentIndex < potentialPeaks.size() - 1; currentIndex++)
        {
            // if there's a gap of 3 or less in the peak locations, add the values of the points in between
            if (potentialPeaks.get(currentIndex + 1).location - potentialPeaks.get(currentIndex).location > 1 &&
                    potentialPeaks.get(currentIndex + 1).location - potentialPeaks.get(currentIndex).location <= 3)
            {
                // add the ones into the list where the locations were skipped
                for (int i = 0; i < (potentialPeaks.get(currentIndex + 1).location - potentialPeaks.get(currentIndex).location); i++)
                {
                    ValueAndLocation vlNew = new ValueAndLocation();
                    vlNew.location = potentialPeaks.get(currentIndex).location + 1 + i;
                    vlNew.value = dataSet.get(vlNew.location).m_PPG;
                    potentialPeaks.add(currentIndex + 1 + i, vlNew);
                }
            }
        }

        // go through each group of peaks and find the max for each group
        currentIndex = 0;
        while (currentIndex < potentialPeaks.size())
        {
            int maxValue = 0;
            int maxLocation = 0;
            int tempIndex = currentIndex;
            boolean foundOne = false;
            while (tempIndex + 1 < potentialPeaks.size())
            {
                if (potentialPeaks.get(tempIndex + 1).location == potentialPeaks.get(tempIndex).location + 1)
                {
                    if (potentialPeaks.get(tempIndex).value > maxValue)
                    {
                        maxValue = (int)potentialPeaks.get(tempIndex).value;
                        maxLocation = potentialPeaks.get(tempIndex).location;
                        foundOne = true;
                    }
                    // seems redundant, but if this is the last time through here, the value at tempIndex + 1 needs to be
                    // checked as a possible max
                    if (potentialPeaks.get(tempIndex + 1).value > maxValue)
                    {
                        maxValue = (int)potentialPeaks.get(tempIndex + 1).value;
                        maxLocation = potentialPeaks.get(tempIndex + 1).location;
                        foundOne = true;
                    }
                }
                else
                {
                    break;
                }
                tempIndex++;
            }
            if (foundOne)
            {
                // maxIndex now contains the location of the max value
                pv.peaks.add(maxLocation);
            }
            currentIndex = tempIndex + 1;
        }

        // loop through the peaks and find the valleys
        for (int i = 0; i < pv.peaks.size() - 1; i++)
        {
            int lowestValley = Integer.MAX_VALUE;
            int lowestValleyLocation = 0;

            for (int j = pv.peaks.get(i); j < pv.peaks.get(i + 1); j++)
            {
                if (dataSet.get(j).m_PPG < lowestValley)
                {
                    lowestValley = dataSet.get(j).m_PPG;
                    lowestValleyLocation = j;
                }
            }
            pv.valleys.add(lowestValleyLocation);
        }
        return pv;
    }

    private double CalculateMean(int startIndex, int endIndex, ArrayList<RealtimeDataSample> dataSet)
    {
        int dataSum = 0;
        for (int i = startIndex; i < endIndex; i++)
        {
            dataSum += dataSet.get(i).m_PPG;
        }
        return dataSum / (endIndex - startIndex);
    }

    private double CalculateStdev(int startIndex, int endIndex, ArrayList<RealtimeDataSample> dataSet)
    {
        double diffSumSquared = 0.0;
        double mean = CalculateMean(startIndex, endIndex, dataSet);

        for (int i = startIndex; i < endIndex; i++)
        {
            diffSumSquared += Math.pow((dataSet.get(i).m_PPG - mean), 2);
        }
        return Math.sqrt(diffSumSquared / (endIndex - startIndex));
    }

    public enum ePostPeakDetection
    {
        LOOKING_FOR_PEAK, LOOKING_FOR_VALLEY
    }

    public enum eHarryPeakDetectionType
    {
        BASELINE, NON_BASELINE
    }
}
