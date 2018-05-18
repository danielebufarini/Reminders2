package com.danielebufarini.reminders2.synchronisation;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.util.ApplicationCache;

public class LocalDatabase implements Source {
    public static final String LOGTAG = LocalDatabase.class.getSimpleName();
    private final String accountName;

    public LocalDatabase(String accountName) {

        this.accountName = accountName;
    }

    @Override
    public List<GTask> getTasks(GTaskList list) {

        List<GTask> tasks = ApplicationCache.INSTANCE.getDatabase().taskDao()
                .loadAllTasksByListId(list.id, accountName);
        rebuildTasksHierarchy(tasks);
        return tasks;
    }

    private void rebuildTasksHierarchy(List<GTask> tasks) {

        Map<String, GTask> map = tasks.stream().collect(toMap(GTask::getGoogleId, identity()));
        Iterator<GTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            GTask task = iterator.next();
            task.isStored = true;
            if (task.getParentId() != null && !task.getParentId().isEmpty()) {
                map.get(task.getParentId()).getChildren().add(task);
                iterator.remove();
            }
        }
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