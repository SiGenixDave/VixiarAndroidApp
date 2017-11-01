package com.vixiar.indicor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PracticeActivity extends Activity
{
    Handler handler = new Handler();
    final Runnable r = new Runnable()
    {
        public void run()
        {
            handler.postDelayed(this, 100);
            Animate();
        }
    };
    private ImageView pressureBarImage;
    private ImageView pressureBallImage;
    private DisplayMetrics metrics;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        handler.postDelayed(r, 100);
    }

    int index = 0;

    private void Animate()
    {
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(pressureBallImage.getLayoutParams());
        marginParams.setMargins(100, (int)((float)index * metrics.density), 0, 0);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(marginParams);
        pressureBallImage.setLayoutParams(layoutParams);

        index += 5;
        if (index >= 300)
        {
            index = 0;
        }
    }
}
