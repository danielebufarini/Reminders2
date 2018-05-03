package com.danielebufarini.reminders2.synchronisation.commands;

import com.danielebufarini.reminders2.model.Item;

public class DeleteFromDB implements Command {
    private Item item;

    public DeleteFromDB(Item item) {

        this.item = item;
    }

    @Override
    public void execute() {

        item.delete();
    }
}
