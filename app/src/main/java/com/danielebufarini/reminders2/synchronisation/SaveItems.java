package com.danielebufarini.reminders2.synchronisation;

import android.content.Context;

import com.danielebufarini.reminders2.database.DatabaseHelper;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.model.Item;
import com.danielebufarini.reminders2.synchronisation.commands.Command;
import com.danielebufarini.reminders2.synchronisation.commands.DeleteFromDB;
import com.danielebufarini.reminders2.synchronisation.commands.DeleteFromGoogleTaskAndFromDB;
import com.danielebufarini.reminders2.synchronisation.commands.InsertInDB;
import com.danielebufarini.reminders2.synchronisation.commands.InsertInGoogleTaskAndMergeInDB;
import com.danielebufarini.reminders2.synchronisation.commands.MergeInDB;
import com.danielebufarini.reminders2.synchronisation.commands.MergeInGoogleTask;
import com.danielebufarini.reminders2.util.GoogleService;
import com.google.api.services.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

public class SaveItems implements Runnable {
	private final boolean isSyncWithGoogleEnabled;
	private final List<GTaskList> inMemoryLists;
	private final DatabaseHelper dbHelper;
	private Tasks googleService;

	public SaveItems(Context context, boolean isSyncWithGTasksEnabled,
		String accountName, List<GTaskList> inMemoryLists)
	{
		this.dbHelper = new DatabaseHelper(context);
		this.inMemoryLists = inMemoryLists;//deepCopy(inMemoryLists);
		this.isSyncWithGoogleEnabled = isSyncWithGTasksEnabled;
		if (isSyncWithGoogleEnabled)
            this.googleService = GoogleService.getGoogleTasksService(context, accountName);
	}

    private List<GTaskList> deepCopy(List<GTaskList> lists) {
        List<GTaskList> newList = new ArrayList<GTaskList>(lists.size());
        for (GTaskList list : lists)
            newList.add(new GTaskList(list));
        return newList;
    }

	private List<Command> mergeItems(List<? extends Item> items) {
		List<Command> commands = new ArrayList<Command>(50);

		for (Item item : items) {
			if (item.isDeleted) {
				if (isSyncWithGoogleEnabled)
					commands.add(new DeleteFromGoogleTaskAndFromDB(dbHelper, googleService, item));
				else if (item.isStored) {
					if ("".equals(item.googleId))
						commands.add(new DeleteFromDB(dbHelper, item));
					else
						commands.add(new MergeInDB(dbHelper, item));
				}
			} else {
				boolean isAlreadyMerged = false;
				if (isSyncWithGoogleEnabled) {
					if (item.googleId.equals("")) {
						commands.add(new InsertInGoogleTaskAndMergeInDB(dbHelper, googleService, item));
						isAlreadyMerged = true;
					} else if (item.isModified)
						commands.add(new MergeInGoogleTask(googleService, item));
				}
				if (!item.isStored)
					commands.add(new InsertInDB(dbHelper, item));
				else if (!isAlreadyMerged && item.isModified)
					commands.add(new MergeInDB(dbHelper, item));
			}
			if (item.hasChildren())
				commands.addAll(mergeItems(item.getChildren()));
		}

		return commands;
	}

	@Override
	public void run() {
		List<Command> commands = mergeItems(inMemoryLists);
		for (Command command: commands)
			command.execute();
	}
}
