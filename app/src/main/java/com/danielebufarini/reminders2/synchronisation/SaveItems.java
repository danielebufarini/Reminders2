
package com.danielebufarini.reminders2.synchronisation;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.model.Item;
import com.danielebufarini.reminders2.synchronisation.commands.Command;
import com.danielebufarini.reminders2.synchronisation.commands.DeleteFromDB;
import com.danielebufarini.reminders2.synchronisation.commands.InsertInDB;
import com.danielebufarini.reminders2.synchronisation.commands.MergeInDB;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.rits.cloning.Cloner;

import android.util.Log;

public class SaveItems implements Runnable {

    private static transient final Cloner       CLONER = new Cloner();
    private static transient final ObjectMapper MAPPER = new ObjectMapper();
    private static transient final String       TAG    = SaveItems.class.getSimpleName();

    private final boolean                       isSyncWithGoogleEnabled;
    private final DriveResourceClient           driveResourceClient;
    private final List<GTaskList>               inMemoryLists;

    public SaveItems(List<GTaskList> inMemoryLists, DriveResourceClient driveResourceClient) {

        this.inMemoryLists = inMemoryLists;// CLONER.deepClone(lists);
        this.isSyncWithGoogleEnabled = ApplicationCache.INSTANCE.isSyncWithGTasksEnabled();
        this.driveResourceClient = driveResourceClient;
    }

    private List<Command> mergeItems(List<? extends Item> items) {

        List<Command> commands = new ArrayList<>(50);

        for (Item item : items) {
            if (item.isDeleted) {
                if (isSyncWithGoogleEnabled) {
                    commands.add(new DeleteFromDB(item));
                } else if (item.isStored) {
                    if ("".equals(item.getGoogleId())) {
                        commands.add(new DeleteFromDB(item));
                    } else {
                        commands.add(new MergeInDB(item));
                    }
                }
            } else {
                boolean isAlreadyMerged = false;
                if (isSyncWithGoogleEnabled) {
                    if (item.getGoogleId().isEmpty()) {
                        commands.add(new MergeInDB(item));
                        isAlreadyMerged = true;
                    }
                }
                if (!item.isStored) {
                    commands.add(new InsertInDB(item));
                } else if (!isAlreadyMerged && item.isModified) {
                    commands.add(new MergeInDB(item));
                }
            }
            if (item.hasChildren()) {
                commands.addAll(mergeItems(item.getChildren()));
            }
        }

        return commands;
    }

    @Override
    public void run() {

        List<Command> commands = mergeItems(inMemoryLists);
        for (Command command : commands) {
            command.execute();
        }
        if (!commands.isEmpty()) {
            try {
                LocalDatabase database = new LocalDatabase(ApplicationCache.INSTANCE.accountName());
                String objectsAsJSon = MAPPER.writeValueAsString(new Container(database.getLists()));
                writeFile(driveContents -> {
                    OutputStream outputStream = driveContents.getOutputStream();
                    try (Writer writer = new OutputStreamWriter(outputStream)) {
                        writer.write(objectsAsJSon);
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing serialised data into stream");
                    }
                });
            } catch (JsonProcessingException e) {
                Log.e(TAG, "Marshalling error parsing Java objects", e);
            }
        }
    }

    private void writeFile(Consumer<DriveContents> consumer) {

        Query query = new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE, "tasks.json")).build();
        Task<MetadataBuffer> queryTask = driveResourceClient.query(query);
        queryTask.addOnSuccessListener(metadataBuffer -> {
            Iterator<Metadata> iterator = metadataBuffer.iterator();
            if (iterator.hasNext()) {
                Metadata metadata = iterator.next();
                Task<DriveContents> openFileTask = driveResourceClient.openFile(metadata.getDriveId().asDriveFile(),
                        DriveFile.MODE_WRITE_ONLY);
                openFileTask.continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    consumer.accept(contents);
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("tasks.json")
                            .setMimeType("text/plain").build();
                    return driveResourceClient.commitContents(contents, changeSet);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Cannot read serialised tasks file", e);
                });
                metadataBuffer.release();
            } else {
                createNewFile(consumer);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Cannot get Google Drive metadatabuffer", e);
        });
    }

    private void createNewFile(Consumer<DriveContents> consumer) {

        if (ApplicationCache.INSTANCE.isGoogleDriveAvailable()) {
            final Task<DriveFolder> rootFolderTask = driveResourceClient.getRootFolder();
            final Task<DriveContents> createContentsTask = driveResourceClient.createContents();
            Tasks.whenAll(rootFolderTask, createContentsTask).continueWithTask(task -> {

                DriveFolder parent = rootFolderTask.getResult();
                DriveContents contents = createContentsTask.getResult();
                consumer.accept(contents);
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("tasks.json")
                        .setMimeType("text/plain").build();
                return driveResourceClient.createFile(parent, changeSet, contents);
            }).addOnSuccessListener(driveFile -> {
                System.out.println(driveFile.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Unable to create file", e);
            });
        }
    }

    public static class Container {

        public List<GTaskList> lists;

        public Container() {

        }

        public Container(List<GTaskList> lists) {

            this.lists = lists;
        }
    }
}
