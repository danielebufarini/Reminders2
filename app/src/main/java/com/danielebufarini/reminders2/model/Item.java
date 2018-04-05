package com.danielebufarini.reminders2.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.database.sqlite.SQLiteDatabase;

import com.danielebufarini.reminders2.util.ApplicationCache;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public abstract class Item implements Serializable {
    private static final long serialVersionUID = 987654322L;

    public static final List<Item> EMPTY_LIST = new ArrayList<Item>(0);
    private static final ApplicationCache CACHE = ApplicationCache.INSTANCE;

    @PrimaryKey
    public long id;
    @ColumnInfo(name = "updated_date")
    public long updated;
    public String title;
    public String googleId;
    @ColumnInfo(name = "account_name")
    public String accountName;
    public boolean isDeleted;
    public boolean isMerged;
    public transient boolean isModified;
    public transient boolean isStored;

    public Item() {

        id = generateId();
    }

    public Item(long id) {

        this.id = id;
    }

    public Item(Item that) {

        this.id = that.id;
        this.updated = that.updated;
        this.title = that.title;
        this.googleId = that.googleId;
        this.accountName = that.accountName;
        this.isDeleted = that.isDeleted;
        this.isMerged = that.isMerged;
        this.isModified = that.isModified;
    }

    public static long generateId() {

        return CACHE.getNextLong();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        return id == item.id;
    }

    @Override
    public int hashCode() {

        return (int) (id ^ (id >>> 32));
    }

    public abstract void insert(SQLiteDatabase db);

    public abstract void delete(SQLiteDatabase db);

    public abstract void merge(SQLiteDatabase db);

    public abstract void insert(Tasks googleService) throws IOException;

    public abstract void delete(Tasks googleService) throws IOException;

    public abstract void merge(Tasks googleService) throws IOException;

    public abstract boolean hasChildren();

    public abstract <T extends Item> List<T> getChildren();

    public abstract <T extends Item> void setChildren(List<T> items);

    public abstract boolean hasReminder();

    public abstract long getReminder();

    public abstract void setReminder(long reminder);

    public abstract long getReminderInterval();

    public abstract void setReminderInterval(long intervall);
}
