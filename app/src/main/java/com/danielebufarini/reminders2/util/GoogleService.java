package com.danielebufarini.reminders2.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.danielebufarini.reminders2.R;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;

import java.util.ArrayList;
import java.util.Collection;

public class GoogleService {
    public static final Collection<String> TASKS_SCOPES;

    static {
        TASKS_SCOPES = new ArrayList<>(1);
        TASKS_SCOPES.add(TasksScopes.TASKS);
    }

    public static Tasks getGoogleTasksService(final Context context, String accountName) {

        final GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, TASKS_SCOPES)
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(accountName);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new Tasks.Builder(transport, jsonFactory, credential)
                .setApplicationName(getAppName(context))
                .setHttpRequestInitializer(request -> {
                    credential.initialize(request);
                    request.setConnectTimeout(3 * 1000);  // 3 seconds connect timeout
                    request.setReadTimeout(3 * 1000);  // 3 seconds read timeout
                })
                .build();
    }

    private static String getAppName(Context context) {

        return context.getResources().getString(R.string.app_name)
                + "/" + context.getResources().getString(R.string.app_version);
    }

    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
