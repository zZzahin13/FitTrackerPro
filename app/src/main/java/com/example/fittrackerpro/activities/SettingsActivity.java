package com.example.fittrackerpro.activities;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.fittrackerpro.databinding.ActivitySettingsBinding;
import com.example.fittrackerpro.utils.NotificationHelper;
import com.example.fittrackerpro.utils.ReminderScheduler;

/**
 * Day 8A: Settings screen to turn Workout, Water, and Meal reminders on/off,
 * choose their daily schedule times, and trigger a test WorkManager notification.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    private int workoutHour, workoutMin;
    private int waterHour, waterMin;
    private int mealHour, mealMin;
    private boolean updatingUi = false;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this,
                            "Notification permission is required to show scheduled reminders.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        NotificationHelper.createChannels(this);
        loadSavedSettings();

        binding.swWorkout.setOnCheckedChangeListener((btn, checked) -> {
            if (updatingUi) return;
            if (checked) ensureNotificationPermission();
            ReminderScheduler.saveAndSchedule(this,
                    ReminderScheduler.KIND_WORKOUT, checked, workoutHour, workoutMin);
            showStatusToast("Workout reminder", checked, workoutHour, workoutMin);
        });

        binding.swWater.setOnCheckedChangeListener((btn, checked) -> {
            if (updatingUi) return;
            if (checked) ensureNotificationPermission();
            ReminderScheduler.saveAndSchedule(this,
                    ReminderScheduler.KIND_WATER, checked, waterHour, waterMin);
            showStatusToast("Water reminder", checked, waterHour, waterMin);
        });

        binding.swMeal.setOnCheckedChangeListener((btn, checked) -> {
            if (updatingUi) return;
            if (checked) ensureNotificationPermission();
            ReminderScheduler.saveAndSchedule(this,
                    ReminderScheduler.KIND_MEAL, checked, mealHour, mealMin);
            showStatusToast("Meal reminder", checked, mealHour, mealMin);
        });

        binding.btnPickWorkoutTime.setOnClickListener(v ->
                pickTime(workoutHour, workoutMin, (h, m) -> {
                    workoutHour = h;
                    workoutMin = m;
                    binding.tvWorkoutTime.setText("Scheduled at " + ReminderScheduler.formatTime(h, m));
                    ReminderScheduler.saveAndSchedule(this,
                            ReminderScheduler.KIND_WORKOUT, binding.swWorkout.isChecked(), h, m);
                    if (binding.swWorkout.isChecked()) {
                        showStatusToast("Workout reminder", true, h, m);
                    }
                }));

        binding.btnPickWaterTime.setOnClickListener(v ->
                pickTime(waterHour, waterMin, (h, m) -> {
                    waterHour = h;
                    waterMin = m;
                    binding.tvWaterTime.setText("Scheduled at " + ReminderScheduler.formatTime(h, m));
                    ReminderScheduler.saveAndSchedule(this,
                            ReminderScheduler.KIND_WATER, binding.swWater.isChecked(), h, m);
                    if (binding.swWater.isChecked()) {
                        showStatusToast("Water reminder", true, h, m);
                    }
                }));

        binding.btnPickMealTime.setOnClickListener(v ->
                pickTime(mealHour, mealMin, (h, m) -> {
                    mealHour = h;
                    mealMin = m;
                    binding.tvMealTime.setText("Scheduled at " + ReminderScheduler.formatTime(h, m));
                    ReminderScheduler.saveAndSchedule(this,
                            ReminderScheduler.KIND_MEAL, binding.swMeal.isChecked(), h, m);
                    if (binding.swMeal.isChecked()) {
                        showStatusToast("Meal reminder", true, h, m);
                    }
                }));

        binding.btnTestReminderNow.setOnClickListener(v -> {
            ensureNotificationPermission();
            ReminderScheduler.triggerTestReminderNow(this, ReminderScheduler.KIND_WORKOUT);
            Toast.makeText(this,
                    "Test reminder enqueued via WorkManager!",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSavedSettings() {
        updatingUi = true;
        workoutHour = ReminderScheduler.getHour(this, ReminderScheduler.KIND_WORKOUT, 18);
        workoutMin = ReminderScheduler.getMinute(this, ReminderScheduler.KIND_WORKOUT, 0);
        binding.swWorkout.setChecked(ReminderScheduler.isEnabled(this, ReminderScheduler.KIND_WORKOUT));
        binding.tvWorkoutTime.setText("Scheduled at " + ReminderScheduler.formatTime(workoutHour, workoutMin));

        waterHour = ReminderScheduler.getHour(this, ReminderScheduler.KIND_WATER, 11);
        waterMin = ReminderScheduler.getMinute(this, ReminderScheduler.KIND_WATER, 0);
        binding.swWater.setChecked(ReminderScheduler.isEnabled(this, ReminderScheduler.KIND_WATER));
        binding.tvWaterTime.setText("Scheduled at " + ReminderScheduler.formatTime(waterHour, waterMin));

        mealHour = ReminderScheduler.getHour(this, ReminderScheduler.KIND_MEAL, 13);
        mealMin = ReminderScheduler.getMinute(this, ReminderScheduler.KIND_MEAL, 0);
        binding.swMeal.setChecked(ReminderScheduler.isEnabled(this, ReminderScheduler.KIND_MEAL));
        binding.tvMealTime.setText("Scheduled at " + ReminderScheduler.formatTime(mealHour, mealMin));
        updatingUi = false;
    }

    private interface OnTimePicked {
        void onPicked(int hourOfDay, int minute);
    }

    private void pickTime(int initialHour, int initialMin, OnTimePicked callback) {
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> callback.onPicked(hourOfDay, minute),
                initialHour,
                initialMin,
                false
        ).show();
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void showStatusToast(String label, boolean enabled, int h, int m) {
        if (enabled) {
            Toast.makeText(this,
                    label + " scheduled daily for " + ReminderScheduler.formatTime(h, m),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, label + " turned off", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
