package com.example.fittrackerpro.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.fittrackerpro.databinding.ActivityTrackingBinding;
import com.example.fittrackerpro.services.ActivityTrackingService;
import com.example.fittrackerpro.utils.ActivityEntryDialog;
import com.example.fittrackerpro.utils.ActivitySessionStore;
import com.example.fittrackerpro.utils.CalorieCalculator;
import com.example.fittrackerpro.viewmodel.ActivityViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

/**
 * Day 6, Part 2: Timed activity session screen backed by ActivityTrackingService.
 */
public class TrackingActivity extends AppCompatActivity {

    public static final String EXTRA_FINISH_FROM_NOTIFICATION = "extra_finish_from_notification";

    private ActivityTrackingBinding binding;
    private ActivityViewModel viewModel;

    private double weightKg = 70;
    private double heightCm = 170;
    private boolean finishPromptShowing = false;
    private boolean lowBatteryToastShown = false;

    /**
     * Day 8A: Dynamic BroadcastReceiver registered while TrackingActivity is visible.
     * Warns the user if the device battery becomes low during a timed session.
     */
    private final BroadcastReceiver lowBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || binding == null) {
                return;
            }
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_LOW.equals(action)) {
                showLowBatteryWarning(-1);
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int pct = Math.round((level * 100f) / scale);
                    if (pct <= 15) {
                        showLowBatteryWarning(pct);
                    } else {
                        binding.cardLowBatteryWarning.setVisibility(View.GONE);
                    }
                }
            }
        }
    };

    private void showLowBatteryWarning(int pct) {
        binding.cardLowBatteryWarning.setVisibility(View.VISIBLE);
        if (pct >= 0) {
            binding.tvLowBatteryMessage.setText(String.format(Locale.getDefault(),
                    "Low battery (%d%%)! Consider stopping and saving your session soon so your progress is recorded safely.",
                    pct));
        }
        if (!lowBatteryToastShown && ActivitySessionStore.get(this).isActive()) {
            lowBatteryToastShown = true;
            Toast.makeText(this,
                    "Low battery warning: save your activity session soon.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private final Handler tickHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            renderTick();
            tickHandler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this,
                            "Notification permission denied. The timer will still run in the background.",
                            Toast.LENGTH_LONG).show();
                }
                actuallyStartSession();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(ActivityViewModel.class);
        viewModel.getProfile().observe(this, profile -> {
            if (profile != null) {
                weightKg = profile.currentWeightKg;
                heightCm = profile.heightCm;
            }
        });

        ActivitySessionStore.observe(this).observe(this, this::renderState);

        binding.btnStart.setOnClickListener(v -> checkPermissionAndStart());
        binding.btnPauseResume.setOnClickListener(v -> {
            ActivitySessionStore.State s = ActivitySessionStore.get(this);
            if (s.status == ActivitySessionStore.RUNNING) {
                ActivityTrackingService.pauseSession(this);
            } else if (s.status == ActivitySessionStore.PAUSED) {
                ActivityTrackingService.resumeSession(this);
            }
        });
        binding.btnStop.setOnClickListener(v -> onStopClicked());

        handleStopIntentIfNeeded(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleStopIntentIfNeeded(intent);
    }

    private void handleStopIntentIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_FINISH_FROM_NOTIFICATION, false)) {
            intent.removeExtra(EXTRA_FINISH_FROM_NOTIFICATION);
            if (ActivitySessionStore.get(this).isActive()) {
                onStopClicked();
            }
        }
    }

    private void checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Allow session notifications?")
                    .setMessage("FitTrackerPro uses an ongoing notification to show your live timer and Pause/Stop buttons while you use other apps or lock your screen.")
                    .setPositiveButton("Continue", (d, w) ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton("Start without notification", (d, w) -> actuallyStartSession())
                    .show();
        } else {
            actuallyStartSession();
        }
    }

    private void actuallyStartSession() {
        String type = binding.spActivityType.getSelectedItem().toString();
        ActivityTrackingService.startSession(this, type);
    }

    private void renderState(ActivitySessionStore.State state) {
        if (state == null || !state.isActive()) {
            binding.spActivityType.setEnabled(true);
            binding.btnStart.setVisibility(View.VISIBLE);
            binding.groupActiveControls.setVisibility(View.GONE);
            binding.tvStatus.setText("Ready to start");
            binding.tvTimer.setText("0:00");
            binding.tvEstimate.setText("Estimated calories will update as you go");
            return;
        }

        // Lock spinner to the active session's activity type
        selectSpinnerType(state.type);
        binding.spActivityType.setEnabled(false);
        binding.btnStart.setVisibility(View.GONE);
        binding.groupActiveControls.setVisibility(View.VISIBLE);

        boolean running = state.status == ActivitySessionStore.RUNNING;
        binding.tvStatus.setText(running ? "Running • " + state.type : "Paused • " + state.type);
        binding.btnPauseResume.setText(running ? "Pause" : "Resume");
        renderTick();
    }

    private void renderTick() {
        ActivitySessionStore.State s = ActivitySessionStore.get(this);
        if (!s.isActive()) {
            return;
        }
        long elapsedMs = s.elapsedMs(System.currentTimeMillis());
        binding.tvTimer.setText(ActivitySessionStore.formatElapsed(elapsedMs));

        int minutesSoFar = Math.max(1, (int) Math.round(elapsedMs / 60000.0));
        double estKcal = CalorieCalculator.activityCalories(s.type, weightKg, minutesSoFar);
        binding.tvEstimate.setText(String.format(Locale.getDefault(),
                "≈ %d kcal burned so far (estimate)", Math.round(estKcal)));
    }

    private void onStopClicked() {
        if (finishPromptShowing) {
            return;
        }
        ActivitySessionStore.State s = ActivitySessionStore.get(this);
        if (!s.isActive()) {
            return;
        }

        // Pause while the user decides what to do
        if (s.status == ActivitySessionStore.RUNNING) {
            ActivityTrackingService.pauseSession(this);
            s = ActivitySessionStore.get(this);
        }

        long elapsedMs = s.elapsedMs(System.currentTimeMillis());
        long startedAt = s.startedAt;
        String type = s.type != null ? s.type : "Walking";

        // Sessions under 30 seconds are too short to log as a 1-minute activity
        if (elapsedMs < 30_000L) {
            finishPromptShowing = true;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Session too short")
                    .setMessage("This session is under 30 seconds. Keep going, or discard it?")
                    .setPositiveButton("Keep going", (d, w) -> {
                        finishPromptShowing = false;
                        ActivityTrackingService.resumeSession(this);
                    })
                    .setNegativeButton("Discard", (d, w) -> {
                        finishPromptShowing = false;
                        endAndClearService();
                    })
                    .setOnCancelListener(d -> finishPromptShowing = false)
                    .show();
            return;
        }

        int minutes = Math.max(1, (int) Math.round(elapsedMs / 60000.0));
        finishPromptShowing = true;

        ActivityEntryDialog.showFinish(
                this,
                type,
                minutes,
                weightKg,
                heightCm,
                () -> {
                    // Discard clicked
                    finishPromptShowing = false;
                    endAndClearService();
                },
                (savedType, unusedDayStart, savedMinutes, km, steps) -> {
                    // Save clicked: record the session in Room via ActivityViewModel
                    finishPromptShowing = false;
                    viewModel.addLog(savedType, startedAt, savedMinutes, km, steps, () -> {
                        endAndClearService();
                        Toast.makeText(this, "Session saved to Activity history",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                });
    }

    private void endAndClearService() {
        ActivitySessionStore.clear(this);
        ActivityTrackingService.stopSession(this);
    }

    @SuppressWarnings("unchecked")
    private void selectSpinnerType(String type) {
        if (type == null || binding.spActivityType.getAdapter() == null) {
            return;
        }
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.spActivityType.getAdapter();
        int pos = adapter.getPosition(type);
        if (pos >= 0) {
            binding.spActivityType.setSelection(pos);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(lowBatteryReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(lowBatteryReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tickHandler.post(tickRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tickHandler.removeCallbacks(tickRunnable);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
