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

    private TextView screenTitle;
    private Button navigationButton;
    private TextView bottomMessage;
    private ImageView practiceButton;
    private ImageView nextButton;


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
        if (b != null)
        {
            b.setAlpha((float) 0.5);
        }
    }

    public void UnDimPracticeButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.practiceButton);
        if (b != null)
        {
            b.setAlpha((float) 1.0);
        }
    }

    public void DimNextButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
        if (b != null)
        {
            b.setAlpha((float) 0.5);
        }
    }

    public void UnDimNextButton(Activity a)
    {
        ImageButton b = (ImageButton) a.findViewById(R.id.startTestButton);
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
