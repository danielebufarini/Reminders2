
package com.danielebufarini.reminders2.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.danielebufarini.reminders2.services.AsyncHandler;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.util.Log;

@Entity(tableName = "list")
public class GTaskList extends Item implements Serializable {

    private static transient final String LOGTAG            = GTaskList.class.getSimpleName();
    private static final long             serialVersionUID  = 1234567890L;

    @Ignore
    public List<GTask>                    tasks;
    public boolean                        isHideCompleted   = false;
    public boolean                        isSortedByDueDate = true;

    public GTaskList() {

        super();
    }

    @Override
    public long getListId() {

        return id;
    }

    @Override
    public void setListId(long listId) {

    }

    @Ignore
    public GTaskList(Long id) {

        super(id);
    }

    @Override
    public String toString() {

        return "GTaskList[ id = \"" + id + "\" :: googleId = \"" + googleId + "\"" + (" :: title = \"" + title + "\"")
                + " ]";
    }

    @Override
    public void insert() {

        AsyncHandler.post(() -> ApplicationCache.INSTANCE.getDatabase().listDao().insert(this));
        isStored = true;
        Log.d(LOGTAG, "db :: inserted list " + this);
    }

    @Override
    public void delete() {

        AsyncHandler.post(() -> ApplicationCache.INSTANCE.getDatabase().listDao().delete(this));
        Log.d(LOGTAG, "db :: deleted list " + this);
    }

    @Override
    public void merge() {

        AsyncHandler.post(() -> ApplicationCache.INSTANCE.getDatabase().listDao().update(this));
        Log.d(LOGTAG, "db :: updated list " + this);
    }

    private TaskList newTaskList() {

        TaskList taskList = new TaskList();
        taskList.setTitle(title);
        taskList.setUpdated(new DateTime(updated));
        return taskList;
    }

    @Override
    public void insert(Tasks googleService) throws IOException {

        TaskList taskList = googleService.tasklists().insert(newTaskList()).execute();
        googleId = taskList.getId();
        Log.d(LOGTAG, "google :: inserted list id " + id);
    }

    @Override
    public void delete(Tasks googleService) throws IOException {

        googleService.tasklists().delete(googleId).execute();
        Log.d(LOGTAG, "google :: deleted list id " + id);
    }

    @Override
    public void merge(Tasks googleService) throws IOException {

        TaskList taskList = newTaskList();
        taskList.setId(googleId);
        googleService.tasklists().update(googleId, taskList).execute();
        Log.d(LOGTAG, "google :: updated list id " + id);
    }

    @Override
    public boolean hasChildren() {

        return true;
    }

    @Override
    public <T extends Item> List<T> getChildren() {

        return (List<T>) tasks;
    }

    @Override
    public <T extends Item> void setChildren(List<T> items) {

        tasks = (List<GTask>) items;
    }

    public boolean hasReminder() {

        return false;
    }

    public long getReminder() {

        return 0L;
    }

    public void setReminder(long reminder) {

    }

    public long getReminderInterval() {

        return 0L;
    }

    public void setReminderInterval(long interval) {

    }

    @Override
    public String getParentId() {

        return null;
    }

    @Override
    public void setParentId(String parentId) {

    }
}
