package com.example.fittrackerpro.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.fittrackerpro.utils.ReminderScheduler;

/**
 * Day 8A: Static BroadcastReceiver registered in AndroidManifest.xml for BOOT_COMPLETED.
 * Re-schedules enabled workout, water, and meal reminders after a device restart.
 */
public class BootReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ReminderScheduler.rescheduleAllEnabled(context);
        }
    }
}
