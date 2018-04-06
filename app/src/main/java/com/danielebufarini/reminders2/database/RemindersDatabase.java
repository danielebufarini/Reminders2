package com.danielebufarini.reminders2.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;

@Database(version = 1, entities = {GTask.class, GTaskList.class})
public abstract class RemindersDatabase extends RoomDatabase {

    public static final String NAME = "reminders.db";

    public abstract TaskDao taskDao();
    public abstract ListDao listDao();
}
