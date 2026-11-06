package com.example.fittrackerpro.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.fittrackerpro.workers.SyncWorker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Day 8B: Enqueues Firestore cloud sync jobs through WorkManager and tracks
 * the last sync timestamp and status for display on ProfileFragment.
 */
public class SyncManager {

    public static final String UNIQUE_WORK_NAME = "fittracker_firestore_sync";
    private static final String PREFS = "fit_tracker_sync_prefs";
    private static final String KEY_LAST_TIME = "last_sync_time";
    private static final String KEY_LAST_COUNT = "last_sync_count";
    private static final String KEY_LAST_ERROR = "last_sync_error";

    private SyncManager() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void triggerSyncNow(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag(UNIQUE_WORK_NAME)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );
    }

    public static LiveData<List<WorkInfo>> observeSyncWork(Context context) {
        return WorkManager.getInstance(context.getApplicationContext())
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME);
    }

    public static void recordSyncSuccess(Context context, int count) {
        prefs(context).edit()
                .putLong(KEY_LAST_TIME, System.currentTimeMillis())
                .putInt(KEY_LAST_COUNT, count)
                .remove(KEY_LAST_ERROR)
                .apply();
    }

    public static void recordSyncError(Context context, String error) {
        prefs(context).edit()
                .putString(KEY_LAST_ERROR, error)
                .apply();
    }

    public static String getLastSyncSummary(Context context) {
        SharedPreferences p = prefs(context);
        String err = p.getString(KEY_LAST_ERROR, null);
        if (err != null && !err.isEmpty()) {
            return "Last sync failed (check internet or Firestore rules)";
        }
        long lastTime = p.getLong(KEY_LAST_TIME, 0L);
        if (lastTime <= 0L) {
            return "Not synced to Cloud Firestore yet";
        }
        int lastCount = p.getInt(KEY_LAST_COUNT, 0);
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM, hh:mm a", Locale.getDefault());
        return String.format(Locale.getDefault(),
                "Last synced: %s (%d record%s uploaded)",
                fmt.format(new Date(lastTime)),
                lastCount,
                lastCount == 1 ? "" : "s");
    }
}
