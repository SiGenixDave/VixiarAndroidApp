/**
 * This class is used to filter data entry fields and will not allow values outside of a range to be
 * entered
 *
 * @file InputFilterMinMax.java
 * @brief Defines the InputFilterMinMax class
 * @copyright Copyright 2018 Vixiar Inc.. All rights reserved.
 */

package com.vixiar.indicor.Activities;

import android.text.InputFilter;
import android.text.Spanned;


/**
 * his class is used to filter data entry fields and will not allow values outside of a range to be
 * entered
 */
public class InputFilterMinMax implements InputFilter
{
    private int m_min, m_max;

    /**
     * Constructor that takes integer values for min and max
     *
     * @param min The min allowed value
     * @param max
     */
    public InputFilterMinMax(int min, int max)
    {
        this.m_min = min;
        this.m_max = max;
    }

    /**
     * Constructor that takes string values for min and max
     *
     * @param min The min allowed value
     * @param max
     */
    public InputFilterMinMax(String min, String max)
    {
        this.m_min = Integer.parseInt(min);
        this.m_max = Integer.parseInt(max);
    }

    /**
     * @param source
     * @param start
     * @param end
     * @param dest
     * @param dstart
     * @param dend
     * @return
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    {
        try
        {
            // Remove the string out of destination that is to be replaced
            int input;
            String newVal = dest.toString() + source.toString();
            if (newVal.length() == 1 && newVal.charAt(0) == '-')
            {
                input = m_min; //allow
            } else
            {
                newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                // Add the new string in
                newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                input = Integer.parseInt(newVal);
            }

            //int input = Integer.parseInt(dest.toString() + source.toString());

            if (isInRange(m_min, m_max, input))
                return null;
        } catch (NumberFormatException nfe)
        {
        }
        return "";
    }

    /**
     * Determines if a valued is within an allowed range
     *
     * @param min   The min
     * @param max   The max
     * @param value The value to test
     * @return True if value is greater than or equal to min and less than or equal to max
     */
    private boolean isInRange(int min, int max, int value)
    {
        return max > min ? value >= min && value <= max : value >= max && value <= min;
    }
}
