package com.danielebufarini.reminders2.synchronisation.commands;

import android.util.Log;

import com.danielebufarini.reminders2.model.Item;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;

abstract class GoogleTaskCommand implements Command {
	private static final String LOGTAG = GoogleTaskCommand.class.getSimpleName();
	private Tasks googleService;
	private Item item;

	public GoogleTaskCommand(Tasks googleService, Item item) {
		this.googleService = googleService;
		this.item = item;
	}

	@Override
	final public void execute() {
		try {
			doExecute(googleService, item);
		} catch (IOException e) {
			item.updated = System.currentTimeMillis(); // force update for the next sync
			Log.e(LOGTAG, "error executing command for " + item, e);
		}
	}

	abstract public void doExecute(Tasks googleService, Item item) throws IOException;
}