package com.danielebufarini.reminders2.synchronisation;

import android.content.Context;
import android.util.Log;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.util.GoogleService;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadItemsFromGoogle implements LoadItemsFromResource {
	public static final String LOGTAG = LoadItemsFromGoogle.class.getSimpleName();
    private final Context context;
    private final String accountName;
    private final Tasks googleService;

    public LoadItemsFromGoogle(final Context context, String accountName) {
        this.accountName = accountName;
        this.context = context;
        googleService = GoogleService.getGoogleTasksService(context, accountName);
    }
    
    @Override
    public List<GTask> loadTasks(GTaskList list) throws IOException {
    	List<GTask> gtasks = new ArrayList<>(20);
    	com.google.api.services.tasks.model.Tasks tasks = googleService.tasks().list(list.googleId).execute();
    	if (tasks.getItems() != null)
    		for (com.google.api.services.tasks.model.Task task : tasks.getItems()) {
    			GTask gtask = new GTask();
    			gtask.googleId = task.getId();
    			gtask.title = task.getTitle();
    			gtask.updated = task.getUpdated().getValue();
    			gtask.notes = task.getNotes();
    			gtask.completed = task.getCompleted() != null ? task.getCompleted().getValue() : 0;
    			gtask.dueDate = task.getDue() != null ? task.getDue().getValue() : 0;
    			gtask.isDeleted = task.getDeleted() != null ? task.getDeleted() : false;
    			gtask.setList(list);
    			gtask.accountName = accountName;
    			gtasks.add(gtask);
    			Log.d(LOGTAG, "google :: downloaded task " + gtask + " for list " + list);
    		}
		return gtasks;
    }

    @Override
    public List<GTaskList> loadLists() {
		List<GTaskList> googleItems = new ArrayList<>(30);
		try {
			TaskLists taskLists = googleService.tasklists().list().execute();
			for (TaskList taskList : taskLists.getItems())
				try {
					GTaskList list = new GTaskList();
					list.googleId = taskList.getId();
					list.title = taskList.getTitle();
					list.updated = taskList.getUpdated().getValue();
					list.accountName = accountName;
                    list.tasks = loadTasks(list);
					googleItems.add(list);
					Log.d(LOGTAG, "google :: downloaded list " + list);
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
		Log.d(LOGTAG, "google :: loaded '" + googleItems.size() + "' items.");
		return googleItems;
    }
}
