package com.danielebufarini.reminders2.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Tables {
    public static final String TASK_TABLE = "tasks";
    public static final String ID = "_id";
    public static final String TITLE = "title";
    public static final String UPDATED = "updated_date";
    public static final String DELETED = "deleted";
    public static final String GTASK_ID = "gtask_id";
    public static final String CATEGORY = "category";
    public static final String NOTES = "notes";
    public static final String COMPLETED = "completed";
    public static final String PRIORITY = "priority";
    public static final String LEVEL = "level";
    public static final String DUE_DATE = "due_date";
    public static final String REMINDER_DATE = "reminder_date";
    public static final String REMINDER_INTERVAL = "reminder_interval";
    public static final String REMINDER_LOCATION_LAT = "reminder_location_lat";
    public static final String REMINDER_LOCATION_LNG = "reminder_location_lng";
    public static final String REMINDER_LOCATION_RADIUS = "reminder_location_radius";
    public static final String REMINDER_LOCATION_TITLE = "reminder_location_title";
    public static final String MERGED = "merged";
    public static final String LIST_ID = "list_id";
    public static final String ACCOUNT_NAME = "account_name";
    public static final String HIDE_COMPLETED = "hide_completed";
    public static final String SORT_BY_DUE_DATE = "sort_by_date";

    private static final String GTASKS_SYNCHRONISATION_COLUMNS =
            ID + " integer primary key autoincrement,"
                    + TITLE + " text not null,"
                    + UPDATED + " integer not null,"
                    + GTASK_ID + " text not null,"
                    + MERGED + " integer not null,"
                    + DELETED + " tinyint not null,"
                    + ACCOUNT_NAME + " text";

    private static final String CREATE_TASK_TABLE =
            "create table " + TASK_TABLE
                    + "("
                    + GTASKS_SYNCHRONISATION_COLUMNS + ","
                    + CATEGORY + " text null,"
                    + NOTES + " text null,"
                    + COMPLETED + " integer not null,"
                    + PRIORITY + " tinyint not null,"
                    + LEVEL + " integer not null,"
                    + DUE_DATE + " integer not null,"
                    + REMINDER_DATE + " integer,"
                    + REMINDER_INTERVAL + " integer,"
                    + REMINDER_LOCATION_LAT + " real,"
                    + REMINDER_LOCATION_LNG + " real,"
                    + REMINDER_LOCATION_RADIUS + " real,"
                    + REMINDER_LOCATION_TITLE + " text,"
                    + LIST_ID + " integer not null"
                    + ");";

    public static final String LIST_TABLE = "task_lists";
    private static final String CREATE_LIST_TABLE =
            "create table " + LIST_TABLE
                    + "("
                    + GTASKS_SYNCHRONISATION_COLUMNS + ","
                    + HIDE_COMPLETED + " integer null,"
                    + SORT_BY_DUE_DATE + " integer null"
                    + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TASK_TABLE);
        database.execSQL(CREATE_LIST_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(Tables.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE);
        onCreate(database);
    }

    private void upgradeDB() {
        // TODO add upgrade logic
    }
}
