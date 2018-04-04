package com.danielebufarini.reminders2.ui.task;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.Priority;
import com.danielebufarini.reminders2.ui.LocationBasedReminderFragment;
import com.danielebufarini.reminders2.ui.PagerAdapter;
import com.danielebufarini.reminders2.ui.TaskFragment;
import com.danielebufarini.reminders2.ui.TimeBasedReminderFragment;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.danielebufarini.reminders2.util.Dates;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ManageTaskActivity extends AppCompatActivity
        implements TimeBasedReminderFragment.OnReminderDateChangedListener,
        LocationBasedReminderFragment.OnReminderPlaceChangedListener {

    public static final String TIME_FORMAT_STRING = "%02d:%02d";
    private static final ApplicationCache CACHE = ApplicationCache.INSTANCE;

    private TextView dueDateDay, dueDateTime;
    private EditText title, notes;
    private Spinner priority;
    private GTask task = null;
    private Calendar reminderDate;
    private Integer taskPosition;
    private Calendar dueDate;
    private boolean isEditingExistingTask;
    private double latitude, longitude;
    private CharSequence locationTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_task);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                task = (GTask) extras.get(TaskFragment.TASK);
                taskPosition = extras.getInt(TaskFragment.TASK_POSITION);
            }
        } else
            task = (GTask) savedInstanceState.getSerializable(TaskFragment.TASK);

        setupWidgets();
        if (task != null) {
            updateWidgets(task);
        } else {
            task = createEmptyTask();
        }

        CACHE.setTask(task);
        isEditingExistingTask = taskPosition != null;
    }

    @Override
    protected void onPause() {

        super.onPause();
        CACHE.setTask(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (isTaskHasBeenModified()) {
                updateModel();
                storeItemInDB();
            }
            Intent upIntent = NavUtils.getParentActivityIntent(this);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                // This activity is NOT part of this app's task, so create a new task
                // when navigating up, with a synthesized back stack.
                TaskStackBuilder.create(this)
                        // Add all of this activity's parents to the back stack
                        .addNextIntentWithParentStack(upIntent)
                        // Navigate up to the closest parent
                        .startActivities();
            } else {
                // This activity is part of this app's task, so simply
                // navigate up to the logical parent activity.
                upIntent.putExtra(TaskFragment.TASK, task);
                upIntent.putExtra(TaskFragment.TASK_POSITION, taskPosition);
                if (getParent() == null)
                    setResult(RESULT_OK, upIntent);
                else
                    getParent().setResult(RESULT_OK, upIntent);
                NavUtils.navigateUpTo(this, upIntent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void storeItemInDB() {

        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        try (SQLiteDatabase db = databaseHelper.getWritableDatabase()) {
            if (isEditingExistingTask)
                task.merge(db);
            else
                task.insert(db);
        }
    }

    private int translatePriorityPosition(int position) {

        return position == 0 ? Priority.NONE.getPriority() : position;
    }

    private boolean isTaskHasBeenModified() {

        if (!title.getText().toString().equals(task.title)) return true;
        if (!notes.getText().toString().equals(task.notes)) return true;
        if (reminderDate != null && task.reminderDate != reminderDate.getTimeInMillis())
            return true;
        if (task.dueDate != dueDate.getTimeInMillis()) return true;
        if (task.priority != translatePriorityPosition(priority.getSelectedItemPosition()))
            return true;
        if (Double.compare(task.latitude, latitude) != 0) return true;
        if (Double.compare(task.longitude, longitude) != 0) return true;
        if (task.locationTitle.equals(locationTitle.toString())) return true;
        return false;
    }

    private void updateModel() {

        List<GTask> tasks =
                (List<GTask>) CACHE.getFolders().get(CACHE.getActiveFolder()).getChildren();
        tasks.remove(tasks.get(tasks.indexOf(task)));
        task.title = title.getText().toString();
        task.notes = notes.getText().toString();
        task.dueDate = dueDate.getTimeInMillis();
        task.isModified = true;
        task.updated = System.currentTimeMillis();
        task.reminderDate = reminderDate == null ? 0 : reminderDate.getTimeInMillis();
        task.priority = translatePriorityPosition(priority.getSelectedItemPosition());
        task.latitude = latitude;
        task.longitude = longitude;
        task.locationTitle = locationTitle != null ? locationTitle.toString() : "";
        tasks.add(task);
    }

    private void setupWidgets() {

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setIcon(R.mipmap.ic_alarm_black_24dp));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.mipmap.ic_room_black_24dp));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(R.id.pager);
        PagerAdapter adapter = new PagerAdapter(getFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        title = findViewById(R.id.taskTitle);
        notes = findViewById(R.id.note);

        dueDate = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());

        dueDateDay = findViewById(R.id.due_date_day);
        dueDateDay.setOnClickListener(v -> {

            DatePickerDialog datePickerDialog = new DatePickerDialog(ManageTaskActivity.this,
                    (view, year, monthOfYear, dayOfMonth) -> {

                        dueDate.set(Calendar.YEAR, year);
                        dueDate.set(Calendar.MONTH, monthOfYear);
                        dueDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dueDateDay.setText(Dates.formatDate(dueDate));
                    },
                    dueDate.get(Calendar.YEAR),
                    dueDate.get(Calendar.MONTH),
                    dueDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        dueDateDay.setText(Dates.formatDate(dueDate));
        dueDateTime = findViewById(R.id.due_date_time);
        dueDateTime.setOnClickListener(v -> {
            TimePickerDialog datePicker = new TimePickerDialog(ManageTaskActivity.this,
                    (view, hourOfDay, minute) -> {
                        dueDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        dueDate.set(Calendar.MINUTE, minute);
                        dueDateTime.setText(String.format(TIME_FORMAT_STRING, hourOfDay, minute));
                    },
                    dueDate.get(Calendar.HOUR_OF_DAY),
                    dueDate.get(Calendar.MINUTE),
                    true
            );
            datePicker.show();
        });
        dueDateTime.setText(String.format(TIME_FORMAT_STRING, dueDate.get(Calendar.HOUR_OF_DAY),
                dueDate.get(Calendar.MINUTE)));
        priority = findViewById(R.id.priority);
        priority.setAdapter(new PriorityAdapter(this, R.layout.spinner_priority_task, Priority.PRIORITIES));
    }

    private void updateWidgets(GTask task) {

        title.setText(task.title);
        Linkify.addLinks(title, Linkify.ALL);
        notes.setText(task.notes);
        Linkify.addLinks(notes, Linkify.ALL);
        priority.setSelection(task.priority);
        if (task.dueDate > 0) {
            dueDate.setTimeInMillis(task.dueDate);
            dueDateDay.setText(Dates.formatDate(dueDate));
            dueDateTime.setText(String.format(TIME_FORMAT_STRING, dueDate.get(Calendar.HOUR_OF_DAY),
                    dueDate.get(Calendar.MINUTE)));
        }
    }

    private static final String EMPTY_STRING = "";

    private GTask createEmptyTask() {

        GTask task = new GTask();
        task.title = "";
        task.googleId = "";
        task.accountName = CACHE.accountName();
        task.notes = EMPTY_STRING;
        task.priority = 0;
        task.level = 0;
        task.updated = System.currentTimeMillis();
        task.isDeleted = false;
        return task;
    }

    @Override
    public void onReminderDateChanged(Calendar reminderDate) {

        this.reminderDate = reminderDate;
    }

    @Override
    public void onReminderPlaceChanged(double latitude, double longitude, CharSequence locationTitle) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.locationTitle = locationTitle;
    }
}
