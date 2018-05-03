package com.danielebufarini.reminders2.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.util.ApplicationCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.danielebufarini.reminders2.ui.Reminders.LOGV;

public class NotificationUtils {
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String DUE_DATE = "dueDate";
    public static final String LIST_ID = "listId";

    private static final Map<Long, Integer> NOTIFICATION_ID = new ConcurrentHashMap<>(100);
    private static final ApplicationCache CAHCE = ApplicationCache.INSTANCE;
    private static final String TAG = NotificationUtils.class.getSimpleName();

    static {
        CAHCE.setAtomicIntValue(0);
    }

    public static int getNotificationId(long id) {

        Integer newId = NOTIFICATION_ID.get(id);
        if (newId == null) {
            synchronized (CAHCE) {
                newId = NOTIFICATION_ID.get(id);
                if (newId == null) {
                    newId = CAHCE.getNextInt();
                    NOTIFICATION_ID.put(id, newId);
                }
            }
        }
        return newId;
    }

    public static void setReminder(Context context, long id, String title, long dueDate,
                                   long listId, long reminderDate, long reminderInterval) {

        String actionName = context.getResources().getString(R.string.intent_action_alarm);
        Intent alarmIntent = new Intent(actionName);
        alarmIntent.putExtra(NotificationUtils.ID, id);
        alarmIntent.putExtra(NotificationUtils.TITLE, title);
        alarmIntent.putExtra(NotificationUtils.DUE_DATE, dueDate);
        alarmIntent.putExtra(NotificationUtils.LIST_ID, listId);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, getNotificationId(id), alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (reminderInterval > 0) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminderDate,
                    reminderInterval, alarmPendingIntent);
            if (LOGV) Log.v(TAG, "TaskDetailDialog set recurring reminder at " + dueDate);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderDate, alarmPendingIntent);
            if (LOGV) Log.v(TAG, "TaskDetailDialog set reminder at " + dueDate);
        }
    }

    public static void setReminder(Context context, GTask task) {

        String actionName = context.getResources().getString(R.string.intent_action_alarm);
        Intent alarmIntent = new Intent(actionName);
        alarmIntent.putExtra(NotificationUtils.ID, task.id);
        alarmIntent.putExtra(NotificationUtils.TITLE, task.title);
        alarmIntent.putExtra(NotificationUtils.DUE_DATE, task.dueDate);
        alarmIntent.putExtra(NotificationUtils.LIST_ID, task.getListId());
        alarmIntent.putExtra("task", task);
        PendingIntent alarmPendingIntent =
                PendingIntent.getBroadcast(context, getNotificationId(task.id), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (task.reminderInterval > 0) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, task.reminderDate,
                    task.reminderInterval, alarmPendingIntent);
            if (LOGV) Log.v(TAG, "TaskDetailDialog set recurring reminder at " + task.dueDate);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, task.reminderDate, alarmPendingIntent);
            if (LOGV) Log.v(TAG, "TaskDetailDialog set reminder at " + task.dueDate);
        }
    }

    public static void cancelReminder(Context context, GTask task) {

        if (task != null) {
            String actionName = context.getResources().getString(R.string.intent_action_alarm);
            Intent alarmIntent = new Intent(actionName);
            alarmIntent.putExtra(NotificationUtils.ID, task.id);
            if (task.title != null)
                alarmIntent.putExtra(NotificationUtils.TITLE, task.title);
            alarmIntent.putExtra(NotificationUtils.DUE_DATE, task.dueDate);
            alarmIntent.putExtra(NotificationUtils.LIST_ID, task.getListId());
            alarmIntent.putExtra("task", task);
            PendingIntent alarmPendingIntent =
                    PendingIntent.getBroadcast(context, getNotificationId(task.id), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmPendingIntent);
            alarmPendingIntent.cancel();
            if (LOGV) Log.v(TAG, "TaskDetailDialog deleted reminder set at " + task.dueDate);
        }
    }
}
