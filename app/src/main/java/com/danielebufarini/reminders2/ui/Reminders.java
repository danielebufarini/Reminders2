package com.danielebufarini.reminders2.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.synchronisation.LoadItems;
import com.danielebufarini.reminders2.synchronisation.SaveItems;
import com.danielebufarini.reminders2.ui.task.ManageTaskActivity;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.danielebufarini.reminders2.util.GoogleAccountHelper;
import com.danielebufarini.reminders2.util.GoogleService;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Reminders extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int REFRESH_TASKS = 3;
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String PREF_SYNC_GOOGLE_ENABLED = "syncEnabled";

    private static final int SYNCHRONISE = 1;
    private static final String NO_ACCOUNT_SETUP = "<no account set up>";
    private static final int CHANGED_GOOGLE_ACCOUNT = 2;
    private static final boolean DONT_SAVE_TASKS = false;
    private static final String LOGTAG = "Reminders";

    private TaskFragment taskFragment;
    private volatile int progressBarCounter;
    private GoogleAccountHelper accountHelper;
    private static final ApplicationCache CACHE = ApplicationCache.getInstance();

    // UI widgets
    private Spinner folders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accountHelper = new GoogleAccountHelper(this);

        if (CACHE.isAtomicLongNull())
            CACHE.setAtomicLongValue(0L);
        if (CACHE.accountName() == null)
            CACHE.accountName(accountHelper.getAccounts()[0].name);
        if (CACHE.isSyncWithGTasksEnabled() == null)
            CACHE.isSyncWithGTasksEnabled(checkGooglePlayServicesAvailable());
        if (CACHE.getFolders().isEmpty())
            authorise(accountHelper.getNames()[accountHelper.getIndex(CACHE.accountName())],
                    SYNCHRONISE,
                    new IfAlreadyAuthorised() {
                        @Override
                        public void doAction() {
                            synchroniseAndUpdateUI(DONT_SAVE_TASKS);
                    }
                },
                null
            );

        taskFragment = (TaskFragment) getFragmentManager().findFragmentById(R.id.tasksFragment);
        setupWidgets();

    }

    private String[] toArray(List<GTaskList> foldersList) {
        String[] folders = new String[foldersList.size()];
        for (int i = 0; i < foldersList.size(); ++i)
            folders[i] = foldersList.get(i).title;
        return folders;
    }

    private void setupWidgets() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent insertNewTask = new Intent(Reminders.this, ManageTaskActivity.class);
                startActivity(insertNewTask);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        folders = (Spinner) findViewById(R.id.folders);
        folders.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                toArray(CACHE.getFolders())));
        folders.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CACHE.setActiveFolder(position);
                final GTaskList taskList = CACHE.getFolders().get(position);
                /*if (taskList.tasks == null)
                    new Thread(new Runnable() {
                        public void run() {
                            LoadItems loadItems = new LoadItems(
                                    Reminders.this, CACHE.isSyncWithGTasksEnabled(), CACHE.accountName());
                            taskList.tasks = loadItems.getTasks(taskList.id);
                        }
                };*/
                taskFragment.onFolderSelected(taskList.tasks);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        View header = navigationView.getHeaderView(0);
        Spinner accountName = (Spinner) header.findViewById(R.id.account_name);

        SharedPreferences settings = getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        CACHE.accountName(settings.getString(PREF_ACCOUNT_NAME, accountHelper.getNames()[0]));
        accountName.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                accountHelper.getNames()));
        accountName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                String account = accountHelper.getNames()[position];
                if (!CACHE.accountName().equals(account)) {
                    authorise(account,
                            CHANGED_GOOGLE_ACCOUNT,
                            new IfAlreadyAuthorised() {
                                @Override
                                public void doAction() {
                                    synchroniseAndUpdateUI(true);
                                }
                            }, null /*syncButton*/);
                    CACHE.accountName(account);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        accountName.setSelection(accountHelper.getIndex(CACHE.accountName()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences settings = getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME, CACHE.accountName());
        editor.putBoolean(PREF_SYNC_GOOGLE_ENABLED, CACHE.isSyncWithGTasksEnabled());
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean checkGooglePlayServicesAvailable() {
        return !GooglePlayServicesUtil.isUserRecoverableError(
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        );
    }

    private void synchroniseAndUpdateUI(final boolean areItemsToBeSaved) {
        //final View progressBar = this.findViewById(R.id.title_refresh_progress);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ++progressBarCounter;
                //progressBar.setVisibility(View.VISIBLE);
            }
        });
        final Activity activity = this;
        new Thread(new Runnable() {
            public void run() {
                if (areItemsToBeSaved)
                    new SaveItems(
                            activity,
                            CACHE.isSyncWithGTasksEnabled(),
                            CACHE.accountName(),
                            new ArrayList<GTaskList>()
                    ).run();
                LoadItems loadItems = new LoadItems(activity, CACHE.isSyncWithGTasksEnabled(), CACHE.accountName());
                List<GTaskList> taskLists = loadItems.getLists();
                CACHE.setFolders(taskLists);
                List<String> folderNames = new ArrayList<>(taskLists.size());
                for (GTaskList folder : taskLists)
                    folderNames.add(folder.title);
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        Reminders.this, android.R.layout.simple_spinner_item, folderNames
                );
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        folders.setAdapter(adapter);
                        --progressBarCounter;
                        if (progressBarCounter == 0)
                            ;//progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private interface IfAlreadyAuthorised {
        void doAction();
    }

    private void authorise(final String accountName, final int requestCode,
                           final IfAlreadyAuthorised ifAlreadyAuthorised, final ImageButton syncButton) {
        if (!isAccountAuthorised(accountName)) {
            final Tasks googleService = GoogleService.getGoogleTasksService(this, accountName);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        googleService.tasklists().list().setMaxResults(1L).execute();
                        saveAuthorisation(accountName);
                        CACHE.isSyncWithGTasksEnabled(true);
                        ifAlreadyAuthorised.doAction();
                        Reminders.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //syncButton.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (UserRecoverableAuthIOException userRecoverableException) {
                        Intent intent = userRecoverableException.getIntent();
                        intent.putExtra("reminders_position", accountHelper.getIndex(accountName));
                        startActivityForResult(intent, requestCode);
                    } catch (IOException e) {
                        Log.e(LOGTAG, "authorise() :: cannot contact google servers - account = \""
                                + accountName + "\"", e);
                    }
                }
            }).start();
        } else
            ifAlreadyAuthorised.doAction();
    }

    private void saveAuthorisation(String accountName) {
        SharedPreferences settings = getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(accountName, true);
        editor.apply();
    }

    private boolean isAccountAuthorised(String accountName) {
        SharedPreferences settings = getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        return settings.getBoolean(accountName, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SYNCHRONISE:
                if (resultCode == Reminders.RESULT_OK) {
                    saveAuthorisation(CACHE.accountName());
                    CACHE.isSyncWithGTasksEnabled(true);
                    synchroniseAndUpdateUI(true);
                }
                break;
            case CHANGED_GOOGLE_ACCOUNT:
                if (resultCode == Reminders.RESULT_OK) {
                    int position = data.getIntExtra("reminders_position", 0);
                    saveAuthorisation(accountHelper.getNames()[position]);
                    CACHE.isSyncWithGTasksEnabled(true);
                    /*if (!selectedAccountName.equals(accountHelper[position].name))
                        switchAccount(position);*/
                }
                break;
        }
    }
}
