package com.vixiar.indicor.CustomDialog;

import android.content.DialogInterface;

/**
 * Created by gyurk on 11/22/2017.
 */

public interface CustomDialogInterface
{

    void onClickPositiveButton(DialogInterface dialog, int dialogID);

    void onClickNegativeButton(DialogInterface dialog, int dialogID);
}