package com.vixiar.indicor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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

        ImageButton btnExit = (ImageButton) findViewById(R.id.exitButton);
        btnExit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                adb.setTitle(getString(R.string.exit_confirmation_title));
                adb.setMessage(getString(R.string.exit_confirmation_message));
                adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                System.exit(0);
                            }
                        });
                adb.setNegativeButton(android.R.string.cancel, null);
                adb.show();
            }
        });
    }

    public void onImageStartClick(View view)
    {
        Intent intent = new Intent(this, PatInfoActivity.class);
        startActivity(intent);
    }
}
