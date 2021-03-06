package com.danielebufarini.reminders2.ui;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.database.RemindersDatabase;
import com.danielebufarini.reminders2.model.GTaskList;
import com.danielebufarini.reminders2.services.AsyncHandler;
import com.danielebufarini.reminders2.synchronisation.LoadItems;
import com.danielebufarini.reminders2.synchronisation.SaveItems;
import com.danielebufarini.reminders2.ui.task.ManageTaskActivity;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.danielebufarini.reminders2.util.GoogleAccountHelper;
import com.danielebufarini.reminders2.util.GoogleService;
import com.facebook.stetho.Stetho;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Reminders extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final int REFRESH_TASKS = 4;
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String PREF_SYNC_GOOGLE_ENABLED = "syncEnabled";
    public static final boolean LOGV = true;

    private static final int SYNCHRONISE = 1;
    private static final int CHANGED_GOOGLE_ACCOUNT = 2;
    private static final int RC_SIGN_IN = 3;
    private static final boolean DONT_SAVE_TASKS = false;
    private static final String LOGTAG = "Reminders";
    private static final ApplicationCache CACHE = ApplicationCache.INSTANCE;

    private volatile AtomicInteger progressBarCounter = new AtomicInteger();
    private GoogleAccountHelper accountHelper;
    private final Map<Integer, Command> commands = new ConcurrentHashMap<>(3);
    Spinner lists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        accountHelper = new GoogleAccountHelper(this);
        if (CACHE.isAtomicLongNull())
            CACHE.setAtomicLongValue(0L);
        if (CACHE.isSyncWithGTasksEnabled() == null)
            CACHE.isSyncWithGTasksEnabled(checkGooglePlayServicesAvailable());
        CACHE.setDatabase(Room.databaseBuilder(getApplicationContext(),
                RemindersDatabase.class, RemindersDatabase.NAME).build());
        setupWidgets();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        commands.put(SYNCHRONISE, (requestCode, resultCode, data) -> {
            if (resultCode == Reminders.RESULT_OK) {
                saveAuthorisation(CACHE.accountName());
                CACHE.isSyncWithGTasksEnabled(true);
                synchroniseAndUpdateUI(true);
            }
        });
        commands.put(CHANGED_GOOGLE_ACCOUNT, (requestCode, resultCode, data) -> {
            if (resultCode == Reminders.RESULT_OK) {
                int position = data.getIntExtra("reminders_position", 0);
                saveAuthorisation(CACHE.accountName());
                CACHE.isSyncWithGTasksEnabled(true);
            }
        });
        commands.put(RC_SIGN_IN, (requestCode, resultCode, data) -> {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                setupProfileWidgets(account, navigationView);
            } catch (ApiException e) {
                CACHE.isSyncWithGTasksEnabled(false);
            }
        });
        if (CACHE.isSyncWithGTasksEnabled()) {
            setupGoogleAccount(navigationView);
        }
        Stetho.initializeWithDefaults(this);
    }

    private void setupGoogleAccount(NavigationView navigationView) {

        GoogleSignInOptions googleSignInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail().requestProfile()
                        .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } else {
            setupProfileWidgets(account, navigationView.getHeaderView(0));
        }
    }

    private void setupProfileWidgets(GoogleSignInAccount account, View header) {

        String name = account.getAccount().name;
        authoriseGoogleAccount(name);
        TextView userName = header.findViewById(R.id.user_name);
        userName.setText(account.getDisplayName());
        TextView accountName = header.findViewById(R.id.account_name);
        accountName.setText(name);
        ImageView photo = header.findViewById(R.id.profile_image);
        loadProfileImage(photo, account);
    }

    @Override
    protected void onStop() {

//        final boolean syncWithGoogle = isSyncWithGTasksEnabled && isNetworkAvailable();
        new Thread(new SaveItems(this, true, CACHE.accountName(), CACHE.getLists())).start();
        super.onStop();
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView image;

        public DownloadImageTask(ImageView image) {

            this.image = image;
        }

        protected Bitmap doInBackground(String... urls) {

            String urldisplay = urls[0];
            Bitmap icon = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                icon = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
            }
            return icon;
        }

        protected void onPostExecute(Bitmap result) {

            image.setImageBitmap(result);
        }
    }

    private String[] toArray(List<GTaskList> lists) {

        String[] folders = new String[lists.size()];
        for (int i = 0, j = lists.size(); i < j; ++i) {
            folders[i] = lists.get(i).title;
        }
        return folders;
    }

    private void setupWidgets() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent insertNewTask = new Intent(Reminders.this, ManageTaskActivity.class);
            startActivity(insertNewTask);
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    private void loadProfileImage(ImageView image, GoogleSignInAccount account) {

        Uri uri = account.getPhotoUrl();
        assert uri != null;
        new DownloadImageTask(image).execute(uri.toString());
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean checkGooglePlayServicesAvailable() {

        return !GooglePlayServicesUtil.isUserRecoverableError(
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        );
    }

    private void synchroniseAndUpdateUI(final boolean areItemsToBeSaved) {

        progressBarCounter.incrementAndGet();
        AppCompatActivity activity = this;
        AsyncHandler.post(() -> {
            if (areItemsToBeSaved)
                new SaveItems(
                        activity,
                        CACHE.isSyncWithGTasksEnabled(),
                        CACHE.accountName(),
                        new ArrayList<>()
                ).run();
            LoadItems loadItems = new LoadItems(activity, CACHE.isSyncWithGTasksEnabled(),
                    CACHE.accountName());
            List<GTaskList> taskLists = loadItems.getLists();
            CACHE.setFolders(taskLists);
            List<String> folderNames = new ArrayList<>(taskLists.size());
            for (GTaskList list : taskLists) {
                folderNames.add(list.title);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    Reminders.this, android.R.layout.simple_spinner_item, folderNames);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (progressBarCounter.decrementAndGet() == 0) {
                activity.runOnUiThread(() -> {
                    showTaskFragment();
                    lists.setAdapter(adapter);
                });
            }
        });
    }

    private void showTaskFragment() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        TaskFragment fragment = (TaskFragment) fragmentManager.findFragmentById(R.id.main);
        if (fragment == null) {
            TaskFragment taskFragment = new TaskFragment();
            fragmentManager.beginTransaction().replace(R.id.waiting, taskFragment).commit();
            ProgressBar progressBar = findViewById(R.id.waitingProgressBar);
            progressBar.setVisibility(View.GONE);
            lists = findViewById(R.id.lists);
            lists.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                    toArray(CACHE.getLists())));
            lists.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    CACHE.setActiveFolder(position);
                    GTaskList list = CACHE.getLists().get(position);
                    /*if (list.tasks == null)
                        new Thread(new Runnable() {
                            public void run() {
                                LoadItems loadItems = new LoadItems(
                                        Reminders.this, CACHE.isSyncWithGTasksEnabled(), CACHE.accountName());
                                list.tasks = loadItems.getTasks(list.id);
                            }
                    };*/
                    taskFragment.onFolderSelected(list.tasks);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            FloatingActionButton floatingActionButton = findViewById(R.id.fab);
            floatingActionButton.setVisibility(View.VISIBLE);
        }
    }

    private interface IfAlreadyAuthorised {

        void doAction();
    }

    private void authorise(final String accountName, final int requestCode,
                           final IfAlreadyAuthorised ifAlreadyAuthorised, final ImageButton syncButton) {

        if (!isAccountAuthorised(accountName)) {
            Tasks googleService = GoogleService.getGoogleTasksService(this, accountName);
            new Thread(() -> {
                try {
                    googleService.tasklists().list().setMaxResults(1L).execute();
                    saveAuthorisation(accountName);
                    CACHE.isSyncWithGTasksEnabled(true);
                    ifAlreadyAuthorised.doAction();
                    Reminders.this.runOnUiThread(() -> {
                        //syncButton.setVisibility(View.VISIBLE);
                    });
                } catch (UserRecoverableAuthIOException userRecoverableException) {
                    Intent intent = userRecoverableException.getIntent();
                    intent.putExtra("reminders_position", accountHelper.getIndex(accountName));
                    startActivityForResult(intent, requestCode);
                } catch (IOException e) {
                    Log.e(LOGTAG, "authorise() :: cannot contact google servers - account = \""
                            + accountName + "\"", e);
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

    interface Command {

        void execute(int requestCode, int resultCode, Intent data);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        Command command = commands.get(requestCode);
        if (command != null)
            command.execute(requestCode, resultCode, data);
    }

    private void authoriseGoogleAccount(String accountName) {

        CACHE.accountName(accountName);
        if (CACHE.getLists().isEmpty())
            authorise(accountName,
                    SYNCHRONISE,
                    () -> synchroniseAndUpdateUI(DONT_SAVE_TASKS),
                    null
            );
    }
}
