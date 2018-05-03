package com.danielebufarini.reminders2.synchronisation.commands;

import android.util.Log;

import com.danielebufarini.reminders2.model.Item;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;

public class InsertInGoogleTaskAndMergeInDB implements Command {
    private static final String TAG = InsertInGoogleTaskAndMergeInDB.class.getSimpleName();
    private Tasks googleService;
    private Item item;

    public InsertInGoogleTaskAndMergeInDB(Tasks googleService, Item item) {

        this.googleService = googleService;
        this.item = item;
    }

    @Override
    public void execute() {

        try {
            item.isMerged = true;
            item.insert(googleService);
        } catch (IOException e) {
            item.isMerged = false;
            Log.w(TAG, "", e);
        }
        if (item.isStored && (item.isMerged || item.isModified)) {
            item.merge();
        }
    }
}
