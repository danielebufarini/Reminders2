package com.danielebufarini.reminders2.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.danielebufarini.reminders2.util.Dates;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static com.danielebufarini.reminders2.ui.task.ManageTaskActivity.TIME_FORMAT_STRING;

public class TimeBasedReminderFragment extends Fragment {
    private static final long[] INTERVALS = {
            0L, // none
            60000L, // one minute
            3600000L, // one hour
            86400000L, // one day
            604800000L, // one week
            1209600000L, // two weeks
            2630000000L, // one month
            31560000000L // one year
    };
    private static final Map<Long, Integer> INTERVALS_MAP = new HashMap<>(INTERVALS.length);

    static {
        for (int i = 0; i < INTERVALS.length; ++i)
            INTERVALS_MAP.put(INTERVALS[i], i);
    }

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final ApplicationCache CACHE = ApplicationCache.INSTANCE;

    private OnReminderDateChangedListener listener;
    private TextView reminderDay, reminderTime;
    private Calendar reminderDate;
    private String param1, param2;

    public static TimeBasedReminderFragment newInstance(String param1, String param2) {

        TimeBasedReminderFragment fragment = new TimeBasedReminderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TimeBasedReminderFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            param1 = getArguments().getString(ARG_PARAM1);
            param2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tab_fragment_time_based_reminder, container, false);
    }

    private void attachListener(Activity activity) {

        try {
            listener = (OnReminderDateChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnReminderDateChangedListener.class.getSimpleName());
        }
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        attachListener((Activity) context);
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        attachListener(activity);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        setupWidgets();
    }

    @Override
    public void onDetach() {

        super.onDetach();
        listener = null;
    }

    private void setupWidgets() {

        GTask task = CACHE.getTask();
        TextView remindMeLabel = getActivity().findViewById(R.id.remind_me_label);
        RelativeLayout reminderLayout = getActivity().findViewById(R.id.reminder_layout);
        boolean isOnlyLabelVisible = task.reminderDate == 0L;
        remindMeLabel.setVisibility(isOnlyLabelVisible ? View.VISIBLE : View.INVISIBLE);
        remindMeLabel.setOnClickListener(v -> {
            remindMeLabel.setVisibility(View.GONE);
            reminderLayout.setVisibility(View.VISIBLE);
        });
        reminderLayout.setVisibility(isOnlyLabelVisible ? View.GONE : View.VISIBLE);
        ImageButton deleteReminder = getActivity().findViewById(R.id.delete_reminder);
        deleteReminder.setOnClickListener(v -> {
            remindMeLabel.setVisibility(View.VISIBLE);
            reminderLayout.setVisibility(View.GONE);
            task.reminderDate = 0L;
            task.reminderInterval = 0L;
        });
        Spinner interval = getActivity().findViewById(R.id.reminderInterval);
        interval.setSelection(INTERVALS_MAP.get(task.getReminderInterval()));
        reminderDate = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        reminderDay = getActivity().findViewById(R.id.reminder_day);
        reminderDay.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    TimeBasedReminderFragment.this.getActivity(),
                    (view, year, monthOfYear, dayOfMonth) -> {
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(year, monthOfYear, dayOfMonth);
                        reminderDay.setText(Dates.formatDate(newDate));
                        listener.onReminderDateChanged(reminderDate);
                    },
                    reminderDate.get(Calendar.YEAR),
                    reminderDate.get(Calendar.MONTH),
                    reminderDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        reminderDay.setText(Dates.formatDate(reminderDate));
        reminderTime = getActivity().findViewById(R.id.reminder_time);
        reminderTime.setOnClickListener(v -> {
            TimePickerDialog datePicker = new TimePickerDialog(
                    TimeBasedReminderFragment.this.getActivity(),
                    (view, hourOfDay, minute) -> {
                        reminderDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        reminderDate.set(Calendar.MINUTE, minute);
                        listener.onReminderDateChanged(reminderDate);
                        reminderTime.setText(String.format(TIME_FORMAT_STRING, hourOfDay, minute));
                    },
                    reminderDate.get(Calendar.HOUR_OF_DAY),
                    reminderDate.get(Calendar.MINUTE),
                    true
            );
            datePicker.show();
        });
        reminderTime.setText(String.format(TIME_FORMAT_STRING, reminderDate.get(Calendar.HOUR_OF_DAY),
                reminderDate.get(Calendar.MINUTE)));
    }

    public interface OnReminderDateChangedListener {

        void onReminderDateChanged(Calendar reminderDate);
    }

}
