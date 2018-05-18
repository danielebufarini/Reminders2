package com.danielebufarini.reminders2.synchronisation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.ui.Reminders;
import com.danielebufarini.reminders2.util.GoogleService;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;

import android.content.Context;
import android.util.Log;

public class GoogleTasksRemoteServer implements Source {
    public static final String LOGTAG = GoogleTasksRemoteServer.class.getSimpleName();
    private final String accountName;
    private final Tasks googleService;

    public GoogleTasksRemoteServer(final Context context, String accountName) {

        this.accountName = accountName;
        googleService = GoogleService.getGoogleTasksService(context, accountName);
    }

    @Override
    public List<GTask> getTasks(GTaskList list) throws IOException {

        List<GTask> subtasks = new ArrayList<>(20);
        Map<String, GTask> gtasks = new HashMap<>(20);
        com.google.api.services.tasks.model.Tasks tasks = googleService.tasks().list(list.getGoogleId()).execute();
        if (tasks.getItems() != null) {
            for (com.google.api.services.tasks.model.Task task : tasks.getItems()) {
                GTask gtask = new GTask();
                gtask.setGoogleId(task.getId());
                gtask.title = task.getTitle();
                gtask.updated = task.getUpdated().getValue();
                gtask.notes = task.getNotes();
                gtask.completed = task.getCompleted() != null ? task.getCompleted().getValue() : 0;
                gtask.dueDate = task.getDue() != null ? task.getDue().getValue() : 0;
                gtask.isDeleted = task.getDeleted() != null ? task.getDeleted() : false;
                gtask.setParentId(task.getParent());
                gtask.setListId(list.id);
                gtask.setListGoogleId(list.getGoogleId());
                gtask.accountName = accountName;
                if (gtask.getParentId() == null || gtask.getParentId().isEmpty()) {
                    gtasks.put(gtask.getGoogleId(), gtask);
                } else {
                    subtasks.add(gtask);
                }
                if (Reminders.LOGV)
                    Log.d(LOGTAG, "google :: downloaded task " + gtask + " for list " + list);
            }
        }
        insertSubtasksInList(gtasks, subtasks);
        return new ArrayList<>(gtasks.values());
    }

    private void insertSubtasksInList(Map<String, GTask> tasks, List<GTask> subtasks) {

        subtasks.forEach(subtask -> {
            GTask task = tasks.get(subtask.getParentId());
            if (task != null) {
                task.getChildren().add(subtask);
            }
        });
    }

    @Override
    public List<GTaskList> getLists() {

        List<GTaskList> googleItems = new ArrayList<>(30);
        try {
            TaskLists taskLists = googleService.tasklists().list().execute();
            for (TaskList taskList : taskLists.getItems())
                try {
                    GTaskList list = new GTaskList();
                    list.setGoogleId(taskList.getId());
                    list.title = taskList.getTitle();
                    list.updated = taskList.getUpdated().getValue();
                    list.accountName = accountName;
                    list.tasks = getTasks(list);
                    googleItems.add(list);
                    if (Reminders.LOGV) Log.d(LOGTAG, "google :: downloaded list " + list);
                } catch (Exception e) {
                    Log.e(LOGTAG, "google :: cannot retrive task from google servers for account +\""
                            + accountName + "\"", e);
                }
        } catch (Exception e) {
            //TODO java.net.SocketTimeoutException: SSL handshake timed out
            //TODO java.net.ConnectException
            Log.e(LOGTAG, "google :: cannot retrive lists from google servers for account \""
                    + accountName + "\"", e);
        }
        if (Reminders.LOGV) Log.d(LOGTAG, "google :: loaded '" + googleItems.size() + "' items.");
        return googleItems;
    }
}
