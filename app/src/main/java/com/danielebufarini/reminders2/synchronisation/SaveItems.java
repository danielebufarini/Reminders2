
package com.danielebufarini.reminders2.synchronisation;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.model.Item;
import com.danielebufarini.reminders2.synchronisation.commands.Command;
import com.danielebufarini.reminders2.synchronisation.commands.DeleteFromDB;
import com.danielebufarini.reminders2.synchronisation.commands.DeleteFromGoogleTaskAndFromDB;
import com.danielebufarini.reminders2.synchronisation.commands.InsertInDB;
import com.danielebufarini.reminders2.synchronisation.commands.InsertInGoogleTaskAndMergeInDB;
import com.danielebufarini.reminders2.synchronisation.commands.MergeInDB;
import com.danielebufarini.reminders2.synchronisation.commands.MergeInGoogleTask;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.danielebufarini.reminders2.util.GoogleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.rits.cloning.Cloner;

import android.content.Context;
import android.util.Log;

public class SaveItems implements Runnable {

    private static transient final Cloner       CLONER = new Cloner();
    private static transient final ObjectMapper MAPPER = new ObjectMapper();
    private static transient final String       TAG    = SaveItems.class.getSimpleName();

    private final boolean                       isSyncWithGoogleEnabled;
    private final DriveClient                   driveClient;
    private final DriveResourceClient           driveResourceClient;
    private final List<GTaskList>               inMemoryLists;
    private com.google.api.services.tasks.Tasks googleService;

    public SaveItems(Context context, String accountName, List<GTaskList> inMemoryLists, DriveClient driveClient,
            DriveResourceClient driveResourceClient) {

        this.inMemoryLists = inMemoryLists;// CLONER.deepClone(lists);
        this.isSyncWithGoogleEnabled = ApplicationCache.INSTANCE.isSyncWithGTasksEnabled();
        this.driveClient = driveClient;
        this.driveResourceClient = driveResourceClient;
        if (isSyncWithGoogleEnabled) {
            this.googleService = GoogleService.getGoogleTasksService(context, accountName);
        }
    }

    private List<Command> mergeItems(List<? extends Item> items) {

        List<Command> commands = new ArrayList<>(50);

        for (Item item : items) {
            if (item.isDeleted) {
                if (isSyncWithGoogleEnabled) {
                    commands.add(new DeleteFromGoogleTaskAndFromDB(googleService, item));
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
                        commands.add(new InsertInGoogleTaskAndMergeInDB(googleService, item));
                        isAlreadyMerged = true;
                    } else if (item.isModified) {
                        commands.add(new MergeInGoogleTask(googleService, item));
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

        String objectsAsJSon;
        try {
            Container container = new Container();
            objectsAsJSon = MAPPER.writeValueAsString(container);
            createFileInAppFolder(objectsAsJSon);
        } catch (JsonProcessingException e) {
            Log.e(TAG, "Marshalling error parsing Java objects", e);
        }
        List<Command> commands = mergeItems(inMemoryLists);
        for (Command command : commands)
            command.execute();
    }

    private void createFileInAppFolder(String fileContent) {

        if (ApplicationCache.INSTANCE.isGoogleDriveAvailable()) {
            final Task<DriveFolder> rootFolderTask = driveResourceClient.getRootFolder();
            final Task<DriveContents> createContentsTask = driveResourceClient.createContents();
            Tasks.whenAll(rootFolderTask, createContentsTask).continueWithTask(task -> {

                DriveFolder parent = rootFolderTask.getResult();
                DriveContents contents = createContentsTask.getResult();
                OutputStream outputStream = contents.getOutputStream();
                try (Writer writer = new OutputStreamWriter(outputStream)) {
                    writer.write(fileContent);
                }
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("tasks.json")
                        .setMimeType("text/plain").setStarred(true).build();
                return driveResourceClient.createFile(parent, changeSet, contents);
            }).addOnSuccessListener(driveFile -> {

                System.out.println(driveFile.toString());
                // showMessage(getString(R.string.file_created, driveFile.getDriveId().encodeToString()));
                // finish();
            }).addOnFailureListener(e -> {

                Log.e(TAG, "Unable to create file", e);
                // showMessage(getString(R.string.file_create_error));
                // finish();
            });
        }
    }

    public static class Container {

        public List<GTaskList> lists = ApplicationCache.INSTANCE.getLists();
    }
}
