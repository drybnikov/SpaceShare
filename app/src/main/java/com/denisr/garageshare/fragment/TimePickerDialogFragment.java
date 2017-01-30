package com.denisr.garageshare.fragment;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;

import com.denisr.garageshare.R;

import java.util.Calendar;

public class TimePickerDialogFragment extends DialogFragment {
    final Calendar calendar = Calendar.getInstance();
    // Use this instance of the interface to deliver action events
    private TimePickerDialogListener mListener;
    private TimePicker startTime;
    private TimePicker endTime;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.book_date_picker, null);

        startTime = (TimePicker) view.findViewById(R.id.start_time_picker);
        endTime = (TimePicker) view.findViewById(R.id.end_time_picker);

        startTime.setIs24HourView(true);
        endTime.setIs24HourView(true);
        startTime.setHour(8);
        endTime.setHour(17);

        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);

        builder.setView(view)
                .setTitle(R.string.select_booked_time_dialog_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(TimePickerDialogFragment.this,
                                getTime(startTime.getHour(), startTime.getMinute()),
                                getTime(endTime.getHour(), endTime.getMinute()));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(TimePickerDialogFragment.this);
                        //dialog.cancel();
                    }
                });


        return builder.create();
    }

    private long getTime(int hour, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        return calendar.getTimeInMillis();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (TimePickerDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString() + " must implement TimePickerDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface TimePickerDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, long startTime, long endTime);

        void onDialogNegativeClick(DialogFragment dialog);
    }
}
