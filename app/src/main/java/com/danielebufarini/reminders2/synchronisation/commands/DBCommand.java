package com.danielebufarini.reminders2.synchronisation.commands;

import com.danielebufarini.reminders2.model.Item;

public abstract class DBCommand implements Command {
    private Item item;

    public DBCommand(Item item) {

        this.item = item;
    }

    @Override
    final public void execute() {

        doExecute(item);
    }

    public abstract void doExecute(Item item);
}
