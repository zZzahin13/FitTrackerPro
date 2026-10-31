package com.example.fittrackerpro.activities;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fittrackerpro.adapters.PlanExerciseAdapter;
import com.example.fittrackerpro.database.entity.PlanExerciseItem;
import com.example.fittrackerpro.databinding.ActivityWorkoutDayBinding;
import com.example.fittrackerpro.utils.DateUtil;
import com.example.fittrackerpro.viewmodel.WorkoutDayViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutDayActivity extends AppCompatActivity
        implements PlanExerciseAdapter.Listener {

    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_DAY = "day_of_week";

    private ActivityWorkoutDayBinding binding;
    private WorkoutDayViewModel viewModel;
    private PlanExerciseAdapter adapter;
    private String planId;
    private List<PlanExerciseItem> currentItems = new ArrayList<>();

    // The screen reads the clock 4 times per second and redraws the timers
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null) {
                updateTimers();
            }
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkoutDayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        planId = getIntent().getStringExtra(EXTRA_PLAN_ID);
        int day = getIntent().getIntExtra(EXTRA_DAY, 0);
        if (planId == null || day < 1 || day > 7) {
            finish();
            return;
        }

        // Keep the screen awake while exercising
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(DateUtil.dayName(day) + " workout");
        }

        viewModel = new ViewModelProvider(this).get(WorkoutDayViewModel.class);
        viewModel.init(planId, day);

        adapter = new PlanExerciseAdapter(this);
        binding.rvExercises.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExercises.setAdapter(adapter);

        viewModel.getExercises().observe(this, items -> {
            currentItems = items;
            adapter.submitList(items);

            long weekStart = DateUtil.startOfWeek();
            int done = 0;
            for (PlanExerciseItem item : items) {
                if (item.completedOn >= weekStart) done++;
            }
            binding.tvProgress.setText(done + " of " + items.size() + " exercises done this week");
        });

        binding.btnTimer.setOnClickListener(v -> {
            viewModel.toggleTimer();
            updateTimers();
        });
        binding.btnSkipRest.setOnClickListener(v -> {
            viewModel.skipRest();
            updateTimers();
        });
        binding.btnFinish.setOnClickListener(v -> finishWorkout());

        // Back button: warn if a workout is in progress
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void updateTimers() {
        binding.tvTimer.setText(formatTime(viewModel.getElapsedMs()));
        binding.btnTimer.setText(viewModel.isRunning() ? "Pause"
                : viewModel.hasStarted() ? "Resume" : "Start workout");

        if (viewModel.isRestActive()) {
            long remaining = viewModel.getRestRemainingMs();
            if (remaining == 0) {
                viewModel.skipRest();
                binding.cardRest.setVisibility(View.GONE);
                restOverAlert();
            } else {
                binding.cardRest.setVisibility(View.VISIBLE);
                binding.tvRest.setText("Rest: " + ((remaining + 999) / 1000) + " s");
            }
        } else {
            binding.cardRest.setVisibility(View.GONE);
        }
    }

    private void restOverAlert() {
        Toast.makeText(this, "Rest is over. Next set!", Toast.LENGTH_SHORT).show();
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP);
            handler.postDelayed(tone::release, 600);
        } catch (RuntimeException ignored) {
            // Some devices cannot create a tone; the toast is enough
        }
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void finishWorkout() {
        if (viewModel.getElapsedMs() <= 0) {
            Toast.makeText(this, "Start the timer first", Toast.LENGTH_SHORT).show();
            return;
        }

        long weekStart = DateUtil.startOfWeek();
        boolean allDone = !currentItems.isEmpty();
        for (PlanExerciseItem item : currentItems) {
            if (item.completedOn < weekStart) allDone = false;
        }
        final boolean completed = allDone;

        String message = completed
                ? "Save this workout to your history?"
                : "Some exercises are not ticked. Save it as a partial workout?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Finish workout")
                .setMessage(message)
                .setPositiveButton("Save", (d, w) -> {
                    binding.btnFinish.setEnabled(false);
                    viewModel.finishWorkout(planId, completed, kcal -> {
                        Toast.makeText(this, "Workout saved. About " + Math.round(kcal)
                                + " kcal burned (estimate)", Toast.LENGTH_LONG).show();
                        finish();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmLeave() {
        if (!viewModel.hasStarted()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Leave workout?")
                .setMessage("Your timer will be lost unless you save with Finish workout.")
                .setPositiveButton("Leave", (d, w) -> finish())
                .setNegativeButton("Stay", null)
                .show();
    }

    @Override
    public void onCheckedChanged(PlanExerciseItem item, boolean checked) {
        viewModel.setCompleted(item, checked);
    }

    @Override
    public void onItemClick(PlanExerciseItem item) {
        Intent i = new Intent(this, ExerciseDetailActivity.class);
        i.putExtra(ExerciseDetailActivity.EXTRA_EXERCISE_ID, item.exerciseId);
        startActivity(i);
    }

    @Override
    public void onRestClick(PlanExerciseItem item) {
        viewModel.startRest(item.restSeconds);
        updateTimers();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}