package com.danielebufarini.reminders2.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import com.danielebufarini.reminders2.database.Tables;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "tasks")
public class GTask extends Item implements Comparable<GTask>, Serializable {
    private static final long serialVersionUID = 987654321L;

    public static final String SEPARATOR = "::";
    public static final String NOTE_TAG = " " + SEPARATOR + " note ";
    public static final String DUE_TAG = " " + SEPARATOR + " due on ";
    public static final String REMINDER_TAG = " " + SEPARATOR + " reminder for ";
    public static final String REMINDER_LATITUDE_TAG = " " + SEPARATOR + " latitude ";
    public static final String REMINDER_LONGITUDE_TAG = " " + SEPARATOR + " longitute ";
    public static final String REMINDER_RADIUS_TAG = " " + SEPARATOR + " radius ";
    public static final String REMINDER_LOCATION_TITLE = " " + SEPARATOR + " location title ";
    public static final String INTERVAL_TAG = " " + SEPARATOR + " interval ";
    public static final SimpleDateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
    public static final SimpleDateFormat REMINDER_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

    private static final String TASK_COMPLETED = "completed", NEEDS_ACTION = "needsAction";
    private static final String LOGTAG = GTask.class.getSimpleName();

    public String category;
    public String notes;
    public String locationTitle;
    public long completed;
    public long dueDate;
    public long reminderDate;
    public long reminderInterval;
    public long radius;
    public double latitude;
    public double longitude;
    public int priority;
    public int level;
    @Ignore public GTaskList list;
    public long listId;

    public GTask() {

        super();
    }

    @Ignore
    public GTask(long id) {

        super(id);
    }

    @Ignore
    public GTask(GTask that) {

        super(that);
        this.category = that.category;
        this.notes = that.notes;
        this.completed = that.completed;
        this.dueDate = that.dueDate;
        this.reminderDate = that.reminderDate;
        this.reminderInterval = that.reminderInterval;
        this.priority = that.priority;
        this.level = that.level;
        this.latitude = that.latitude;
        this.longitude = that.longitude;
        this.radius = that.radius;
        this.locationTitle = that.locationTitle;
        this.list = that.list;
    }

    @Override
    public String toString() {

        return "GTask[ id = \"" + id + "\" :: googleId = \"" + googleId + "\""
                + (title != null ? " :: title = \"" + title + "\"" : "") + " ]";
    }

    @Override
    public int compareTo(@NonNull GTask that) {

        return Long.compare(dueDate, that.dueDate);
    }

    private ContentValues getValues() {

        ContentValues values = new ContentValues();
        values.put(Tables.ID, id);
        values.put(Tables.TITLE, title);
        values.put(Tables.DELETED, isDeleted);
        values.put(Tables.GTASK_ID, googleId);
        values.put(Tables.CATEGORY, category);
        values.put(Tables.NOTES, notes);
        values.put(Tables.COMPLETED, completed);
        values.put(Tables.UPDATED, updated);
        values.put(Tables.PRIORITY, priority);
        values.put(Tables.LEVEL, level);
        values.put(Tables.DUE_DATE, dueDate);
        values.put(Tables.REMINDER_DATE, reminderDate);
        values.put(Tables.REMINDER_INTERVAL, reminderInterval);
        values.put(Tables.REMINDER_LOCATION_LAT, latitude);
        values.put(Tables.REMINDER_LOCATION_LNG, longitude);
        values.put(Tables.REMINDER_LOCATION_RADIUS, radius);
        values.put(Tables.REMINDER_LOCATION_TITLE, locationTitle);
        values.put(Tables.MERGED, isMerged);
        values.put(Tables.LIST_ID, list.id);
        values.put(Tables.ACCOUNT_NAME, accountName);
        return values;
    }

    @Override
    public void insert(SQLiteDatabase db) {

        db.insert(Tables.TASK_TABLE, null, getValues());
        Log.d(LOGTAG, "db :: inserted task " + this + " in list " + list);
    }

    @Override
    public void delete(SQLiteDatabase db) {

        db.delete(Tables.TASK_TABLE,
                Tables.ID + "=?", new String[]{Long.toString(id)});
        Log.d(LOGTAG, "db :: deleted task " + this + " in list " + list);
    }

