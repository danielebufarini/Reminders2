package com.danielebufarini.reminders2.synchronisation.commands;

import java.io.IOException;

import com.danielebufarini.reminders2.model.Item;
import com.google.api.services.tasks.Tasks;

public class DeleteFromGoogleTaskAndFromDB implements Command {
    private final Tasks googleService;
    private final Item item;

    public DeleteFromGoogleTaskAndFromDB(Tasks googleService, Item item) {

        this.googleService = googleService;
        this.item = item;
    }

    @Override
    public void execute() {

        try {
            if (item.getGoogleId() != null && item.getGoogleId().length() > 0)
                item.delete(googleService);
            if (item.isStored)
                item.delete();
        } catch (IOException e) {
            if (item.isStored)
                item.merge();
        }
    }
}
