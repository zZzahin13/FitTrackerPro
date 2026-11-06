package com.example.fittrackerpro.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.activities.TrackingActivity;
import com.example.fittrackerpro.utils.ActivitySessionStore;
import com.example.fittrackerpro.utils.NotificationHelper;

/**
 * Day 6, Part 2: Foreground Service for timed activity sessions.
 *
 * Keeps a persistent notification in the status bar with a live chronometer
 * and Pause/Resume + Stop action buttons while the user leaves the app.
 */
public class ActivityTrackingService extends Service {

    public static final String ACTION_START = "com.example.fittrackerpro.action.START_SESSION";
    public static final String ACTION_PAUSE = "com.example.fittrackerpro.action.PAUSE_SESSION";
    public static final String ACTION_RESUME = "com.example.fittrackerpro.action.RESUME_SESSION";
    public static final String ACTION_STOP = "com.example.fittrackerpro.action.STOP_SESSION";
    public static final String EXTRA_ACTIVITY_TYPE = "extra_activity_type";

    private static final int NOTIFICATION_ID = 602;

    public static void startSession(Context context, String activityType) {
        Intent intent = new Intent(context, ActivityTrackingService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_ACTIVITY_TYPE, activityType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void pauseSession(Context context) {
        Intent intent = new Intent(context, ActivityTrackingService.class);
        intent.setAction(ACTION_PAUSE);
        context.startService(intent);
    }

    public static void resumeSession(Context context) {
        Intent intent = new Intent(context, ActivityTrackingService.class);
        intent.setAction(ACTION_RESUME);
        context.startService(intent);
    }

    public static void stopSession(Context context) {
        Intent intent = new Intent(context, ActivityTrackingService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_START.equals(action)) {
            String type = intent.getStringExtra(EXTRA_ACTIVITY_TYPE);
            if (type == null || type.isEmpty()) {
                type = "Walking";
            }
            ActivitySessionStore.State state = ActivitySessionStore.start(this, type);
            promoteToForeground(state);

        } else if (ACTION_PAUSE.equals(action)) {
            ActivitySessionStore.State state = ActivitySessionStore.pause(this);
            updateNotification(state);

        } else if (ACTION_RESUME.equals(action)) {
            ActivitySessionStore.State state = ActivitySessionStore.resume(this);
            updateNotification(state);

        } else if (ACTION_STOP.equals(action)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();

        } else {
            // Recreated by the system after process death: restore from SharedPreferences
            ActivitySessionStore.State state = ActivitySessionStore.get(this);
            if (state.isActive()) {
                promoteToForeground(state);
            } else {
                stopSelf();
            }
        }

        return START_STICKY;
    }

    private void promoteToForeground(ActivitySessionStore.State state) {
        Notification notification = buildNotification(state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(ActivitySessionStore.State state) {
        if (!state.isActive()) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(state));
        }
    }

    private Notification buildNotification(ActivitySessionStore.State state) {
        long now = System.currentTimeMillis();
        long elapsed = state.elapsedMs(now);
        boolean running = state.status == ActivitySessionStore.RUNNING;

        // Tapping the notification opens TrackingActivity
        Intent openIntent = new Intent(this, TrackingActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(
                this, 10, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action 1: Pause or Resume directly from the notification
        Intent toggleIntent = new Intent(this, ActivityTrackingService.class);
        toggleIntent.setAction(running ? ACTION_PAUSE : ACTION_RESUME);
        PendingIntent togglePi = PendingIntent.getService(
                this, 11, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action 2: Stop opens TrackingActivity with EXTRA_FINISH_FROM_NOTIFICATION so the save dialog appears
        Intent stopIntent = new Intent(this, TrackingActivity.class);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        stopIntent.putExtra(TrackingActivity.EXTRA_FINISH_FROM_NOTIFICATION, true);
        PendingIntent stopPi = PendingIntent.getActivity(
                this, 12, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = state.type != null ? state.type : "Activity session";
        String subtitle = running
                ? "Session in progress"
                : "Paused at " + ActivitySessionStore.formatElapsed(elapsed);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, NotificationHelper.CHANNEL_SESSION)
                .setSmallIcon(R.drawable.ic_nav_activity)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(contentPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(0, running ? "Pause" : "Resume", togglePi)
                .addAction(0, "Stop", stopPi);

        // Built-in Android chronometer ticks every second in the status bar while running
        if (running) {
            builder.setWhen(now - elapsed)
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            builder.setShowWhen(false)
                    .setUsesChronometer(false);
        }

        return builder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
