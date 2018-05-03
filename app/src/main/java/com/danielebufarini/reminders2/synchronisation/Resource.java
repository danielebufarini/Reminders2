package com.danielebufarini.reminders2.synchronisation;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;

import java.io.IOException;
import java.util.List;

public interface Resource {
    List<GTask> loadTasks(GTaskList list) throws IOException;
    List<GTaskList> loadLists();
}
