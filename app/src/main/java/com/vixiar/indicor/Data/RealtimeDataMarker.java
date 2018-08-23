package com.vixiar.indicor.Data;

/**
 * Created by gyurk on 11/21/2017.
 */

public class RealtimeDataMarker
{
    public enum Marker_Type
    {
        MARKER_START_VALSALVA,
        MARKER_END_VALSALVA,
        MARKER_TEST_ERROR,
    }
    public final Marker_Type type;
    public final int dataIndex;

    RealtimeDataMarker(Marker_Type type, int index)
    {
        this.type = type;
        this.dataIndex = index;
    }
}
