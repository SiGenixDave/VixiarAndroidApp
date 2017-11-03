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
        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, "");
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.main_screen_title));

        // put the version number into the bottom status bar
        String nameAndVersion = getString(R.string.main_screen_title) +
                " V" + BuildConfig.VERSION_NAME;

        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView versionText = (TextView)findViewById(R.id.textViewVersion);
        versionText.setText(nameAndVersion);
        versionText.setTypeface(robotoTypeface);

        TextView v = (TextView) findViewById(R.id.websiteLbl);
        v.setTypeface(robotoTypeface);
        v.setText(R.string.vixiar_web);

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
