
package com.danielebufarini.reminders2.synchronisation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.model.Item;

public class TasksLoader implements Callable<List<GTaskList>> {

    private final LocalDatabase localDatabase;
    private final Source        remoteSource;

    public TasksLoader(String accountName, Source remoteSource) {

        localDatabase = new LocalDatabase(accountName);
        this.remoteSource = remoteSource;
    }

    private <T extends Item> void reconcile(List<T> dbItems, List<T> googleItems) {

        if (dbItems.isEmpty()) {
            for (T item : googleItems) {
                item.setMerged(true);
                item.insert();
                item.getChildren().forEach(item1 -> {
                    item1.setMerged(true);
                    item1.insert();
                });
                dbItems.add(item);
            }
        } else {
            // Map<String,T> map = dbItems.stream().collect(Collectors.toMap(Item::getGoogleId, identity()));
            for (T dbItem : dbItems) {
                for (T googleItem : googleItems) {
                    if (dbItem.getGoogleId().equals(googleItem.getGoogleId())) {
                        googleItem.id = dbItem.id;
                        googleItem.setListId(dbItem.getListId());
                        googleItem.isStored = dbItem.isStored;
                        if (dbItem.hasChildren()) {
                            reconcile(dbItem.getChildren(), googleItem.getChildren());
                        }
                        break;
                    }
                }
            }
        }
    }

    private <T extends Item> List<T> merge(List<T> dbItems, List<T> googleItems) {

        List<T> result = new ArrayList<>(30);

        for (T dbItem : dbItems) {
            if (googleItems.contains(dbItem)) {
                T googleItem = googleItems.get(googleItems.indexOf(dbItem));
                List<Item> mergedItems = null;
                if (dbItem.hasChildren()) {
                    if (dbItem.getParentId() == null) {
                        final T gItem = googleItem;
                        googleItem.getChildren().forEach(item -> item.setListId(gItem.getListId()));
                    }
                    mergedItems = merge(dbItem.getChildren(), googleItem.getChildren());
                }
                if (dbItem.updated < googleItem.updated) {
                    googleItem.id = dbItem.id;
                    googleItem.setReminder(dbItem.getReminder());
                    googleItem.setReminderInterval(dbItem.getReminderInterval());
                    dbItem = googleItem;
                }
                dbItem.setChildren(mergedItems);
            } else if (dbItem.isMerged()) {
                dbItem.isDeleted = true;
            }
            result.add(dbItem);
        }
        if (!dbItems.isEmpty()) {
            for (T googleItem : googleItems) {
                if (result.contains(googleItem)) continue;
                if (dbItems.contains(googleItem)) {
                    T dbItem = dbItems.get(dbItems.indexOf(googleItem));
                    List<Item> mergedItems = null;
                    if (googleItem.hasChildren()) {
                        mergedItems = merge(dbItem.getChildren(), googleItem.getChildren());
                    }
                    if (googleItem.updated < dbItem.updated) {
                        dbItem.setGoogleId(googleItem.getGoogleId());
                        googleItem = dbItem;
                    }
                    googleItem.setChildren(mergedItems);
                }
                result.add(googleItem);
            }
        }

        return result;
    }

    public List<GTaskList> getLists() {

        List<GTaskList> tasksFromDB = localDatabase.getLists();
        List<GTaskList> remoteTasks = remoteSource.getLists();
        if (!remoteTasks.isEmpty()) {
            reconcile(tasksFromDB, remoteTasks);
        }
        return merge(tasksFromDB, remoteTasks);
    }

    @Override
    public List<GTaskList> call() {

        return getLists();
    }
}
