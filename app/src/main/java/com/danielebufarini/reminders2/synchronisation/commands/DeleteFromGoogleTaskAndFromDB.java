package com.danielebufarini.reminders2.synchronisation.commands;

import android.database.sqlite.SQLiteDatabase;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.Item;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;

public class DeleteFromGoogleTaskAndFromDB implements Command {
	private DatabaseHelper dbHelper;
	private Tasks googleService;
	private Item item;
	
	public DeleteFromGoogleTaskAndFromDB(DatabaseHelper dbHelper, Tasks googleService, Item item) {
		this.dbHelper = dbHelper;
		this.googleService = googleService;
		this.item = item;
	}

	@Override
	public void execute() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			if (item.googleId != null && item.googleId.length() > 0)
				item.delete(googleService);
			if (item.isStored)
				item.delete(db);
		} catch (IOException e) {
			if (item.isStored)
				item.merge(db);
		} finally {
			db.close();
		}
	}
}