    @Override
    public void merge(SQLiteDatabase db) {

        db.update(Tables.TASK_TABLE, getValues(), Tables.ID + "=?", new String[]{Long.toString(id)});
        Log.d(LOGTAG, "db :: updated task " + this + " in list " + list);
    }

    private Task newTask() {

        Task task = new Task();
        task.setTitle(title);
        task.setNotes(notes);
        task.setDeleted(isDeleted);
        task.setUpdated(new DateTime(updated));
        task.setStatus(completed != 0 ? TASK_COMPLETED : NEEDS_ACTION);
        if (dueDate > 0)
            task.setDue(new DateTime(dueDate));
        return task;
    }

    @Override
    public void insert(Tasks googleService) throws IOException {

        Task task = newTask();
        Task newTask = googleService.tasks().insert(list.googleId, task).execute();
        googleId = newTask.getId();
        Log.d(LOGTAG, "google :: inserted task " + this + " in list " + list);
    }

    @Override
    public void delete(Tasks googleService) throws IOException {

        googleService.tasks().delete(list.googleId, googleId).execute();
        Log.d(LOGTAG, "google :: deleted task " + this + " in list " + list);
    }

    @Override
    public void merge(Tasks googleService) throws IOException {

        Task task = newTask();
        task.setId(googleId);
        googleService.tasks().update(list.googleId, task.getId(), task).execute();
        Log.d(LOGTAG, "google :: updated task " + this + " in list " + list);
    }

    @Override
    public boolean hasChildren() {

        return false;
    }

    @Override
    public List<? extends Item> getChildren() {

        return EMPTY_LIST;
    }

    @Override
    public <T extends Item> void setChildren(List<T> items) {
        // Do nothing
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

    public double getLatitude() {

        return latitude;
    }

    public void setLatitude(double latitude) {

        this.latitude = latitude;
    }

    public double getLongitude() {

        return longitude;
    }

    public void setLongitude(double longitude) {

        this.longitude = longitude;
    }

    public long getRadius() {

        return radius;
    }

    public void setRadius(long radius) {

        this.radius = radius;
    }

    public String getLocationTitle() {

        return locationTitle;
    }

    public void setLocationTitle(String locationTitle) {

        this.locationTitle = locationTitle;
    }

    private String extractTagValue(final String source, final int index, final String tag) {

        String result = null;
        if (index > 0) {
            final int idx = source.indexOf(SEPARATOR, index + SEPARATOR.length());
            if (idx > 0)
                result = source.substring(index + tag.length(), idx);
            else
                result = source.substring(index + tag.length());
        }
        if (result != null)
            result = result.trim();
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
            if (tmp != null && !tmp.isEmpty())
                notes = tmp;
            try {
                tmp = extractTagValue(str, i, DUE_TAG);
                if (tmp != null && !tmp.isEmpty())
                    dueDate = DUE_DATE_FORMAT.parse(tmp).getTime();
            } catch (ParseException e) {
                Log.d(LOGTAG, "error parsing due date", e);
            }
            try {
                tmp = extractTagValue(str, j, REMINDER_TAG);
                if (tmp != null && !tmp.isEmpty())
                    reminderDate = REMINDER_DATE_FORMAT.parse(tmp).getTime();
            } catch (ParseException e) {
                Log.d(LOGTAG, "error parsing reminder date", e);
            }
            try {
                tmp = extractTagValue(str, l, INTERVAL_TAG);
                if (tmp != null && !tmp.isEmpty())
                    reminderInterval = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_LATITUDE_TAG);
                if (tmp != null && !tmp.isEmpty())
                    latitude = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_LONGITUDE_TAG);
                if (tmp != null && !tmp.isEmpty())
                    longitude = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_RADIUS_TAG);
                if (tmp != null && !tmp.isEmpty())
                    radius = Long.parseLong(tmp);
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
            try {
                tmp = extractTagValue(str, l, REMINDER_LOCATION_TITLE);
                if (tmp != null && !tmp.isEmpty())
                    locationTitle = tmp;
            } catch (NumberFormatException e) {
                Log.d(LOGTAG, "error parsing reminder interval", e);
            }
        }
    }
}
