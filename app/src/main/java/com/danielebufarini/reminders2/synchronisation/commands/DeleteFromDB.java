package com.danielebufarini.reminders2.synchronisation.commands;

import android.database.sqlite.SQLiteDatabase;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.Item;

public class DeleteFromDB implements Command {
	private DatabaseHelper dbHelper;
	private Item item;
	
	public DeleteFromDB(DatabaseHelper dbHelper, Item item) {
		this.dbHelper = dbHelper;
		this.item = item;
	}

	@Override
	public void execute() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			item.delete(db);
		} finally {
			db.close();
		}
	}
}
