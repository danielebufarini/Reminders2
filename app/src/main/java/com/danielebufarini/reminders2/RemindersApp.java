package com.danielebufarini.reminders2;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.danielebufarini.reminders2.ui.Reminders;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.danielebufarini.reminders2.util.GoogleAccountHelper;

import static com.danielebufarini.reminders2.ui.Reminders.PREF_ACCOUNT_NAME;
import static com.danielebufarini.reminders2.ui.Reminders.PREF_SYNC_GOOGLE_ENABLED;

public class RemindersApp extends Application {
    private final ApplicationCache store = ApplicationCache.getInstance();

    private String readAccountName(GoogleAccountHelper accountHelper) {
        SharedPreferences settings = getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        String accountName = settings.getString(PREF_ACCOUNT_NAME, accountHelper.getNames()[0]);
        store.accountName(accountName);
        return accountName;
    }

    private boolean isSyncWithGTasksEnabled() {
        SharedPreferences settings = getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        return settings.getBoolean(PREF_SYNC_GOOGLE_ENABLED, true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /*final GoogleAccountHelper accountHelper = new GoogleAccountHelper(this);
        final List<GTaskList> items = new ArrayList<GTaskList>(50);
        new Thread(new Runnable() {
            public void run() {
                LoadItems loadItems = new LoadItems(
                        getApplicationContext(),
                        isSyncWithGTasksEnabled(),
                        readAccountName(accountHelper));
                items.addAll(loadItems.getLists());
                store.setFolders(items);
            }
        }).start();*/
    }
}
