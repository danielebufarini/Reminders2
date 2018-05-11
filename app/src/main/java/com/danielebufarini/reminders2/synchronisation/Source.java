
package com.danielebufarini.reminders2.synchronisation;

import java.io.IOException;
import java.util.List;

import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.model.GTaskList;

public interface Source {

    List<GTask> loadTasks(GTaskList list) throws IOException;

    List<GTaskList> loadLists();
}
