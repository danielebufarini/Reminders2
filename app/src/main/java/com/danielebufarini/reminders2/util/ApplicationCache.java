
package com.danielebufarini.reminders2.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.danielebufarini.reminders2.database.RemindersDatabase;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
import com.google.android.gms.drive.DriveResourceClient;

public enum ApplicationCache {
    INSTANCE;

    private final static Object listsLock = new Object();

    private List<GTaskList>     lists     = new ArrayList<>();
    private AtomicInteger       value;
    private int                 activeList;
    private String              accountName;
    private Boolean             isSyncWithGTasksEnabled;
    private GTask               task;
    private RemindersDatabase   database;
    private volatile boolean    isDriveAvailable;

    public DriveResourceClient getDriveResourceClient() {

        return driveResourceClient;
    }

    public void setDriveResourceClient(DriveResourceClient driveResourceClient) {

        this.driveResourceClient = driveResourceClient;
    }

    private DriveResourceClient driveResourceClient;

    public List<GTaskList> getLists() {

        synchronized (listsLock) {
            return Collections.synchronizedList(lists);
        }
    }

    public void setLists(List<GTaskList> lists) {

        synchronized (listsLock) {
            this.lists = lists;
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

    public int getActiveListPosition() {

        return activeList;
    }

    public GTaskList getActiveList() {

        return lists.get(activeList);
    }

    public void setActiveListPosition(int activeList) {

        this.activeList = activeList;
    }

    public String accountName() {

        return accountName;
    }

    public void setAccountName(String accountName) {

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
