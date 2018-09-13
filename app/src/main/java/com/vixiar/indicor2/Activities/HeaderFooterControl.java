package com.vixiar.indicor2.Activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.vixiar.indicor2.R;

/**
 * Created by gyurk on 11/2/2017.
 */

public class HeaderFooterControl
{
    // make this a singleton class
    private static HeaderFooterControl ourInstance = new HeaderFooterControl();

    public static HeaderFooterControl getInstance()
    {
        return ourInstance;
    }

    private static final int BATTERY_LEVEL_FULL = 90;
    private static final int BATTERY_LEVEL_3BARS = 70;
    private static final int BATTERY_LEVEL_2BARS = 50;
    private static final int BATTERY_LEVEL_1BARS = 30;
    private static final int BATTERY_LEVEL_0 = 10;

    public void SetTypefaces(Activity a, Context c)
    {
        Typeface robotoTypeface = Typeface.createFromAsset(c.getAssets(), "fonts/roboto_light.ttf");

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

    public void SetScreenTitle(Activity a, String title)
    {
        TextView v = (TextView) a.findViewById(R.id.txtScreenName);
        if (v != null)
        {
            v.setText(title);
        }
    }

    public void SetNavButtonTitle(Activity a, String title)
    {
        TextView v = (TextView) a.findViewById(R.id.navButton);
        if (v != null)
        {
            v.setText(title);
        }
    }

    public void SetBottomMessage(Activity a, String title)
    {
        TextView v = (TextView) a.findViewById(R.id.txtMessage);
        if (v != null)
        {
            v.setText(title);
        }
    }

    public void HidePracticeButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        if (b != null)
        {
            b.setVisibility(View.INVISIBLE);
        }
    }

    public void DimPracticeButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        b.setEnabled(false);
        if (b != null)
        {
            b.setAlpha((float) 0.5);
        }
    }

    public void UnDimPracticeButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        b.setEnabled(true);
        if (b != null)
        {
            b.setAlpha((float) 1.0);
        }
    }

    public void DimNextButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
        b.setEnabled(false);
        if (b != null)
        {
            b.setAlpha((float) 0.5);
        }
    }

    public void UnDimNextButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
        b.setEnabled(true);
        if (b != null)
        {
            b.setAlpha((float) 1.0);
        }
    }

    public void HideBatteryIcon(Activity a)
    {
        ImageView v = a.findViewById(R.id.batteryIcon);
        if (v != null)
        {
            v.setVisibility(View.INVISIBLE);
        }
    }

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

    public void SetNavButtonListner(Activity a, View.OnClickListener l)
    {
        Button b = (Button) a.findViewById(R.id.navButton);
        if (b != null)
        {
            b.setOnClickListener(l);
        }
    }

    public void SetNextButtonListner(Activity a, View.OnClickListener l)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
        if (b != null)
        {
            b.setOnClickListener(l);
        }
    }

    public void SetPracticeButtonListner(Activity a, View.OnClickListener l)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        if (b != null)
        {
            b.setOnClickListener(l);
        }
    }
}