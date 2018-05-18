package com.danielebufarini.reminders2.ui;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.services.NotificationUtils;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class NotificationActivity extends Activity {
    private static final long TEN_MINUTES = 10 * 60 * 1000L;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        final Bundle extras = intent.getExtras();
        finish();
        if (extras != null) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String action = intent.getStringExtra("notificaton action");
            String notificationId = intent.getExtras().getString(NotificationUtils.ID);
            if ("snooze".equals(action)) {
                final String title = extras.getString(NotificationUtils.TITLE);
                final long dueDateInMillis = extras.getLong(NotificationUtils.DUE_DATE);
                notificationManager.cancel(NotificationUtils.getNotificationId(notificationId));
                NotificationUtils.setReminder(
                        getBaseContext(),
                        notificationId,
                        title,
                        dueDateInMillis,
                        extras.getLong(NotificationUtils.LIST_ID),
                        System.currentTimeMillis() + TEN_MINUTES,
                        0
                );
            } else if ("done".equals(action)) {
                GTask task = (GTask) intent.getSerializableExtra("task");
                NotificationUtils.cancelReminder(this, task);
                notificationManager.cancel(NotificationUtils.getNotificationId(notificationId));
                if (task != null) {
                    Intent resultIntent = new Intent(this, Reminders.class);
                    resultIntent.putExtra("action", "refresh list if active");
                    resultIntent.putExtra("task", task);
                    resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(resultIntent);

                }
            }
        }
    }
}
