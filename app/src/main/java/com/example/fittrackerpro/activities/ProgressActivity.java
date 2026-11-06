package com.example.fittrackerpro.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.adapters.WeightHistoryAdapter;
import com.example.fittrackerpro.customviews.TrendChartView;
import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.database.entity.FoodLogItem;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WeightHistory;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.databinding.ActivityProgressBinding;
import com.example.fittrackerpro.utils.BMIUtil;
import com.example.fittrackerpro.utils.DateUtil;
import com.example.fittrackerpro.viewmodel.ProgressViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Day 7: Progress & Analytics screen.
 * Shows weight & BMI line chart, 7-day / 30-day calorie & active-minute bar charts,
 * workout completion & streak, weight history table, and Implicit Intent sharing.
 */
public class ProgressActivity extends AppCompatActivity {

    private ActivityProgressBinding binding;
    private ProgressViewModel viewModel;
    private WeightHistoryAdapter weightAdapter;

    private UserProfile currentProfile;
    private List<WeightHistory> weightsAsc = new ArrayList<>();
    private List<FoodLogItem> monthFoods = new ArrayList<>();
    private List<ActivityLog> monthActivities = new ArrayList<>();
    private List<WorkoutSession> allSessions = new ArrayList<>();
    private List<DaySummary> planSummaries = new ArrayList<>();

    private boolean showBmiOnLineChart = false;
    private boolean showCaloriesOnBarChart = true;
    private int barChartDays = 7;
    private int currentStreakDays = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        weightAdapter = new WeightHistoryAdapter();
        binding.rvWeightHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvWeightHistory.setAdapter(weightAdapter);
        setupSwipeToDelete();

        viewModel.getProfile().observe(this, profile -> {
            currentProfile = profile;
            if (profile != null) {
                weightAdapter.setUserMetrics(profile.heightCm, profile.age);
            }
            renderWeightCardAndChart();
        });

        viewModel.getWeightsAsc().observe(this, list -> {
            weightsAsc = list != null ? list : new ArrayList<>();
            renderWeightCardAndChart();
        });

