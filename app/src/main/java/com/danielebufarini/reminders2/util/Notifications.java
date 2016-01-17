package com.danielebufarini.reminders2.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTask;

import java.util.concurrent.ConcurrentHashMap;

public class Notifications {
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String DUE_DATE = "dueDate";
    public static final String LIST_ID = "listId";

    private static final ConcurrentHashMap<Long, Integer> NOTIFICATION_ID = new ConcurrentHashMap<Long, Integer>(100);
    private static final ApplicationCache MEMORY_STORE = ApplicationCache.getInstance();

    static {
        MEMORY_STORE.setAtomicIntValue(0);
    }

    public static int getNotificationId(long id) {
        Integer newId = NOTIFICATION_ID.get(id);
        if (newId == null) {
            synchronized (MEMORY_STORE) {
                newId = NOTIFICATION_ID.get(id);
                if (newId == null) {
                    newId = MEMORY_STORE.getNextInt();
                    NOTIFICATION_ID.put(id, newId);
                }
            }
        }
        return newId;
    }

    public static void setReminder(Context context, GTask task)
    {
        String actionName = context.getResources().getString(R.string.intent_action_alarm);
        Intent alarmIntent = new Intent(actionName);
        alarmIntent.putExtra(Notifications.ID, task.id);
        alarmIntent.putExtra(Notifications.TITLE, task.title);
        alarmIntent.putExtra(Notifications.DUE_DATE, task.dueDate);
        alarmIntent.putExtra(Notifications.LIST_ID, task.list.id);
        alarmIntent.putExtra("task", task);
        PendingIntent alarmPendingIntent =
                PendingIntent.getBroadcast(context, getNotificationId(task.id), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (task.reminderInterval > 0) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, task.reminderDate,
                    task.reminderInterval, alarmPendingIntent);
        } else
            alarmManager.set(AlarmManager.RTC_WAKEUP, task.reminderDate, alarmPendingIntent);
    }

    public static void cancelReminder(Context context, GTask task) {
        if (task != null) {
            String actionName = context.getResources().getString(R.string.intent_action_alarm);
            Intent alarmIntent = new Intent(actionName);
            alarmIntent.putExtra(Notifications.ID, task.id);
            if (task.title != null)
                alarmIntent.putExtra(Notifications.TITLE, task.title);
            alarmIntent.putExtra(Notifications.DUE_DATE, task.dueDate);
            alarmIntent.putExtra(Notifications.LIST_ID, task.list.id);
            alarmIntent.putExtra("task", task);
            PendingIntent alarmPendingIntent =
                    PendingIntent.getBroadcast(context, getNotificationId(task.id), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmPendingIntent);
            alarmPendingIntent.cancel();
        }
    }
}
