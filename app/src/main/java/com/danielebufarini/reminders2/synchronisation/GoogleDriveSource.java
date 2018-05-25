
package com.danielebufarini.reminders2.synchronisation;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;
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

import android.util.Log;

public class GoogleDriveSource {

    private static final String           TAG          = GoogleDriveSource.class.getSimpleName();
    private static final ObjectMapper     MAPPER       = new ObjectMapper();
    private static final ApplicationCache CACHE        = ApplicationCache.INSTANCE;
    private static final Source           EMPTY_SOURCE = new Source() {
        @Override
        public List<GTask> getTasks(GTaskList list) {

            return Collections.emptyList();
        }

        @Override
        public List<GTaskList> getLists() {
            return Collections.emptyList();
        }
    };

    public static void query(GoogleDriveCallback callback) {

        DriveResourceClient driveResourceClient = CACHE.getDriveResourceClient();
        Query query = new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE, "tasks.json")).build();
        Task<MetadataBuffer> queryTask = driveResourceClient.query(query);
        queryTask.addOnSuccessListener(metadataBuffer -> {
            Iterator<Metadata> iterator = metadataBuffer.iterator();
            if (iterator.hasNext()) {
                Metadata metadata = iterator.next();
                Task<DriveContents> openFileTask = driveResourceClient.openFile(metadata.getDriveId().asDriveFile(),
                        DriveFile.MODE_READ_ONLY);
                openFileTask.continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(contents.getInputStream()))) {
                        String serialisedLists = reader.lines().collect(joining("\n"));
                        SaveItems.Container container = MAPPER.readValue(serialisedLists, SaveItems.Container.class);
                        callback.googleDriveSourceReady(new Source() {

                            @Override
                            public List<GTask> getTasks(GTaskList list) {

                                List<GTaskList> lists = container.lists;
                                return lists.get(lists.indexOf(list)).tasks;
                            }

                            @Override
                            public List<GTaskList> getLists() {

                                return container.lists;
                            }
                        });
                    }
                    return driveResourceClient.discardContents(contents);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Cannot read serialised tasks file", e);
                    callback.googleDriveSourceReady(EMPTY_SOURCE);
                });
                metadataBuffer.release();
            } else {
                callback.googleDriveSourceReady(EMPTY_SOURCE);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Cannot get Google Drive metadatabuffer", e);
            callback.googleDriveSourceReady(EMPTY_SOURCE);
        });
    }

    public interface GoogleDriveCallback {

        void googleDriveSourceReady(Source source);
    }

    public static void save() {

        try {
            String objectsAsJSon = MAPPER.writeValueAsString(CACHE.getLists());
            GoogleDriveSource.writeFile(driveContents -> {
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
    
    private static void writeFile(Consumer<DriveContents> consumer) {

        DriveResourceClient driveResourceClient = CACHE.getDriveResourceClient();
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

    private static void createNewFile(Consumer<DriveContents> consumer) {

        if (CACHE.isGoogleDriveAvailable()) {
            DriveResourceClient driveResourceClient = CACHE.getDriveResourceClient();
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
}
