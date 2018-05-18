
package com.danielebufarini.reminders2.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.danielebufarini.reminders2.database.RemindersDatabase;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;

public enum ApplicationCache {
    INSTANCE;

    private final static Object listsLock = new Object();

    private List<GTaskList>     folders   = new ArrayList<>();
    private AtomicInteger       value;
    private int                 activeFolder;
    private String              accountName;
    private Boolean             isSyncWithGTasksEnabled;
    private GTask               task;
    private RemindersDatabase   database;
    private volatile boolean    isDriveAvailable;

    public List<GTaskList> getLists() {

        synchronized (listsLock) {
            return Collections.synchronizedList(folders);
        }
    }

    public void setFolders(List<GTaskList> folders) {

        synchronized (listsLock) {
            this.folders = folders;
        }
    }

    public Boolean isSyncWithGTasksEnabled() {

        return isSyncWithGTasksEnabled;
    }

    public void isSyncWithGTasksEnabled(Boolean isSyncWithGTasksEnabled) {

        this.isSyncWithGTasksEnabled = isSyncWithGTasksEnabled;
    }

    public void setAtomicIntValue(int value) {

        this.value = new AtomicInteger(value);
    }

    public int getNextInt() {

        return value.getAndIncrement();
    }

    public int getActiveList() {

        return activeFolder;
    }

    public void setActiveFolder(int activeFolder) {

        this.activeFolder = activeFolder;
    }

    public String accountName() {

        return accountName;
    }

    public void accountName(String accountName) {

        this.accountName = accountName;
    }

    public GTask getTask() {

        return task;
    }

    public void setTask(GTask task) {

        this.task = task;
    }

    public RemindersDatabase getDatabase() {

        return database;
    }

    public void setDatabase(RemindersDatabase database) {

        this.database = database;
    }

    public boolean isGoogleDriveAvailable() {

        return isDriveAvailable;
    }

    public void setGoogleDriveAvailable(boolean driveAvailable) {

        isDriveAvailable = driveAvailable;
    }
}
