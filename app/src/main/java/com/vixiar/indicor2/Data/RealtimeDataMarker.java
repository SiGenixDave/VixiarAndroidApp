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
        MARKER_START_VALSALVA, MARKER_END_VALSALVA, MARKER_TEST_ERROR,
    }
}
