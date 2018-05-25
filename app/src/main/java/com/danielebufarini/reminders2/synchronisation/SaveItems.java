
package com.danielebufarini.reminders2.synchronisation;

import java.util.ArrayList;
import java.util.List;

import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.model.Item;
import com.danielebufarini.reminders2.synchronisation.commands.Command;
import com.danielebufarini.reminders2.synchronisation.commands.DeleteFromDB;
import com.danielebufarini.reminders2.synchronisation.commands.InsertInDB;
import com.danielebufarini.reminders2.synchronisation.commands.MergeInDB;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.drive.DriveResourceClient;

public class SaveItems implements Runnable {

    private static transient final ObjectMapper     MAPPER = new ObjectMapper();
    private static transient final String           TAG    = SaveItems.class.getSimpleName();
    private static transient final ApplicationCache CACHE  = ApplicationCache.INSTANCE;

    private final boolean                           isSyncWithGoogleEnabled;
    private final List<GTaskList>                   lists;

    public SaveItems(DriveResourceClient driveResourceClient, List<GTaskList> lists) {

        this.lists = lists;
        this.isSyncWithGoogleEnabled = CACHE.isSyncWithGTasksEnabled();
    }

    private List<Command> mergeItems(List<? extends Item> items) {

        List<Command> commands = new ArrayList<>(50);

        for (Item item : items) {
            if (item.isDeleted) {
                if (isSyncWithGoogleEnabled) {
                    commands.add(new DeleteFromDB(item));
                } else if (item.isStored) {
                    if ("".equals(item.getGoogleId())) {
                        commands.add(new DeleteFromDB(item));
                    } else {
                        commands.add(new MergeInDB(item));
                    }
                }
            } else {
                boolean isAlreadyMerged = false;
                if (isSyncWithGoogleEnabled) {
                    if (item.getGoogleId().isEmpty()) {
                        commands.add(new MergeInDB(item));
                        isAlreadyMerged = true;
                    }
                }
                if (!item.isStored) {
                    commands.add(new InsertInDB(item));
                } else if (!isAlreadyMerged && item.isModified) {
                    commands.add(new MergeInDB(item));
                }
            }
            if (item.hasChildren()) {
                commands.addAll(mergeItems(item.getChildren()));
            }
        }

        return commands;
    }

    @Override
    public void run() {

        List<Command> commands = mergeItems(lists);
        for (Command command : commands) {
            command.execute();
        }
    }

    public static class Container {

        public List<GTaskList> lists;

        public Container() {

        }

        public Container(List<GTaskList> lists) {

            this.lists = lists;
        }
    }
}
