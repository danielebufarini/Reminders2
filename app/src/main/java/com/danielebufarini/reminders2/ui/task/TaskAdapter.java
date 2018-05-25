
package com.danielebufarini.reminders2.ui.task;

import static com.danielebufarini.reminders2.ui.TasksFragment.TASK;
import static com.danielebufarini.reminders2.ui.TasksFragment.TASK_POSITION;

import java.util.Calendar;
import java.util.List;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.Priority;
import com.danielebufarini.reminders2.ui.Reminders;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.danielebufarini.reminders2.util.Dates;
import com.danielebufarini.reminders2.util.Notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class TaskAdapter extends ArrayAdapter<GTask> {

    private static Calendar calendar = Calendar.getInstance();
    private final Activity  context;
    private final Callback  callback;

    private static class ViewHolder {

        View      divider;
        TextView  padding;
        TextView  title;
        CheckBox  completed;
        TextView  dueDate;
        ImageView reminder;
        ImageView recurring;
        TextView  priority;
        ImageView insertSubtask;
        EditText  subtask;
        // DragGripView handle;
        // DragGripView noHandle;
    }

    public TaskAdapter(Activity context, int resource, List<GTask> tasks, Callback callback) {

        super(context, resource, tasks);
        this.context = context;
        this.callback = callback;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate(R.layout.task, null, true);
            viewHolder = new ViewHolder();
            viewHolder.divider = rowView.findViewById(R.id.divider);
            viewHolder.padding = rowView.findViewById(R.id.padding);
            viewHolder.title = rowView.findViewById(R.id.item);
            viewHolder.completed = rowView.findViewById(R.id.item_check);
            viewHolder.dueDate = rowView.findViewById(R.id.dueDate);
            viewHolder.reminder = rowView.findViewById(R.id.reminder);
            viewHolder.recurring = rowView.findViewById(R.id.recurring);
            viewHolder.priority = rowView.findViewById(R.id.item_priority);
            viewHolder.insertSubtask = rowView.findViewById(R.id.insert_subtask);
            viewHolder.subtask = rowView.findViewById(R.id.subtask);
            // viewHolder.handle = (DragGripView) rowView.findViewById(R.id.drag_handle);
            // viewHolder.noHandle = (DragGripView) rowView.findViewById(R.id.no_handle);
            convertView = rowView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final GTask task = getItem(position);
        convertView.setOnClickListener(view -> {
            Intent viewTask = new Intent(context, ManageTaskActivity.class);
            viewTask.putExtra(TASK, task);
            viewTask.putExtra(TASK_POSITION, position);
            context.startActivityForResult(viewTask, Reminders.REFRESH_TASKS);
        });
        viewHolder.divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        if (task.reminderDate < System.currentTimeMillis() && task.reminderInterval == 0) {
            task.reminderDate = 0;
        }
        boolean isChecked = task.completed > 0;
        viewHolder.padding.setVisibility(hasParentId(task) ? View.VISIBLE : View.GONE);
        viewHolder.insertSubtask.setVisibility(hasParentId(task) ? View.GONE : View.VISIBLE);
        viewHolder.insertSubtask.setOnClickListener(view -> {
            String subtaskTitle = viewHolder.subtask.getText().toString();
            if (!subtaskTitle.isEmpty()) {
                ApplicationCache cache = ApplicationCache.INSTANCE;
                createNewSubtask(task, subtaskTitle);
                viewHolder.subtask.setText("");
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                callback.onListSelected(cache.getActiveList().tasks);
            }
        });
        viewHolder.subtask.setVisibility(hasParentId(task) ? View.GONE : View.VISIBLE);
        viewHolder.title.setText(task.title);
        Linkify.addLinks(viewHolder.title, Linkify.ALL);
        if (isChecked) {
            viewHolder.title.setPaintFlags(viewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            viewHolder.title.setPaintFlags(viewHolder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        viewHolder.completed.setOnCheckedChangeListener(null);
        viewHolder.completed.setChecked(isChecked);
        viewHolder.completed.setOnCheckedChangeListener((buttonView, isChecked1) -> {
            if (isChecked1) {
                Notifications.cancelReminder(context, task);
            } else if (task.reminderDate > 0) {
                Notifications.setReminder(context, task);
            }
            task.completed = isChecked1 ? System.currentTimeMillis() : 0;
            task.isModified = true;
            task.updated = System.currentTimeMillis();
            notifyDataSetChanged();
        });
        if (task.dueDate > 0) {
            calendar.setTimeInMillis(task.dueDate);
            viewHolder.dueDate.setTextColor(
                    Dates.isInThePast2(calendar) ? context.getResources().getColor(android.R.color.holo_red_dark)
                            : context.getResources().getColor(android.R.color.black));
            viewHolder.dueDate.setText(Dates.formatDate(calendar));
        } else {
            viewHolder.dueDate.setText("");
        }
        viewHolder.reminder.setVisibility(task.reminderDate > 0 ? View.VISIBLE : View.INVISIBLE);
        viewHolder.recurring.setVisibility(task.reminderInterval > 0 ? View.VISIBLE : View.INVISIBLE);
        if (task.priority > 0 && task.priority < Priority.PRIORITIES.length) {
            GradientDrawable shape = (GradientDrawable) context.getResources().getDrawable(R.drawable.coloured_box);
            shape.setColor(Priority.getColourForValue(task.priority));
            viewHolder.priority.setBackgroundDrawable(shape);
            viewHolder.priority.setVisibility(View.VISIBLE);
            viewHolder.priority.setText(String.valueOf(task.priority));
            viewHolder.priority.setTextColor(Color.WHITE);
            viewHolder.priority.setTextSize(14.0F);
            viewHolder.priority.setTypeface(null, Typeface.BOLD_ITALIC);
            viewHolder.priority.setPadding(8, 40, 8, 8);
        } else {
            viewHolder.priority.setVisibility(View.GONE);
        }
        // viewHolder.handle.setVisibility(task.list.isSortedByDueDate ? View.GONE : View.VISIBLE);
        // viewHolder.noHandle.setVisibility(task.list.isSortedByDueDate ? View.VISIBLE : View.GONE);
        return convertView;
    }

    private void createNewSubtask(GTask task, String subtaskTitle) {

        ApplicationCache cache = ApplicationCache.INSTANCE;
        GTask subtask = new GTask();
        if (cache.isSyncWithGTasksEnabled()) {
            subtask.setGoogleId(subtask.id);
        }
        subtask.title = subtaskTitle;
        subtask.setParentId(task.id);
        subtask.updated = System.currentTimeMillis();
        subtask.setListId(task.getListId());
        subtask.accountName = task.accountName;
        subtask.insert();
        cache.getActiveList().tasks.add(subtask);
    }

    private boolean hasParentId(GTask task) {

        return task.getParentId() != null && !task.getParentId().isEmpty();
    }

    public interface Callback {

        void onListSelected(List<GTask> tasks);
    }
}
