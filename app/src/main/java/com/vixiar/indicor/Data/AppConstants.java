package com.vixiar.indicor.Data;

public class AppConstants
{
    public static final double BASELINE_SCALE_FACTOR = 0.7;
    public static final double VALSALVA_SCALE_FACTOR = 0.8;
    public static final double POST_VALSALVA_SCALE_FACTOR = 1.0;
    public static final int BASELINE_LENGTH_SEC = 15;
    public static final int BASELINE_END_BEFORE_VSTART_SEC = 5;
    public static final int SAMPLES_PER_SECOND = 50;
    public static final int MIN_STABLE_HR = 40;
    public static final int MAX_STABLE_HR = 120;
    public static final int PPG_AMPLITUDE_LIMIT_FOR_MOVEMENT = 60000;
    public static final int LOOKBACK_SECONDS_FOR_FLATLINE = 2;
    public static final int LOOKBACK_SECONDS_FOR_MOVEMENT = 1;
    public static final double  SD_LIMIT_FOR_FLATLINE = 200.0;
    public static final double HF_NOISE_LIMIT = 0.4;
}
