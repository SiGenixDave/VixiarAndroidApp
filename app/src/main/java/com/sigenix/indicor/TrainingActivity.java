package com.sigenix.indicor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class TrainingActivity extends Activity
{

    int yOrig = 640;
    int offset = -5;
    Handler handler = new Handler();
    private ImageView pressureDetector;
    final Runnable r = new Runnable()
    {
        public void run()
        {
            handler.postDelayed(this, 10);
            Animate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        InitViews();
    }

    private void InitViews()
    {

        yOrig = ConvertDptoPixels(480);
        pressureDetector = (ImageView) findViewById(R.id.imViewPressureDetector);

        handler.postDelayed(r, 1000);
    }

    private int ConvertDptoPixels(int dp)
    {
        return dp * 213 / 160;
    }

    private void Animate()
    {

        if (yOrig < ConvertDptoPixels(40))
        {
            offset = 5;
        } else if (yOrig > ConvertDptoPixels(480))
        {
            offset = -5;
        }

        yOrig += offset;

        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(pressureDetector.getLayoutParams());
        marginParams.setMargins(ConvertDptoPixels(135), yOrig, 0, 0);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(marginParams);
        pressureDetector.setLayoutParams(layoutParams);
    }


}
