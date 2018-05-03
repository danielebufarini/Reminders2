package com.danielebufarini.reminders2.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.danielebufarini.reminders2.model.GTaskList;

import java.util.List;

@Dao
public interface ListDao {

    @Query("select * from list where accountName = :accountName")
    List<GTaskList> loadLists(String accountName);

    @Insert
    void insert(GTaskList list);

    @Delete
    void delete(GTaskList list);

    @Update
    void update(GTaskList list);

}
