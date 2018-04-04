package com.danielebufarini.reminders2.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.database.Tables;
import com.danielebufarini.reminders2.ui.NotificationActivity;
import com.danielebufarini.reminders2.ui.Reminders;

import static com.danielebufarini.reminders2.ui.Reminders.LOGV;
import static com.danielebufarini.reminders2.ui.Reminders.PREF_ACCOUNT_NAME;

public class AlarmReceiver extends BroadcastReceiver {
    private static final long[] VIBRATE_PATTERN = {0, 250, 0, 250, 0, 500, 0, 500};
    private static final String TAG = AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {

        final String action = intent.getAction();
        if (LOGV) Log.v(TAG, "AlarmInitReceiver \"" + action + "\"");
        final PendingResult result = goAsync();
        final WakeLock wakelock = AlarmAlertWakeLock.createPartialWakeLock(context);
        wakelock.acquire(2000L);
        AsyncHandler.post(() -> {
            try {
                String alarmActionName = context.getResources().getString(R.string.intent_action_alarm);
                if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                    SharedPreferences prefs =
                            context.getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
                    String accountName = prefs.getString(PREF_ACCOUNT_NAME, null);
                    if (LOGV)
                        Log.v(TAG, "AlarmInitReceiver accountName = \"" + accountName + "\"");
                    if (accountName != null && !accountName.equals(""))
                        setReminders(context, accountName);
                } else if (alarmActionName.equals(action)) {
                    showNotification(context, intent);

                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(VIBRATE_PATTERN, 1);
                }
                result.finish();
                if (LOGV) Log.v(TAG, "AlarmInitReceiver finished");
            } finally {
                wakelock.release();
            }
        });
    }

    private void setReminders(Context context, String accountName) {

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                String.format("select %s, %s, %s, %s, %s, %s, %s from %s where %s=\"%s\"",
                        Tables.COMPLETED, Tables.REMINDER_DATE, Tables.REMINDER_INTERVAL, Tables.ID,
                        Tables.DUE_DATE, Tables.LIST_ID, Tables.TITLE,
                        Tables.TASK_TABLE, Tables.ACCOUNT_NAME, accountName
                ),
                null
        );
        try {
            while (cursor.moveToNext()) {
                boolean notCompleted = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.COMPLETED)) == 0;
                if (notCompleted) {
                    long reminderDate = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.REMINDER_DATE));
                    long reminderInterval = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.REMINDER_INTERVAL));
                    if (reminderDate > System.currentTimeMillis() || reminderInterval > 0) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(Tables.TITLE));
                        long dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.DUE_DATE));
                        long listId = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.LIST_ID));
                        NotificationUtils.setReminder(context, id, title, dueDate, listId, reminderDate, reminderInterval);
                    }
                }
            }
        } finally {
            cursor.close();
            db.close();
        }
    }

    private void showNotification(Context context, Intent intent) {

        Intent snoozeIntent = new Intent(context, NotificationActivity.class);
        snoozeIntent.putExtras(intent.getExtras());
        snoozeIntent.putExtra("notificaton action", "snooze");
        PendingIntent snoozePendingIntent =
                PendingIntent.getActivity(context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent doneIntent = new Intent(context, NotificationActivity.class);
        doneIntent.putExtras(intent.getExtras());
        doneIntent.putExtra("notificaton action", "done");
        doneIntent.putExtra("task", intent.getSerializableExtra("task"));
        PendingIntent donePendingIntent =
                PendingIntent.getActivity(context, 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent resultIntent = new Intent(context, Reminders.class);
        resultIntent.putExtra("action", "view task");
        resultIntent.putExtra("task", intent.getSerializableExtra("task"));
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_action_alarms)
                .setContentTitle(context.getResources().getString(R.string.alarmPopupTitle1))
                .setContentIntent(resultPendingIntent)
                .addAction(R.drawable.ic_action_alarms, "Snooze", snoozePendingIntent)
                .addAction(R.drawable.ic_action_done, "Done", donePendingIntent)
                .setContentText(intent.getExtras().getString(NotificationUtils.TITLE));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = NotificationUtils.getNotificationId(intent.getExtras().getLong(NotificationUtils.ID));
        notificationManager.notify(notificationId, builder.build());
    }
}