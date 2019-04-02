package com.vixiar.indicor2.Data;

/**
 * Created by gyurk on 11/21/2017.
 */

public class RealtimeDataMarker
{
    public Marker_Type type;
    public int dataIndex;
    RealtimeDataMarker(Marker_Type type, int index)
    {
        this.type = type;
        this.dataIndex = index;
    }

    public enum Marker_Type
    {
        MARKER_START_VALSALVA,
        MARKER_END_VALSALVA,
        MARKER_VALSALVA_PRESSURE,
        MARKER_COMMUNICATIONS_TIMEOUT,
        MARKER_AMBIENT_LIGHT,
        MARKER_FLATLINE,
        MARKER_HR_OUT_OF_RANGE,
        MARKER_HF_NOISE,
        MARKER_MOVEMENT_DETECTED,
        MARKER_UNSATABLE_BASELINE,
        MARKER_WEAK_PULSE,
    }
}