        viewModel.getWeightsDesc().observe(this, list -> {
            List<WeightHistory> desc = list != null ? list : new ArrayList<>();
            weightAdapter.submitList(desc);
            binding.tvEmptyWeights.setVisibility(desc.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getMonthFoodLogs().observe(this, logs -> {
            monthFoods = logs != null ? logs : new ArrayList<>();
            renderDailyBarChart();
        });

        viewModel.getMonthActivityLogs().observe(this, logs -> {
            monthActivities = logs != null ? logs : new ArrayList<>();
            renderDailyBarChart();
            renderStreakAndCompletion();
        });

        viewModel.getWorkoutSessions().observe(this, sessions -> {
            allSessions = sessions != null ? sessions : new ArrayList<>();
            renderDailyBarChart();
            renderStreakAndCompletion();
        });

        viewModel.getPlanDaySummaries().observe(this, summaries -> {
            planSummaries = summaries != null ? summaries : new ArrayList<>();
            renderStreakAndCompletion();
        });

        binding.btnModeWeight.setOnClickListener(v -> {
            showBmiOnLineChart = false;
            renderWeightCardAndChart();
        });
        binding.btnModeBmi.setOnClickListener(v -> {
            showBmiOnLineChart = true;
            renderWeightCardAndChart();
        });

        binding.btnMetricCalories.setOnClickListener(v -> {
            showCaloriesOnBarChart = true;
            renderDailyBarChart();
        });
        binding.btnMetricMinutes.setOnClickListener(v -> {
            showCaloriesOnBarChart = false;
            renderDailyBarChart();
        });
        binding.btnRange7d.setOnClickListener(v -> {
            barChartDays = 7;
            renderDailyBarChart();
        });
        binding.btnRange30d.setOnClickListener(v -> {
            barChartDays = 30;
            renderDailyBarChart();
        });

        binding.btnLogWeight.setOnClickListener(v -> showLogWeightDialog());
        binding.btnShareProgress.setOnClickListener(v -> shareProgressImplicitIntent());
    }

    private void renderStreakAndCompletion() {
        // An "active day" is any day with a completed workout session or logged activity
        Set<Long> activeDayStarts = new HashSet<>();
        for (WorkoutSession s : allSessions) {
            if (s.completed) {
                activeDayStarts.add(DateUtil.dayStartOf(s.date));
            }
        }
        for (ActivityLog a : monthActivities) {
            if (a.durationMinutes > 0) {
                activeDayStarts.add(DateUtil.dayStartOf(a.date));
            }
        }

        long cursor = DateUtil.startOfToday();
        if (!activeDayStarts.contains(cursor)) {
            // If nothing logged yet today, check if streak is alive from yesterday
            cursor = DateUtil.addDays(cursor, -1);
        }
        int streak = 0;
        while (activeDayStarts.contains(cursor)) {
            streak++;
            cursor = DateUtil.addDays(cursor, -1);
        }
        currentStreakDays = streak;

        binding.tvStreakSummary.setText(String.format(Locale.getDefault(),
                "%d-day active streak • %d total workouts completed",
                streak, allSessions.size()));

        int totalPlanEx = 0;
        int donePlanEx = 0;
        for (DaySummary ds : planSummaries) {
            totalPlanEx += ds.exerciseCount;
            donePlanEx += ds.completedCount;
        }
        if (totalPlanEx > 0) {
            int pct = (int) Math.round((donePlanEx * 100.0) / totalPlanEx);
            binding.tvPlanCompletion.setText(String.format(Locale.getDefault(),
                    "This week's plan: %d / %d exercises done (%d%%)",
                    donePlanEx, totalPlanEx, pct));
        } else {
            binding.tvPlanCompletion.setText("No active workout plan this week.");
        }
    }

    private void renderWeightCardAndChart() {
        double heightCm = (currentProfile != null && currentProfile.heightCm > 0)
                ? currentProfile.heightCm : 170;
        int age = (currentProfile != null && currentProfile.age > 0)
                ? currentProfile.age : 22;

        double latestWeight = (currentProfile != null) ? currentProfile.currentWeightKg : 0;
        if (!weightsAsc.isEmpty()) {
            latestWeight = weightsAsc.get(weightsAsc.size() - 1).weightKg;
        }

        if (latestWeight > 0) {
            double bmi = BMIUtil.bmi(latestWeight, heightCm);
            String targetPart = (currentProfile != null && currentProfile.targetWeightKg > 0)
                    ? String.format(Locale.getDefault(), "Target: %.1f kg • ", currentProfile.targetWeightKg)
                    : "";
            binding.tvWeightOverview.setText(String.format(Locale.getDefault(),
                    "Current: %.1f kg • %sBMI %.1f (%s)",
                    latestWeight, targetPart, bmi, BMIUtil.category(bmi, age)));
        } else {
            binding.tvWeightOverview.setText("Log your weight below to track your weight and BMI trend.");
        }

        List<Float> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM", Locale.getDefault());

        for (WeightHistory w : weightsAsc) {
            if (showBmiOnLineChart) {
                values.add((float) BMIUtil.bmi(w.weightKg, heightCm));
            } else {
                values.add((float) w.weightKg);
            }
            labels.add(fmt.format(new Date(w.date)));
        }

        binding.chartWeight.setData(
                TrendChartView.MODE_LINE,
                values,
                labels,
                "No weight entries yet");
    }

    private void renderDailyBarChart() {
        String metricTitle = showCaloriesOnBarChart ? "Daily calories eaten" : "Daily active minutes";
        binding.tvTrendChartTitle.setText(String.format(Locale.getDefault(),
                "%s (last %d days)", metricTitle, barChartDays));

        long today = DateUtil.startOfToday();
        List<Float> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat(barChartDays == 7 ? "EEE" : "d/M", Locale.getDefault());

        for (int i = barChartDays - 1; i >= 0; i--) {
            long dStart = DateUtil.addDays(today, -i);
            long dEnd = DateUtil.addDays(dStart, 1);

            if (showCaloriesOnBarChart) {
                double kcal = 0;
                for (FoodLogItem f : monthFoods) {
                    if (f.log.date >= dStart && f.log.date < dEnd) {
                        kcal += f.log.totalCalories;
                    }
                }
                values.add((float) kcal);
            } else {
                int mins = 0;
                for (ActivityLog a : monthActivities) {
                    if (a.date >= dStart && a.date < dEnd) {
                        mins += a.durationMinutes;
                    }
                }
                for (WorkoutSession s : allSessions) {
                    if (s.date >= dStart && s.date < dEnd) {
                        mins += s.durationMinutes;
                    }
                }
                values.add((float) mins);
            }
            labels.add(fmt.format(new Date(dStart)));
        }

        binding.chartDailyTrend.setData(
                TrendChartView.MODE_BAR,
                values,
                labels,
                "No entries in this period");
    }

    private void showLogWeightDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Weight in kg (30 to 300)");
        if (currentProfile != null && currentProfile.currentWeightKg > 0) {
            input.setText(String.format(Locale.getDefault(), "%.1f", currentProfile.currentWeightKg));
            input.setSelection(input.getText().length());
        }

        FrameLayout container = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Log today's weight")
                .setView(container)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(di ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String s = input.getText().toString().trim();
                    double kg;
                    try {
                        kg = Double.parseDouble(s);
                    } catch (NumberFormatException e) {
                        kg = Double.NaN;
                    }
                    if (Double.isNaN(kg) || kg < 30 || kg > 300) {
                        input.setError("Enter a weight between 30 and 300 kg");
                        return;
                    }
                    dialog.dismiss();
                    viewModel.addWeight(kg, () ->
                            Toast.makeText(this, "Weight saved", Toast.LENGTH_SHORT).show());
                }));
        dialog.show();
    }

    /**
     * Day 7 Syllabus Topic: Implicit Intent (ACTION_SEND) to share a fitness progress summary.
     */
    private void shareProgressImplicitIntent() {
        String name = (currentProfile != null && currentProfile.name != null)
                ? currentProfile.name : "Athlete";
        String goal = (currentProfile != null && currentProfile.fitnessGoal != null)
                ? currentProfile.fitnessGoal : "General Fitness";
        double weight = (currentProfile != null) ? currentProfile.currentWeightKg : 0;
        double height = (currentProfile != null && currentProfile.heightCm > 0)
                ? currentProfile.heightCm : 170;
        double bmi = weight > 0 ? BMIUtil.bmi(weight, height) : 0;

        String text = String.format(Locale.getDefault(),
                "FitTrackerPro Progress Summary (%s)\n"
                        + "• Goal: %s\n"
                        + "• Current weight: %.1f kg (BMI %.1f)\n"
                        + "• Active streak: %d day(s)\n"
                        + "• Completed workouts: %d",
                name, goal, weight, bmi, currentStreakDays, allSessions.size());

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "My FitTrackerPro Progress");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Share progress via"));
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                WeightHistory removed = weightAdapter.getItemAt(viewHolder.getBindingAdapterPosition());
                viewModel.deleteWeight(removed);
                Snackbar.make(binding.getRoot(), "Weight entry deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> viewModel.restoreWeight(removed))
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvWeightHistory);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
