package com.danielebufarini.reminders2.ui.task;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.Priority;
import com.danielebufarini.reminders2.util.Dates;
import com.danielebufarini.reminders2.util.Notifications;

import java.util.Calendar;
import java.util.List;

public class TaskAdapter extends ArrayAdapter<GTask> {
    private static Calendar calendar = Calendar.getInstance();
    private Context context;

    private static class ViewHolder {
        TextView title;
        CheckBox completed;
        TextView dueDate;
        ImageView reminder;
        ImageView recurring;
        TextView priority;
        //DragGripView handle;
        //DragGripView noHandle;
    }

    public TaskAdapter(Context context, int resource, List<GTask> items) {
        super(context, resource, items);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder taskViewHolder;
        if (convertView == null) {
            LinearLayout rowView = new LinearLayout(context);
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            li.inflate(R.layout.task, rowView, true);
            rowView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            taskViewHolder = new ViewHolder();
            taskViewHolder.title = (TextView) rowView.findViewById(R.id.item);
            taskViewHolder.completed = (CheckBox) rowView.findViewById(R.id.item_check);
            taskViewHolder.dueDate = (TextView) rowView.findViewById(R.id.dueDate);
            taskViewHolder.reminder = (ImageView) rowView.findViewById(R.id.reminder);
            taskViewHolder.recurring = (ImageView) rowView.findViewById(R.id.recurring);
            taskViewHolder.priority = (TextView) rowView.findViewById(R.id.item_priority);
            //taskViewHolder.handle = (DragGripView) rowView.findViewById(R.id.drag_handle);
            //taskViewHolder.noHandle = (DragGripView) rowView.findViewById(R.id.no_handle);
            convertView = rowView;
            convertView.setTag(taskViewHolder);
		} else
			taskViewHolder = (ViewHolder) convertView.getTag();
        final GTask task = getItem(position);
        if (task.reminderDate < System.currentTimeMillis() && task.reminderInterval == 0)
            task.reminderDate = 0;
        boolean isChecked = task.completed > 0;
        taskViewHolder.title.setText(task.title);
        Linkify.addLinks(taskViewHolder.title, Linkify.ALL);
        if (isChecked)
            taskViewHolder.title.setPaintFlags(taskViewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else
            taskViewHolder.title.setPaintFlags(taskViewHolder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        taskViewHolder.completed.setOnCheckedChangeListener(null);
        taskViewHolder.completed.setChecked(isChecked);
        taskViewHolder.completed.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked)
                            Notifications.cancelReminder(context, task);
                        else if (task.reminderDate > 0)
                            Notifications.setReminder(context, task);
                        task.completed = isChecked ? System.currentTimeMillis() : 0;
                        task.isModified = true;
                        task.updated = System.currentTimeMillis();
                        notifyDataSetChanged();
                    }
                }
        );
        if (task.dueDate > 0) {
            calendar.setTimeInMillis(task.dueDate);
            taskViewHolder.dueDate.setTextColor(Dates.isInThePast2(calendar) ?
                    context.getResources().getColor(android.R.color.holo_red_dark) :
                    context.getResources().getColor(android.R.color.black));
            taskViewHolder.dueDate.setText(Dates.formatDate(calendar));
        } else
            taskViewHolder.dueDate.setText("");
        taskViewHolder.reminder.setVisibility(task.reminderDate > 0 ? View.VISIBLE : View.INVISIBLE);
        taskViewHolder.recurring.setVisibility(task.reminderInterval > 0 ? View.VISIBLE : View.INVISIBLE);
        if (task.priority > 0 && task.priority < Priority.PRIORITIES.length) {
            GradientDrawable shape = (GradientDrawable) context.getResources().getDrawable(R.drawable.coloured_box);
            shape.setColor(Priority.getColourForValue(task.priority));
            taskViewHolder.priority.setBackgroundDrawable(shape);
            taskViewHolder.priority.setVisibility(View.VISIBLE);
            taskViewHolder.priority.setText(String.valueOf(task.priority));
            taskViewHolder.priority.setTextColor(Color.WHITE);
            taskViewHolder.priority.setTextSize(14.0F);
            taskViewHolder.priority.setTypeface(null, Typeface.BOLD_ITALIC);
            taskViewHolder.priority.setPadding(8, 40, 8, 8);
        } else
            taskViewHolder.priority.setVisibility(View.GONE);
        //taskViewHolder.handle.setVisibility(task.list.isSortedByDueDate ? View.GONE : View.VISIBLE);
        //taskViewHolder.noHandle.setVisibility(task.list.isSortedByDueDate ? View.VISIBLE : View.GONE);
        return convertView;
    }
}
