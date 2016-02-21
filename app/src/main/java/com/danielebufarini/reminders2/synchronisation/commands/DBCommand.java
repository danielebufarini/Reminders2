package com.danielebufarini.reminders2.synchronisation.commands;

import android.database.sqlite.SQLiteDatabase;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.Item;

public abstract class DBCommand implements Command {
	private DatabaseHelper dbHelper;
	private Item item;

	public DBCommand(DatabaseHelper dbHelper, Item item) {
		this.dbHelper = dbHelper;
		this.item = item;
	}

	@Override
	final public void execute() {
		try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
			doExecute(db, item);
		}
	}

	public abstract void doExecute(SQLiteDatabase db, Item item);
}
