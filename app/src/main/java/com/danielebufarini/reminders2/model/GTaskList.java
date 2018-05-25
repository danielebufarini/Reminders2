
package com.danielebufarini.reminders2.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.danielebufarini.reminders2.services.AsyncHandler;
import com.danielebufarini.reminders2.synchronisation.GoogleDriveSource;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.TaskList;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.util.Log;

@Entity(tableName = "list")
@JsonIgnoreProperties(value = { "children" })
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

    @Ignore
    public GTaskList(String id) {

        super(id);
    }

    @Override
    public String getListId() {

        return id;
    }

    @Override
    public void setListId(String listId) {

    }

    @Override
    public String toString() {

        return "GTaskList[ id = \"" + id + "\" :: googleId = \"" + getGoogleId() + "\""
                + (" :: title = \"" + title + "\"") + " ]";
    }

    @Override
    public void insert() {

        AsyncHandler.post(() -> ApplicationCache.INSTANCE.getDatabase().listDao().insert(this));
        isStored = true;
        GoogleDriveSource.save();
        Log.d(LOGTAG, "db :: inserted list " + this);
    }

    @Override
    public void delete() {

        AsyncHandler.post(() -> ApplicationCache.INSTANCE.getDatabase().listDao().delete(this));
        GoogleDriveSource.save();
        Log.d(LOGTAG, "db :: deleted list " + this);
    }

    @Override
    public void merge() {

        AsyncHandler.post(() -> ApplicationCache.INSTANCE.getDatabase().listDao().update(this));
        GoogleDriveSource.save();
        Log.d(LOGTAG, "db :: updated list " + this);
    }

    private TaskList newTaskList() {

        TaskList taskList = new TaskList();
        taskList.setTitle(title);
        taskList.setUpdated(new DateTime(updated));
        return taskList;
    }

    @Override
    public boolean hasChildren() {

        return tasks != null && !tasks.isEmpty();
    }

    @Override
    public <T extends Item> List<T> getChildren() {

        if (tasks == null) tasks = new ArrayList<>(20);
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
