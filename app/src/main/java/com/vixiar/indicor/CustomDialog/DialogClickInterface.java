package com.vixiar.indicor.CustomDialog;

import android.content.DialogInterface;

/**
 * Created by gyurk on 11/22/2017.
 */

public interface DialogClickInterface {

    public void onClickPositiveButton(DialogInterface pDialog, int pDialogIntefier);

    public void onClickNegativeButton(DialogInterface pDialog, int pDialogIntefier);
}