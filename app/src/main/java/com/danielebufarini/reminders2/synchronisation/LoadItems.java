package com.danielebufarini.reminders2.synchronisation;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.model.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.danielebufarini.reminders2.model.Item.generateId;
import static com.danielebufarini.reminders2.util.GoogleService.isNetworkAvailable;


public class LoadItems implements Callable<List<GTaskList>> {
    private final boolean isSyncWithGTasksEnabled;
    private final Context context;
    private final String accountName;
    private final LoadItemsFromDB itemsFromDB;
    private final LoadItemsFromGoogle itemsFromGoogle;

    public LoadItems(Context context, boolean isSyncWithGTasksEnabled, String accountName) {
        this.isSyncWithGTasksEnabled = isSyncWithGTasksEnabled;
        this.context = context;
        this.accountName = accountName;
        itemsFromDB = new LoadItemsFromDB(context, accountName);
        itemsFromGoogle = new LoadItemsFromGoogle(context, accountName);
    }

    private <T extends Item> void reconcile(List<T> dbItems, List<T> googleItems) {
        if (dbItems.isEmpty()) {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            try {
                for (T item : googleItems) {
                    item.id = generateId();
                    item.insert(db);
                    dbItems.add(item);
                }
            } finally {
                db.close();
            }
        } else {
            for (T dbItem : dbItems)
                for (T googleItem : googleItems)
                    if (dbItem.googleId.equals(googleItem.googleId)) {
                        googleItem.id = dbItem.id;
                        googleItem.isStored = dbItem.isStored;
                        if (dbItem.hasChildren())
                            reconcile(dbItem.getChildren(), googleItem.getChildren());
                        break;
                    }
        }
    }

    private <T extends Item> List<T> mergeItems(List<T> dbItems, List<T> googleItems) {
        List<T> result = new ArrayList<>(30);

        for (T dbItem : dbItems) {
            if (googleItems.contains(dbItem)) {
                T googleItem = googleItems.get(googleItems.indexOf(dbItem));
                List<Item> mergedItems = null;
                if (dbItem.hasChildren())
                    mergedItems = mergeItems(dbItem.getChildren(), googleItem.getChildren());
                if (dbItem.updated < googleItem.updated) {
                    googleItem.id = dbItem.id;
                    googleItem.setReminder(dbItem.getReminder());
                    googleItem.setReminderInterval(dbItem.getReminderInterval());
                    dbItem = googleItem;
                }
                dbItem.setChildren(mergedItems);
            } else {
                if (dbItem.isMerged)
                    dbItem.isDeleted = true;
            }
            result.add(dbItem);
        }
        if (!dbItems.isEmpty())
            for (T googleItem : googleItems) {
                if (result.contains(googleItem))
                    continue;
                if (dbItems.contains(googleItem)) {
                    T dbItem = dbItems.get(dbItems.indexOf(googleItem));
                    List<Item> mergedItems = null;
                    if (googleItem.hasChildren())
                        mergedItems = mergeItems(dbItem.getChildren(), googleItem.getChildren());
                    if (googleItem.updated < dbItem.updated) {
                        dbItem.googleId = googleItem.googleId;
                        googleItem = dbItem;
                    }
                    googleItem.setChildren(mergedItems);
                }
                result.add(googleItem);
            }

        return result;
    }

    public List<GTaskList> getLists() {
        List<GTaskList> tasksFromDB = itemsFromDB.loadLists();
        if (isSyncWithGTasksEnabled && isNetworkAvailable(context)) {
            List<GTaskList> tasksFromGoogle = itemsFromGoogle.loadLists();
            if (!tasksFromGoogle.isEmpty())
                reconcile(tasksFromDB, tasksFromGoogle);
            return mergeItems(tasksFromDB, tasksFromGoogle);
        }
        return tasksFromDB;
    }

    @Override
    public List<GTaskList> call() {
        return getLists();
    }
}
