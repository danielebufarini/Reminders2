
package com.danielebufarini.reminders2.synchronisation;

import java.util.List;

import com.danielebufarini.reminders2.model.GTaskList;
import com.google.android.gms.drive.DriveResourceClient;

import android.content.Context;

public class SaveItemsBuilder {

    private Context             context;
    private String              accountName;
    private List<GTaskList>     inMemoryLists;
    private DriveResourceClient driveResourceClient;

    public SaveItemsBuilder setContext(Context context) {

        this.context = context;
        return this;
    }

    public SaveItemsBuilder setAccountName(String accountName) {

        this.accountName = accountName;
        return this;
    }

    public SaveItemsBuilder setInMemoryLists(List<GTaskList> inMemoryLists) {

        this.inMemoryLists = inMemoryLists;
        return this;
    }

    public SaveItemsBuilder setDriveResourceClient(DriveResourceClient driveResourceClient) {

        this.driveResourceClient = driveResourceClient;
        return this;
    }

    public SaveItems build() {

        return new SaveItems(context, accountName, inMemoryLists, driveResourceClient);
    }
}