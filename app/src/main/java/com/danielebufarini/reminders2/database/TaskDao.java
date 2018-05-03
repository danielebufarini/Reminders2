package com.danielebufarini.reminders2.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.danielebufarini.reminders2.model.GTask;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("select * from task where listId = :listId and accountName = :accountName")
    List<GTask> loadAllTasksByListId(long listId, String accountName);

    @Insert
    void insert(GTask task);

    @Delete
    void delete(GTask task);

    @Update
    void update(GTask task);

}
