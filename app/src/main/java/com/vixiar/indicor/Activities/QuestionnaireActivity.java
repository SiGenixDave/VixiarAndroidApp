package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.widget.TextView;
import android.view.View;
import android.content.Intent;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.vixiar.indicor.CustomDialog.CustomAlertDialog;
import com.vixiar.indicor.CustomDialog.CustomDialogInterface;

import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QuestionnaireActivity extends Activity implements CustomDialogInterface
{
    private final static String TAG = QuestionnaireActivity.class.getSimpleName();

    RadioGroup radioQ1;
    RadioGroup radioQ2;
    RadioGroup radioQ3;
    RadioGroup radioQ4;
    RadioGroup radioQ5;
    RadioGroup radioQ6;
    RadioButton radioQ1Button;
    RadioButton radioQ2Button;
    RadioButton radioQ3Button;
    RadioButton radioQ4Button;
    RadioButton radioQ5Button;
    RadioButton radioQ6Button;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);
        SetFontFamily();
        InitializeHeaderAndFooter();
    }

    public void SetFontFamily()
    {
        Typeface robotoTypeface = ResourcesCompat.getFont(this, R.font.roboto_light);

        TextView v = findViewById(R.id.lblQ1);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblQ2);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblQ3);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblQ4);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblQ5);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.lblQ6);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioNo1);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioNo2);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioNo3);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioNo4);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioNo5);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioYes1);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioYes2);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioYes3);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioYes4);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioYes5);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioBetter);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioMuchWorse);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioSame);
        v.setTypeface(robotoTypeface);

        v = findViewById(R.id.radioWorse);
        v.setTypeface(robotoTypeface);

    }


    public void InitializeControls()
    {
        radioQ1 = findViewById(R.id.radioQ1);
        radioQ2 = findViewById(R.id.radioQ2);
        radioQ3 = findViewById(R.id.radioQ3);
        radioQ4 = findViewById(R.id.radioQ4);
        radioQ5 = findViewById(R.id.radioQ5);
        radioQ6 = findViewById(R.id.radioQ6);
    }

    public void InitializeHeaderAndFooter()
    {
        InitializeControls();
        HeaderFooterControl.getInstance().SetScreenTitle(this, "Questionnaire");
        HeaderFooterControl.getInstance().SetTypefaces(this);
        HeaderFooterControl.getInstance().HideBatteryIcon(this);
        HeaderFooterControl.getInstance().SetNavButtonTitle(this, getString(R.string.cancel));
        HeaderFooterControl.getInstance().SetScreenTitle(this, getString(R.string.pat_info_screen_title));
        HeaderFooterControl.getInstance().SetBottomMessage(this, getString(R.string.complete_data_entry));
        HeaderFooterControl.getInstance().DimPracticeButton(this);

        HeaderFooterControl.getInstance().SetNextButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int selectedQ1Id = radioQ1.getCheckedRadioButtonId();
                int selectedQ2Id = radioQ2.getCheckedRadioButtonId();
                int selectedQ3Id = radioQ3.getCheckedRadioButtonId();
                int selectedQ4Id = radioQ4.getCheckedRadioButtonId();
                int selectedQ5Id = radioQ5.getCheckedRadioButtonId();
                int selectedQ6Id = radioQ6.getCheckedRadioButtonId();
                radioQ1Button = findViewById(selectedQ1Id);
                radioQ2Button = findViewById(selectedQ2Id);
                radioQ3Button = findViewById(selectedQ3Id);
                radioQ4Button = findViewById(selectedQ4Id);
                radioQ5Button = findViewById(selectedQ5Id);
                radioQ6Button = findViewById(selectedQ6Id);
                PatientInfo.getInstance().set_questionnaire(radioQ1Button.getText().toString(), radioQ2Button.getText().toString(),
                        radioQ3Button.getText().toString(), radioQ4Button.getText().toString(), radioQ5Button.getText().toString(),
                        radioQ6Button.getText().toString());
                CacheQuestionnaireDate();
                Intent intent = new Intent(QuestionnaireActivity.this, TestingActivity.class);
                startActivity(intent);
            }
        });

        HeaderFooterControl.getInstance().SetNavButtonListner(this, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                HandleRequestToCancel();
            }
        });

    }

    private void CacheQuestionnaireDate()
    {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "cache-questionnaireinfo.txt";
        String filePath = baseDir + File.separator + fileName;
        File file = new File(filePath);

        try
        {
            FileOutputStream fos = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(fos);
            pw.println(GetCurrentDate());
            file.setWritable(true);
            pw.flush();
            pw.close();
            fos.close();

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you"
                    + " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String GetCurrentDate()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return simpleDateFormat.format(date);
    }


    public void HandleRequestToCancel()
    {
        // display the test cancel dialog
        CustomAlertDialog.getInstance().showConfirmDialog(CustomAlertDialog.Custom_Dialog_Type.DIALOG_TYPE_WARNING, 2,
                getString(R.string.dlg_title_cancel_test),
                getString(R.string.dlg_msg_cancel_test),
                "Yes",
                "No", QuestionnaireActivity.this , 0, QuestionnaireActivity.this);
    }

    @Override
    public void onClickPositiveButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
            case 0:
                Intent main = new Intent(QuestionnaireActivity.this, MainActivity.class);
                navigateUpTo(main);
                break;
        }
    }

    @Override
    public void onClickNegativeButton(DialogInterface dialog, int dialogID)
    {
        switch (dialogID)
        {
        }
    }
}
