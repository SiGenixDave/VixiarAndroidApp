package com.vixiar.indicor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // put the version number into the bottom status bar
        String nameAndVersion = getString(R.string.application_name) +
                " V" + BuildConfig.VERSION_NAME;

        TextView versionText = (TextView)findViewById(R.id.textViewVersion);
        versionText.setText(nameAndVersion);
    }

    public void onImageStartClick(View view)
    {
        Intent intent = new Intent(this, PatInfoActivity.class);
        startActivity(intent);
    }
}
