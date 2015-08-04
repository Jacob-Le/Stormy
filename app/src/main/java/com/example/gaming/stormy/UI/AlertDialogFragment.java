package com.example.gaming.stormy.UI;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.example.gaming.stormy.R;

/**
 * Created by Gaming on 7/26/2015.
 */
public class AlertDialogFragment extends DialogFragment {
    //Creates a message box to alert the user that an error has occurred
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.error_title))
                .setMessage(context.getString(R.string.error_message))
                .setPositiveButton(context.getString(R.string.error_ok_button_text), null);
        AlertDialog dialog = builder.create();
        return dialog;
    }
}
