package com.danielebufarini.reminders2.synchronisation.commands;

import com.danielebufarini.reminders2.model.Item;

public class MergeInDB extends DBCommand {
    public MergeInDB(Item item) {

        super(item);
    }

    @Override
    public void doExecute(Item item) {

        item.merge();
    }
}
