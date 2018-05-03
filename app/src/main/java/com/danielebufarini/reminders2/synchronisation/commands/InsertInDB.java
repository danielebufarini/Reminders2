package com.danielebufarini.reminders2.synchronisation.commands;

import com.danielebufarini.reminders2.model.Item;

public class InsertInDB extends DBCommand {

    public InsertInDB(Item item) {

        super(item);
    }

    @Override
    public void doExecute(Item item) {

        item.insert();
        item.isStored = true;
    }
}
