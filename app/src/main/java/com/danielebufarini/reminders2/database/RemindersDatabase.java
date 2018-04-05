package com.danielebufarini.reminders2.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;

@Database(version = 1, entities = {GTask.class, GTaskList.class})
abstract class RemindersDatabase extends RoomDatabase {

    abstract public RemindersDao remindersDao();
}
