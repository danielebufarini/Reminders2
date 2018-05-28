
package com.danielebufarini.reminders2.model;

import static android.arch.persistence.room.ForeignKey.CASCADE;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.danielebufarini.reminders2.database.TaskDao;
import com.danielebufarini.reminders2.services.AsyncHandler;
import com.danielebufarini.reminders2.synchronisation.GoogleDriveSource;
import com.danielebufarini.reminders2.ui.Reminders;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.support.annotation.NonNull;
import android.util.Log;

@Entity(tableName = "task", foreignKeys = {
        @ForeignKey(entity = GTaskList.class, parentColumns = "id", childColumns = "listId", onDelete = CASCADE) }, indices = @Index(value = "listId"))
@JsonIgnoreProperties(value = { "children" })
public class GTask extends Item implements Comparable<GTask>, Serializable {

    private static final long            serialVersionUID        = 987654321L;

    public static final String           SEPARATOR               = "::";
    public static final String           NOTE_TAG                = " " + SEPARATOR + " note ";
    public static final String           DUE_TAG                 = " " + SEPARATOR + " due on ";
    public static final String           REMINDER_TAG            = " " + SEPARATOR + " reminder for ";
    public static final String           REMINDER_LATITUDE_TAG   = " " + SEPARATOR + " latitude ";
    public static final String           REMINDER_LONGITUDE_TAG  = " " + SEPARATOR + " longitute ";
    public static final String           REMINDER_RADIUS_TAG     = " " + SEPARATOR + " radius ";
    public static final String           REMINDER_LOCATION_TITLE = " " + SEPARATOR + " location title ";
    public static final String           INTERVAL_TAG            = " " + SEPARATOR + " interval ";
    public static final SimpleDateFormat DUE_DATE_FORMAT         = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
    public static final SimpleDateFormat REMINDER_DATE_FORMAT    = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
    private static final String          LOGTAG                  = GTask.class.getSimpleName();

    @Ignore
    public long                          completed;
    public String                        category;
    public String                        notes;
    public String                        locationTitle;
    public long                          dueDate;
    public long                          reminderDate;
    public long                          reminderInterval;
    public long                          radius;
    public double                        latitude;
    public double                        longitude;
    public int                           priority;
    private String                       parentId;
    private String                       listId;
    private String                       listGoogleId;

    public GTask() {

        super();
    }

    @Ignore
    public GTask(String id) {

        super(id);
    }

    @Override
    public String toString() {

        return "GTask[ id = \"" + id + "\" :: googleId = \"" + getGoogleId() + "\""
                + (title != null ? " :: title = \"" + title + "\"" : "") + " ]";
    }

    @Override
    public int compareTo(@NonNull GTask that) {

        return Long.compare(dueDate, that.dueDate);
    }

    @Override
    public void insert() {

        isStored = true;
        AsyncHandler.post(() -> {
            TaskDao dao = ApplicationCache.INSTANCE.getDatabase().taskDao();
            try {
                dao.insert(this);
            } catch (Exception e) {
                isStored = false;
                Log.e(LOGTAG, e.getMessage());
            }
            if (Reminders.LOGV) {
                Log.d(LOGTAG, "db :: inserted task " + this + " in list id " + listId);
            }
        });
        GoogleDriveSource.save();
    }

    @Override
    public void delete() {

        AsyncHandler.post(() -> {
            TaskDao dao = ApplicationCache.INSTANCE.getDatabase().taskDao();
            dao.delete(this);
            if (Reminders.LOGV) {
                Log.d(LOGTAG, "db :: deleted task " + this + " in list id " + listId);
            }
        });
        GoogleDriveSource.save();
    }

    @Override
    public void merge() {

        AsyncHandler.post(() -> {
            ApplicationCache.INSTANCE.getDatabase().taskDao().update(this);
            if (Reminders.LOGV) {
                Log.d(LOGTAG, "db :: updated task " + this + " in list id " + listId);
            }
        });
        GoogleDriveSource.save();
    }

    @Override
    public boolean hasChildren() {

        return false;
    }

    @Override
    public <T extends Item> List<T> getChildren() {

        return Collections.emptyList();
    }

    @Override
    public <T extends Item> void setChildren(List<T> items) {

    }

    @Override
    public boolean hasReminder() {

        return true;
    }

    @Override
    public long getReminder() {

        return reminderDate;
    }

    @Override
    public void setReminder(long reminder) {

        this.reminderDate = reminder;
    }

    @Override
    public long getReminderInterval() {

        return reminderInterval;
    }

    @Override
    public void setReminderInterval(long interval) {

        this.reminderInterval = interval;
    }

    public void setList(GTaskList list) {

        listId = list.id;
    }

    @Override
    public String getListId() {

        return listId;
    }

    @Override
    public void setListId(String listId) {

        this.listId = listId;
    }

    public String getListGoogleId() {

        return listGoogleId;
    }

    public void setListGoogleId(String listGoogleId) {

        this.listGoogleId = listGoogleId;
    }

    private String extractTagValue(final String source, final int index, final String tag) {

        String result = null;
        if (index > 0) {
            final int idx = source.indexOf(SEPARATOR, index + SEPARATOR.length());
            if (idx > 0) result = source.substring(index + tag.length(), idx);
            else
                result = source.substring(index + tag.length());
        }
        if (result != null) result = result.trim();
        return result;
    }

    public void parse(final String str) {

        final int i = str.indexOf(DUE_TAG);
        final int j = str.indexOf(REMINDER_TAG);
        final int k = str.indexOf(NOTE_TAG);
        final int l = str.indexOf(INTERVAL_TAG);
        final int m = str.indexOf(REMINDER_LATITUDE_TAG);
        final int n = str.indexOf(REMINDER_LONGITUDE_TAG);
        final int o = str.indexOf(REMINDER_RADIUS_TAG);
        final int p = str.indexOf(REMINDER_LOCATION_TITLE);
        if (i == -1 && j == -1 && k == -1 && l == -1 && m == -1 && n == -1 && o == -1 && p == -1) {
            title = str;
        } else {
            title = str.substring(0, str.indexOf(SEPARATOR));
            String tmp = extractTagValue(str, k, NOTE_TAG);
            if (tmp != null && !tmp.isEmpty()) notes = tmp;
            try {
                tmp = extractTagValue(str, i, DUE_TAG);
                if (tmp != null && !tmp.isEmpty()) dueDate = DUE_DATE_FORMAT.parse(tmp).getTime();
            } catch (ParseException e) {
                Log.d(LOGTAG, "error parsing due date", e);
            }
            try {
                tmp = extractTagValue(str, j, REMINDER_TAG);
                if (tmp != null && !tmp.isEmpty()) reminderDate = REMINDER_DATE_FORMAT.parse(tmp).getTime();
            } catch (ParseException e) {
                Log.d(LOGTAG, "error parsing reminder date", e);
            }
            try {
                tmp = extractTagValue(str, l, INTERVAL_TAG);
                if (tmp != null && !tmp.isEmpty()) reminderInterval = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_LATITUDE_TAG);
                if (tmp != null && !tmp.isEmpty()) latitude = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_LONGITUDE_TAG);
                if (tmp != null && !tmp.isEmpty()) longitude = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_RADIUS_TAG);
                if (tmp != null && !tmp.isEmpty()) radius = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_LOCATION_TITLE);
                if (tmp != null && !tmp.isEmpty()) locationTitle = tmp;
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
        }
    }

    @Override
    public String getParentId() {

        return parentId;
    }

    @Override
    public void setParentId(String parentId) {

        this.parentId = parentId;
    }
}
