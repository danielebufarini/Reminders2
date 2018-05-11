
package com.danielebufarini.reminders2.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.danielebufarini.reminders2.util.ApplicationCache;
import com.google.api.services.tasks.Tasks;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity
public abstract class Item implements Serializable {

    private static transient final ApplicationCache CACHE            = ApplicationCache.INSTANCE;
    private static final long                       serialVersionUID = 987654322L;

    @PrimaryKey
    public long                                     id;
    public long                                     updated;
    public String                                   title;
    public String                                   googleId;
    public String                                   accountName;
    public boolean                                  isDeleted;
    private boolean                                 isMerged;
    @Ignore
    public boolean                                  isModified;
    @Ignore
    public boolean                                  isStored;
    @Ignore
    private String                                  parentId;

    public Item() {

        id = generateId();
    }

    public Item(long id) {

        this.id = id;
    }

    private static long generateId() {

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

    public abstract long getListId();

    public abstract void setListId(long listId);

    public abstract void insert();

    public abstract void delete();

    public abstract void merge();

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

    public abstract String getParentId();

    public abstract void setParentId(String parentId);

    public boolean isMerged() {

        return isMerged;
    }

    public void setMerged(boolean merged) {

        isMerged = merged;
    }
}
