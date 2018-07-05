package com.douglasharvey.receipttracker.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    public interface EditDateDialogListener {
        void onFinishEditDialog(int year, int month, int day);
    }


    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = this.getArguments();
        int year, month, day;
        if (bundle!=null && bundle.containsKey("setYear")) {
            year = bundle.getInt("setYear");
            month = bundle.getInt("setMonth");
            day = bundle.getInt("setDay");
        } else {
            Calendar currentDate = Calendar.getInstance();
            year = currentDate.get(Calendar.YEAR);
            month = currentDate.get(Calendar.MONTH);
            day = currentDate.get(Calendar.DAY_OF_MONTH);
        }
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Do something with the date chosen by the user
        EditDateDialogListener listener = (EditDateDialogListener) getActivity();
        listener.onFinishEditDialog(year, month, day);

    }
}
