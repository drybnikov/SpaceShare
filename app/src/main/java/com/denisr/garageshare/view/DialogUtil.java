package com.denisr.garageshare.view;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.denisr.garageshare.R;

public class DialogUtil {

    public static void showDialogWithEditText(Context context, @StringRes int title, final String defaultValue,
                                              final DialogClickListiner listiner) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        final TextInputLayout inputLayout = new TextInputLayout(context);
        inputLayout.setPadding(72, 72, 72, 0);

        final EditText editText = new EditText(context);
        editText.setHint(R.string.item_dialog_title_hint);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(lp);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setText(defaultValue);
        inputLayout.addView(editText, lp);

        alertDialog.setTitle(title);
        alertDialog.setView(inputLayout);

        alertDialog.
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String newValue = editText.getText().toString();
                        listiner.onClick(dialogInterface, newValue);
                    }
                }).
                setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).
                create();

        alertDialog.show();
    }

    public interface DialogClickListiner {
        void onClick(DialogInterface dialogInterface, String itemValue);
    }
}
