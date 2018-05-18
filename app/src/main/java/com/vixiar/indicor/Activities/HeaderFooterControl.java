/**
 * The HeaderFooterControl manages the headers and footers on most of the screens.
 * @file HeaderFooterControl.java
 * @brief Defines the HeaderFooterControl class
 * @copyright Copyright 2018 Vixiar Inc.. All rights reserved.
 */

package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.vixiar.indicor.R;

/**
 * The HeaderFooterControl manages the headers and footers on most of the screens.
 */
public class HeaderFooterControl
{
    /// make this a singleton class
    private static HeaderFooterControl ourInstance = new HeaderFooterControl();

    public static HeaderFooterControl getInstance()
    {
        return ourInstance;
    }

    /** @name
     * Define the thresholds where the battery icon changes
     */
    //@{
    private static final int BATTERY_LEVEL_FULL = 90;
    private static final int BATTERY_LEVEL_3BARS = 70;
    private static final int BATTERY_LEVEL_2BARS = 50;
    private static final int BATTERY_LEVEL_1BARS = 30;
    private static final int BATTERY_LEVEL_0 = 10;
    //@}

    /**
     * This function sets the typeface for the ScreenName, the navigation button,
     * and the message area
     * @param a The Activity that is using the control
     */
    public void SetTypefaces(Activity a)
    {
        Typeface robotoTypeface = ResourcesCompat.getFont(a, R.font.roboto_light);

        TextView v = (TextView) a.findViewById(R.id.txtScreenName);
        if (v != null)
        {
            v.setTypeface(robotoTypeface);
        }

        v = (TextView) a.findViewById(R.id.navButton);
        if (v != null)
        {
            v.setTypeface(robotoTypeface);
        }
        v = (TextView) a.findViewById(R.id.txtMessage);
        if (v != null)
        {
            v.setTypeface(robotoTypeface);
        }
    }

    /**
     * Set the title area text
     * @param a The activity that is using the control
     * @param title The title to display
     */
    public void SetScreenTitle(Activity a, String title)
    {
        TextView v = (TextView) a.findViewById(R.id.txtScreenName);
        if (v != null)
        {
            v.setText(title);
        }
    }

    /**
     * Sets the string on the navigation button
     * @param a The activity that is using the control
     * @param title The title to display
     */
    public void SetNavButtonTitle(Activity a, String title)
    {
        TextView v = (TextView) a.findViewById(R.id.navButton);
        if (v != null)
        {
            v.setText(title);
        }
    }

    /**
     * Sets the message in the footer
     * @param a The activity that is using the control
     * @param title The title to display
     */
    public void SetBottomMessage(Activity a, String title)
    {
        TextView v = (TextView) a.findViewById(R.id.txtMessage);
        if (v != null)
        {
            v.setText(title);
        }
    }

    /**
     * Removes the practice button
     * @param a The activity that is using the control
     */
    public void HidePracticeButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        if (b != null)
        {
            b.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Dims the practice button
     * @param a The activity that is using the control
     */
    public void DimPracticeButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        b.setEnabled(false);
        if (b != null)
        {
            b.setAlpha((float) 0.5);
        }
    }

    /**
     * Undims the practice button
     * @param a The activity that is using the control
     */
    public void UnDimPracticeButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        b.setEnabled(true);
        if (b != null)
        {
            b.setAlpha((float) 1.0);
        }
    }

    /**
     * Dims the next button
     * @param a The activity that is using the control
     */
    public void DimNextButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
        b.setEnabled(false);
        if (b != null)
        {
            b.setAlpha((float) 0.5);
        }
    }

    /**
     * Undims the next button
     * @param a The activity that is using the control
     */
    public void UnDimNextButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
        b.setEnabled(true);
        if (b != null)
        {
            b.setAlpha((float) 1.0);
        }
    }

    /**
     * Removes the battery icon
     * @param a The activity that is using the control
     */
    public void HideBatteryIcon(Activity a)
    {
        ImageView v = a.findViewById(R.id.batteryIcon);
        if (v != null)
        {
            v.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Displays the icon for the battery level based on the level passed and
     * the defined thresholds for when to display each image.
     * @param a The activity that is using the control
     * @param level The current battery level
     */
    public void ShowBatteryIcon(Activity a, int level)
    {
        ImageView v = a.findViewById(R.id.batteryIcon);
        if (v != null)
        {
            v.setVisibility(View.VISIBLE);
            if (level > BATTERY_LEVEL_FULL)
            {
                v.setImageResource(R.drawable.battery_4);
            }
            else if (level >= BATTERY_LEVEL_3BARS)
            {
                v.setImageResource(R.drawable.battery_3);
            }
            else if (level >= BATTERY_LEVEL_2BARS)
            {
                v.setImageResource(R.drawable.battery_2);
            }
            else if (level >= BATTERY_LEVEL_1BARS)
            {
                v.setImageResource(R.drawable.battery_1);
            }
            else
            {
                v.setImageResource(R.drawable.battery_0);
            }
        }
    }

    /**
     * Set's the listner to be called when the user clicks on the navigation button
     * @param a The activity that is using the control
     * @param l The listner
     */
    public void SetNavButtonListner(Activity a, View.OnClickListener l)
    {
        Button b = (Button) a.findViewById(R.id.navButton);
        if (b != null)
        {
            b.setOnClickListener(l);
        }
    }

    /**
     * Set's the listner to be called when the user clicks on the next button
     * @param a The activity that is using the control
     * @param l The listner
     */
    public void SetNextButtonListner(Activity a, View.OnClickListener l)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
        if (b != null)
        {
            b.setOnClickListener(l);
        }
    }

    /**
     * Set's the listner to be called when the user clicks on the practice button
     * @param a The activity that is using the control
     * @param l The listner
     */
    public void SetPracticeButtonListner(Activity a, View.OnClickListener l)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        if (b != null)
        {
            b.setOnClickListener(l);
        }
    }
}
