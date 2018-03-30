package com.danielebufarini.reminders2.synchronisation.commands;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.Item;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;

public class InsertInGoogleTaskAndMergeInDB implements Command {
	private static final String TAG = InsertInGoogleTaskAndMergeInDB.class.getSimpleName();
	private DatabaseHelper dbHelper;
	private Tasks googleService;
	private Item item;
	
	public InsertInGoogleTaskAndMergeInDB(DatabaseHelper dbHelper, Tasks googleService, Item item) {
		this.dbHelper = dbHelper;
		this.googleService = googleService;
		this.item = item;
	}

	@Override
	public void execute() {
		try {
			item.isMerged = true;
			item.insert(googleService);
		} catch (IOException e) {
			item.isMerged = false;
			Log.w(TAG, "", e);
		}
		SQLiteDatabase db = null;
		try {
			if (item.isStored && (item.isMerged || item.isModified)) {
				db = dbHelper.getWritableDatabase();
				item.merge(db);
			}
		} finally {
			if (db != null)
				db.close();
		}
	}
}
