package com.danielebufarini.reminders2.util;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public enum ApplicationCache {
    INSTANCE;

    private List<GTaskList> folders = new ArrayList<GTaskList>();
    private AtomicInteger value;
    private int activeFolder;
    private String accountName;
    private AtomicLong atomicLong;
    private Boolean isSyncWithGTasksEnabled;
    private GTask task;

    public static ApplicationCache getInstance() {
        return INSTANCE;
    }

    private final static Object listsLock = new Object();

    public List<GTaskList> getFolders() {
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

    public int getActiveFolder() {
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

    public void setAtomicLongValue(long value) {
        this.atomicLong = new AtomicLong(value);
    }

    public boolean isAtomicLongNull() {
        return atomicLong == null;
    }

    public long getNextLong() {
        return atomicLong.getAndIncrement();
    }

    public GTask task() {
        return task;
    }

    public void task(GTask task) {
        this.task = task;
    }
}
