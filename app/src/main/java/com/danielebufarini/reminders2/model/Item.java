
package com.danielebufarini.reminders2.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.api.services.tasks.Tasks;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GTaskList.class, name = "list"),
        @JsonSubTypes.Type(value = GTask.class, name = "task")
})
public abstract class Item implements Serializable {

    private static final long serialVersionUID = 987654322L;

    @PrimaryKey
    @NonNull
    public String             id;
    public long               updated;
    public String             title;
    private String            googleId;
    public String             accountName;
    public boolean            isDeleted;
    private boolean           isMerged;
    @Ignore
    public boolean            isModified;
    @Ignore
    public boolean            isStored;

    public Item() {

        id = UUID.randomUUID().toString();
    }

    public Item(@NonNull String id) {

        this.id = id;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        return id != null ? id.equals(item.id) : item.id == null;
    }

    @Override
    public int hashCode() {

        return id != null ? id.hashCode() : 0;
    }

    public abstract String getListId();

    public abstract void setListId(String listId);

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

    public String getGoogleId() {

        return googleId;
    }

    public void setGoogleId(String googleId) {

        this.googleId = googleId;
    }
}
