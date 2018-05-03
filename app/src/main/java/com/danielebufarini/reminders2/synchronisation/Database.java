package com.danielebufarini.reminders2.synchronisation;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.util.ApplicationCache;

import java.util.List;

public class Database implements Resource {
    public static final String LOGTAG = Database.class.getSimpleName();
    private final String accountName;

    public Database(String accountName) {

        this.accountName = accountName;
    }

    @Override
    public List<GTask> loadTasks(GTaskList list) {

        List<GTask> tasks = ApplicationCache.INSTANCE.getDatabase().taskDao()
                .loadAllTasksByListId(list.id, accountName);
        tasks.forEach(task -> task.isStored = true);
        return tasks;
    }

    @Override
    public List<GTaskList> loadLists() {

        List<GTaskList> lists = ApplicationCache.INSTANCE.getDatabase().listDao().loadLists(accountName);
        lists.forEach(list -> {
            list.isStored = true;
            list.tasks = loadTasks(list);
        });
        return lists;
    }
}