package com.danielebufarini.reminders2.synchronisation.commands;

import android.database.sqlite.SQLiteDatabase;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.Item;

public class InsertInDB extends DBCommand {
	public InsertInDB(DatabaseHelper database, Item item) {
		super(database, item);
	}

	@Override
	public void doExecute(SQLiteDatabase db, Item item) {
        item.insert(db);
        item.isStored = true;
    }
}
