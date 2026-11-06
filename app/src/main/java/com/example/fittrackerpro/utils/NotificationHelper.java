package com.example.fittrackerpro.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.activities.MainActivity;

/**
 * Creates the app's notification channels and posts reminder notifications.
 * Android 8.0+ (our minSdk is 26) requires a channel before any notification appears.
 */
public class NotificationHelper {

    public static final String CHANNEL_SESSION = "activity_session";
    public static final String CHANNEL_REMINDERS = "daily_reminders";

    private NotificationHelper() {
    }

    public static void createChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        // LOW importance: shows in the shade without sound, right for an ongoing timer
        NotificationChannel session = new NotificationChannel(
                CHANNEL_SESSION, "Activity sessions", NotificationManager.IMPORTANCE_LOW);
        session.setDescription("Shows the timer while an activity session is running");
        session.setShowBadge(false);
        manager.createNotificationChannel(session);

        // DEFAULT importance: standard sound/alert for scheduled workout, water, and meal reminders
        NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS, "Fitness & hydration reminders", NotificationManager.IMPORTANCE_DEFAULT);
        reminders.setDescription("Scheduled workout, water, and meal logging reminders");
        manager.createNotificationChannel(reminders);
    }

    public static void showReminderNotification(Context context,
                                                int notificationId,
                                                String title,
                                                String message) {
        createChannels(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_nav_workout)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentPi)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}
