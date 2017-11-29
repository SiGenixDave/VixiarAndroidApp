package com.vixiar.indicor.CustomDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vixiar.indicor.R;

/**
 * Created by gyurk on 11/22/2017.
 */

public class CustomAlertDialog implements CustomDialogInterface, DialogInterface.OnClickListener
{

    public static CustomAlertDialog mDialog;
    public CustomDialogInterface mDialogClickInterface;
    private int mDialogIdentifier;
    private Context mContext;

    public static CustomAlertDialog getInstance()
    {

        if (mDialog == null)
            mDialog = new CustomAlertDialog();

        return mDialog;

    }

    public enum Custom_Dialog_Type
    {
        DIALOG_TYPE_CHECK,
        DIALOG_TYPE_INFO,
        DIALOG_TYPE_WARNING
    }

    /**
     * Show confirmation dialog with two buttons
     *
     * @param numButtons
     * @param dialogTitle
     * @param message
     * @param positiveButton
     * @param negativeButton
     * @param context
     * @param dialogID
     */
    public void showConfirmDialog(Custom_Dialog_Type type,
                                  int numButtons, String dialogTitle, String message,
                                  String positiveButton, String negativeButton,
                                  Context context, final int dialogID)
    {

        mDialogClickInterface = (CustomDialogInterface) context;
        mDialogIdentifier = dialogID;
        mContext = context;

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (numButtons == 2)
        {
            dialog.setContentView(R.layout.custom_2btn_dialog_layout);
        }
        else
        {
            dialog.setContentView(R.layout.custom_1btn_dialog_layout);
        }

        if (!dialogTitle.equals(""))
        {
            TextView title = dialog.findViewById(R.id.textTitle);
            title.setText(dialogTitle);
            title.setVisibility(View.VISIBLE);
        }

        TextView text = dialog.findViewById(R.id.textDialog);
        text.setText(message);

        if (numButtons == 2)
        {
            Button btnNegative = dialog.findViewById(R.id.btnNegative);
            btnNegative.setText(negativeButton);

            btnNegative.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Close dialog
                    dialog.dismiss();
                    mDialogClickInterface.onClickNegativeButton(dialog, dialogID);
                }
            });
        }

        Button btnPositive = dialog.findViewById(R.id.btnPositive);
        btnPositive.setText(positiveButton);
        btnPositive.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Close dialog
                dialog.dismiss();
                mDialogClickInterface.onClickPositiveButton(dialog, dialogID);
            }
        });

        dialog.setCancelable(false);
        LinearLayout titleBackground = dialog.findViewById(R.id.dialogTitleBackground);
        switch (type)
        {
            case DIALOG_TYPE_WARNING:
                titleBackground.setBackgroundColor(ContextCompat.getColor(dialog.getContext(), R.color.colorDialogTitleOrange));
                ImageView v = dialog.findViewById(R.id.titleIcon);
                v.setImageResource(R.drawable.dialog_warning);
                break;
            case DIALOG_TYPE_INFO:
                titleBackground.setBackgroundColor(ContextCompat.getColor(dialog.getContext(), R.color.colorDialogTitleBlue));
                v = dialog.findViewById(R.id.titleIcon);
                v.setImageResource(R.drawable.dialog_exclamation);
                break;
            case DIALOG_TYPE_CHECK:
                titleBackground.setBackgroundColor(ContextCompat.getColor(dialog.getContext(), R.color.colorDialogTitleGreen));
                v = dialog.findViewById(R.id.titleIcon);
                v.setImageResource(R.drawable.dialog_check);
                break;
        }
        dialog.show();      // if decline button is clicked, close the custom dialog
    }

    @Override
    public void onClick(DialogInterface pDialog, int pWhich)
    {
        switch (pWhich)
        {
            case DialogInterface.BUTTON_POSITIVE:
                mDialogClickInterface.onClickPositiveButton(pDialog, mDialogIdentifier);

                break;
            case DialogInterface.BUTTON_NEGATIVE:
                mDialogClickInterface.onClickNegativeButton(pDialog, mDialogIdentifier);
                break;
        }

    }

    @Override
    public void onClickPositiveButton(DialogInterface dialog, int dialogID)
    {
    }

    @Override
    public void onClickNegativeButton(DialogInterface dialog, int dialogID)
    {
    }

}