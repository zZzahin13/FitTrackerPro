package com.example.fittrackerpro.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fittrackerpro.utils.NotificationHelper;

/**
 * Day 8A: WorkManager background worker that posts scheduled reminder notifications
 * (Workout, Water, and Meal logging reminders).
 */
public class ReminderWorker extends Worker {

    public static final String KEY_NOTIFICATION_ID = "key_notification_id";
    public static final String KEY_TITLE = "key_title";
    public static final String KEY_MESSAGE = "key_message";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data input = getInputData();
        int notifId = input.getInt(KEY_NOTIFICATION_ID, 2001);
        String title = input.getString(KEY_TITLE);
        String message = input.getString(KEY_MESSAGE);

        if (title == null || title.isEmpty()) {
            title = "FitTrackerPro Reminder";
        }
        if (message == null || message.isEmpty()) {
            message = "Time to check in on your daily fitness and nutrition goals!";
        }

        NotificationHelper.showReminderNotification(
                getApplicationContext(),
                notifId,
                title,
                message
        );
        return Result.success();
    }
}
