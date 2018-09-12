package com.vixiar.indicor2.CustomDialog;

import android.content.DialogInterface;

/**
 * Created by gyurk on 11/22/2017.
 */

public interface CustomDialogInterface
{

    public void onClickPositiveButton(DialogInterface dialog, int dialogID);

    public void onClickNegativeButton(DialogInterface dialog, int dialogID);
}