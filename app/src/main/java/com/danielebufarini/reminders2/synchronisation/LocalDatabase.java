
package com.danielebufarini.reminders2.synchronisation;

import java.util.List;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.util.ApplicationCache;

public class LocalDatabase implements Source {

    public static final String LOGTAG = LocalDatabase.class.getSimpleName();
    private final String       accountName;

    public LocalDatabase(String accountName) {

        this.accountName = accountName;
    }

    @Override
    public List<GTask> getTasks(GTaskList list) {

        List<GTask> tasks = ApplicationCache.INSTANCE.getDatabase().taskDao().loadAllTasksByListId(list.id,
                accountName);
        tasks.forEach(task -> task.isStored = true);
        return tasks;
    }

    @Override
    public List<GTaskList> getLists() {

        List<GTaskList> lists = ApplicationCache.INSTANCE.getDatabase().listDao().loadLists(accountName);
        lists.forEach(list -> {
            list.isStored = true;
            list.tasks = getTasks(list);
        });
        return lists;
    }
}