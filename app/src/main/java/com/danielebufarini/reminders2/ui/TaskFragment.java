package com.danielebufarini.reminders2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.ui.task.ManageTaskActivity;
import com.danielebufarini.reminders2.ui.task.TaskAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TaskFragment extends Fragment implements Serializable {
    public static final String TASK = "task";
    public static final String TASK_POSITION = "task_position";

    private List<GTask> deletedTasks = new ArrayList<>(30);
    private TaskAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        ListView tasks = getActivity().findViewById(R.id.tasksList);
        adapter = new TaskAdapter(getActivity(), R.layout.task, new ArrayList<>(30));
        tasks.setAdapter(adapter);
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        tasks,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {

                                return true;
                            }

                            @Override
                            public void onDismiss(ListView tasks, int[] reverseSortedPositions) {

                                for (final int position : reverseSortedPositions) {
                                    final GTask undoItem = adapter.getItem(position);
                                    adapter.remove(undoItem);
                                    deletedTasks.add(undoItem);
                                    Snackbar.make(tasks, "Deleted task '" + undoItem.title + "'",
                                            Snackbar.LENGTH_LONG)
                                            .setAction("Undo", v -> {

                                                adapter.insert(undoItem, position);
                                                deletedTasks.remove(undoItem);
                                            })
                                            .show();
                                }
                            }
                        }
                );
        tasks.setOnTouchListener(touchListener);
        // Setting getActivity() scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        tasks.setOnScrollListener(touchListener.makeScrollListener());
        tasks.setOnItemClickListener((parent, view, position, id) -> {

            Intent viewTask = new Intent(TaskFragment.this.getActivity(), ManageTaskActivity.class);
            viewTask.putExtra(TASK, adapter.getItem(position));
            viewTask.putExtra(TASK_POSITION, position);
            startActivityForResult(viewTask, Reminders.REFRESH_TASKS);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Reminders.REFRESH_TASKS && data != null) {
            GTask task = (GTask) data.getSerializableExtra(TASK);
            int position = data.getIntExtra(TASK_POSITION, 0);
            onRefreshTasksList(task, position);
        }
    }

    public void onFolderSelected(List<GTask> tasks) {

        adapter.clear();
        if (tasks != null)
            adapter.addAll(tasks);
    }

    public void onRefreshTasksList(GTask task, int position) {

        adapter.remove(adapter.getItem(position));
        adapter.insert(task, position);
    }
}
