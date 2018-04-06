package com.danielebufarini.reminders2.synchronisation;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.database.Tables;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.util.ApplicationCache;

import java.util.ArrayList;
import java.util.List;

public class LoadItemsFromDB implements LoadItemsFromResource {
    public static final String LOGTAG = LoadItemsFromDB.class.getSimpleName();
    private DatabaseHelper dbHelper;
    private final String accountName;

    public LoadItemsFromDB(final Context context, String accountName) {

//        this.dbHelper = new DatabaseHelper(context);
        this.accountName = accountName;
    }

    @Override
    public List<GTask> loadTasks(GTaskList list) {

        List<GTask> tasks = ApplicationCache.INSTANCE.getDatabase().taskDao()
                .loadAllTasksByListId(list.id, accountName);
        tasks.forEach(task -> {
            task.isStored = true;
            task.setList(list);
        });
        return tasks;
    }

    public List<GTask> loadTasks1(GTaskList list) {

        List<GTask> gtasks = new ArrayList<>(20);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                String.format("select * from %s where %s=%s and %s=\"%s\"", Tables.TASK_TABLE,
                        Tables.LIST_ID, list.id, Tables.ACCOUNT_NAME, accountName), null)) {
            while (cursor.moveToNext()) {
                GTask gtask = new GTask();
                gtask.id = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.ID));
                gtask.googleId = cursor.getString(cursor.getColumnIndexOrThrow(Tables.GTASK_ID));
                gtask.title = cursor.getString(cursor.getColumnIndexOrThrow(Tables.TITLE));
                gtask.updated = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.UPDATED));
                gtask.notes = cursor.getString(cursor.getColumnIndexOrThrow(Tables.NOTES));
                gtask.completed = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.COMPLETED));
                gtask.dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.DUE_DATE));
                gtask.reminderDate = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.REMINDER_DATE));
                gtask.reminderInterval = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.REMINDER_INTERVAL));
                gtask.isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.DELETED)) != 0;
                gtask.isMerged = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.MERGED)) != 0;
                gtask.accountName = cursor.getString(cursor.getColumnIndexOrThrow(Tables.ACCOUNT_NAME));
                gtask.priority = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.PRIORITY));
                gtask.latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(Tables.REMINDER_LOCATION_LAT));
                gtask.longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(Tables.REMINDER_LOCATION_LNG));
                gtask.locationTitle = cursor.getString(cursor.getColumnIndexOrThrow(Tables.REMINDER_LOCATION_TITLE));
                gtask.isStored = true;
                gtask.setList(list);
                gtasks.add(gtask);
                Log.d(LOGTAG, "db :: downloaded task " + gtask + " for list " + list);
            }
        } finally {
            db.close();
        }
        return gtasks;
    }

    @Override
    public List<GTaskList> loadLists() {

        List<GTaskList> lists = ApplicationCache.INSTANCE.getDatabase().listDao().loadLists(accountName);
        lists.forEach(list -> list.isStored = true);
        return lists;
    }

    public List<GTaskList> loadLists1() {

        List<GTaskList> dbItems = new ArrayList<>(30);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                String.format("select * from %s where %s=\"%s\"", Tables.LIST_TABLE,
                        Tables.ACCOUNT_NAME, accountName),
                null
        )) {
            while (cursor.moveToNext()) {
                GTaskList list = new GTaskList();
                list.id = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.ID));
                list.title = cursor.getString(cursor.getColumnIndexOrThrow(Tables.TITLE));
                list.updated = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.UPDATED));
                list.googleId = cursor.getString(cursor.getColumnIndexOrThrow(Tables.GTASK_ID));
                list.isMerged = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.MERGED)) != 0;
                list.isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.DELETED)) != 0;
                list.accountName = cursor.getString(cursor.getColumnIndexOrThrow(Tables.ACCOUNT_NAME));
                list.isHideCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.HIDE_COMPLETED)) != 0;
                list.isSortedByDueDate = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.SORT_BY_DUE_DATE)) != 0;
                list.isStored = true;
                list.tasks = loadTasks(list);
                dbItems.add(list);
                Log.d(LOGTAG, "db :: loaded list " + list);
            }
            Log.d(LOGTAG, "db :: loaded '" + dbItems.size() + "' items.");
        } catch (Exception e) {
            Log.e(LOGTAG, "", e);
        }
        return dbItems;
    }
}