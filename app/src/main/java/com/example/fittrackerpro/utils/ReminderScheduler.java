package com.example.fittrackerpro.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.fittrackerpro.workers.ReminderWorker;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Day 8A: Saves reminder preferences (Workout, Water, Meals) and schedules/cancels
 * daily PeriodicWorkRequests through WorkManager.
 */
public class ReminderScheduler {

    public static final String KIND_WORKOUT = "workout";
    public static final String KIND_WATER = "water";
    public static final String KIND_MEAL = "meal";

    private static final String PREFS = "fit_tracker_reminders";

    private ReminderScheduler() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c, String kind) {
        return prefs(c).getBoolean(kind + "_enabled", false);
    }

    public static int getHour(Context c, String kind, int defaultHour) {
        return prefs(c).getInt(kind + "_hour", defaultHour);
    }

    public static int getMinute(Context c, String kind, int defaultMinute) {
        return prefs(c).getInt(kind + "_minute", defaultMinute);
    }

    public static String formatTime(int hourOfDay, int minute) {
        int hour12 = hourOfDay % 12;
        if (hour12 == 0) hour12 = 12;
        String amPm = hourOfDay < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);
    }

    public static void saveAndSchedule(Context c,
                                       String kind,
                                       boolean enabled,
                                       int hourOfDay,
                                       int minute) {
        prefs(c).edit()
                .putBoolean(kind + "_enabled", enabled)
                .putInt(kind + "_hour", hourOfDay)
                .putInt(kind + "_minute", minute)
                .apply();

        if (enabled) {
            scheduleDailyReminder(c, kind, hourOfDay, minute);
        } else {
            cancelReminder(c, kind);
        }
    }

    public static void scheduleDailyReminder(Context c, String kind, int hourOfDay, int minute) {
        long initialDelayMs = computeDelayUntilNext(hourOfDay, minute);
        Data input = buildInputData(kind);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .addTag("reminder_" + kind)
                .build();

        WorkManager.getInstance(c.getApplicationContext()).enqueueUniquePeriodicWork(
                uniqueWorkName(kind),
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    public static void cancelReminder(Context c, String kind) {
        WorkManager.getInstance(c.getApplicationContext())
                .cancelUniqueWork(uniqueWorkName(kind));
    }

    /**
     * Enqueues an immediate OneTimeWorkRequest so the user can test WorkManager
     * notifications right away from the Settings screen without waiting.
     */
    public static void triggerTestReminderNow(Context c, String kind) {
        Data input = buildInputData(kind);
        OneTimeWorkRequest testRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInputData(input)
                .build();
        WorkManager.getInstance(c.getApplicationContext()).enqueue(testRequest);
    }

    /**
     * Called by BootReminderReceiver after device reboot to restore all active reminders.
     */
    public static void rescheduleAllEnabled(Context c) {
        if (isEnabled(c, KIND_WORKOUT)) {
            scheduleDailyReminder(c, KIND_WORKOUT,
                    getHour(c, KIND_WORKOUT, 18), getMinute(c, KIND_WORKOUT, 0));
        }
        if (isEnabled(c, KIND_WATER)) {
            scheduleDailyReminder(c, KIND_WATER,
                    getHour(c, KIND_WATER, 11), getMinute(c, KIND_WATER, 0));
        }
        if (isEnabled(c, KIND_MEAL)) {
            scheduleDailyReminder(c, KIND_MEAL,
                    getHour(c, KIND_MEAL, 13), getMinute(c, KIND_MEAL, 0));
        }
    }

    private static String uniqueWorkName(String kind) {
        return "daily_reminder_" + kind;
    }

    private static Data buildInputData(String kind) {
        int id;
        String title;
        String message;
        if (KIND_WORKOUT.equals(kind)) {
            id = 2101;
            title = "Workout Reminder";
            message = "Ready for today's training session? Open FitTrackerPro to start your workout.";
        } else if (KIND_WATER.equals(kind)) {
            id = 2102;
            title = "Hydration Check";
            message = "Drink a 250 ml glass of water and log it on your dashboard!";
        } else {
            id = 2103;
            title = "Meal Logging Reminder";
            message = "Don't forget to log your meal to stay on top of your calorie and macro targets.";
        }
        return new Data.Builder()
                .putInt(ReminderWorker.KEY_NOTIFICATION_ID, id)
                .putString(ReminderWorker.KEY_TITLE, title)
                .putString(ReminderWorker.KEY_MESSAGE, message)
                .build();
    }

    private static long computeDelayUntilNext(int hourOfDay, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, hourOfDay);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return Math.max(1000L, target.getTimeInMillis() - now.getTimeInMillis());
    }
}
