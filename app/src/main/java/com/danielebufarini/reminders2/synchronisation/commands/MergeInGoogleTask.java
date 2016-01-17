package com.danielebufarini.reminders2.synchronisation.commands;

import com.danielebufarini.reminders2.model.Item;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;

public class MergeInGoogleTask extends GoogleTaskCommand {
	public MergeInGoogleTask(Tasks googleService, Item item) {
		super(googleService, item);
	}

	@Override
	public void doExecute(Tasks googleService, Item item) throws IOException {
		item.merge(googleService);
	}
}
