package com.danielebufarini.reminders2.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import com.danielebufarini.reminders2.model.GTask;

import java.util.List;

@Dao
public interface RemindersDao {

    @Query("select * from tasks where listId = :listId and account_name = :accountName")
    List<GTask> loadAllTasksByListId(long listId, String accountName);
}
