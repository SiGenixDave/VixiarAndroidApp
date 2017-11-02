package com.vixiar.indicor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup the top bar
        Button navButton = (Button)findViewById(R.id.navButton);
        navButton.setText("");
        ImageView batteryIcon = (ImageView) findViewById(R.id.batteryIcon);
        batteryIcon.setVisibility(View.INVISIBLE);

        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView titleTxt = (TextView)findViewById(R.id.txtScreenName);
        titleTxt.setText(R.string.application_name);
        titleTxt.setTypeface(robotoTypeface);

        // put the version number into the bottom status bar
        String nameAndVersion = getString(R.string.application_name) +
                " V" + BuildConfig.VERSION_NAME;

        TextView versionText = (TextView)findViewById(R.id.textViewVersion);
        versionText.setText(nameAndVersion);
        versionText.setTypeface(robotoTypeface);

        TextView v = (TextView) findViewById(R.id.websiteLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.startTestLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.settingsLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.manualLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.trainingLbl);
        v.setTypeface(robotoTypeface);

        v = (TextView) findViewById(R.id.tagLineLbl);
        v.setTypeface(robotoTypeface);
    }

    public void onImageStartClick(View view)
    {
        Intent intent = new Intent(this, PatInfoActivity.class);
        startActivity(intent);
    }
}
