package com.example.fittrackerpro.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.activities.AssessmentActivity;
import com.example.fittrackerpro.activities.ProgressActivity;
import com.example.fittrackerpro.activities.TrackingActivity;
import com.example.fittrackerpro.activities.WorkoutDayActivity;
import com.example.fittrackerpro.customviews.TrendChartView;
import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.database.entity.FoodLogItem;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WorkoutPlan;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.databinding.FragmentHomeBinding;
import com.example.fittrackerpro.utils.CalorieCalculator;
import com.example.fittrackerpro.utils.DateUtil;
import com.example.fittrackerpro.utils.ProfilePhotoHelper;
import com.example.fittrackerpro.utils.TipsXmlParser;
import com.example.fittrackerpro.viewmodel.DashboardViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Day 7 & Day 8A: Modern Home Dashboard with 3 multi-color rings (Calories, Steps, Active Time),
 * Weekly Goal progress bar, Today's Schedule card, 7-day gradient bar chart, and XML Daily Tips.
 */
public class HomeFragment extends Fragment {

    private static final int DAILY_WATER_GOAL_ML = 2000;
    private static final int DAILY_STEPS_GOAL = 8000;
    private static final int DAILY_ACTIVE_MIN_GOAL = 45;

    private FragmentHomeBinding binding;
    private DashboardViewModel viewModel;

    private UserProfile currentProfile;
    private List<FoodLogItem> todayFoods = new ArrayList<>();
    private List<ActivityLog> weekActivities = new ArrayList<>();
    private List<WorkoutSession> allSessions = new ArrayList<>();
    private WorkoutPlan currentPlan;
    private List<DaySummary> currentDaySummaries = new ArrayList<>();

    private List<TipsXmlParser.Tip> xmlTips = new ArrayList<>();
    private int currentTipIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Set the 3 distinct ring colors matching the UI reference (Orange, Cyan, Emerald)
        binding.ringCalories.setRingColor(Color.parseColor("#F97316"));
        binding.ringSteps.setRingColor(Color.parseColor("#38BDF8"));
        binding.ringActiveTime.setRingColor(Color.parseColor("#34D399"));

        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            currentProfile = profile;
            renderHeaderAndCalories();
        });

        viewModel.getTodayFoodLogs().observe(getViewLifecycleOwner(), logs -> {
            todayFoods = logs != null ? logs : new ArrayList<>();
            renderHeaderAndCalories();
        });

        viewModel.getTodayWaterMl().observe(getViewLifecycleOwner(), ml -> {
            int water = ml != null ? ml : 0;
            binding.tvWaterToday.setText(String.format(Locale.getDefault(),
                    "Hydration: %d / %d ml", water, DAILY_WATER_GOAL_ML));
        });

        viewModel.getWeekActivityLogs().observe(getViewLifecycleOwner(), logs -> {
            weekActivities = logs != null ? logs : new ArrayList<>();
            renderHeaderAndCalories();
            renderMovementAndWeeklyChart();
        });

        viewModel.getWorkoutSessions().observe(getViewLifecycleOwner(), sessions -> {
            allSessions = sessions != null ? sessions : new ArrayList<>();
            renderHeaderAndCalories();
            renderMovementAndWeeklyChart();
        });

        viewModel.getActivePlan().observe(getViewLifecycleOwner(), plan -> {
            currentPlan = plan;
            renderTodayWorkoutCard();
        });

        viewModel.getDaySummaries().observe(getViewLifecycleOwner(), summaries -> {
            currentDaySummaries = summaries != null ? summaries : new ArrayList<>();
            renderTodayWorkoutCard();
        });

        binding.btnFinishAssessment.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AssessmentActivity.class)));

        binding.btnQuickWater.setOnClickListener(v -> viewModel.addWaterToday(250));

        binding.btnQuickTimer.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TrackingActivity.class)));

        binding.btnOpenProgress.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProgressActivity.class)));

        binding.cardWeeklyGoal.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProgressActivity.class)));

        binding.cardAvatarShortcut.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProgressActivity.class)));

        // Day 8A: Parse bundled XML tips using XmlPullParser and display on Home
        xmlTips = TipsXmlParser.loadTips(requireContext());
        if (!xmlTips.isEmpty()) {
            int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            currentTipIndex = dayOfYear % xmlTips.size();
            renderCurrentXmlTip();
        }
        binding.btnNextTip.setOnClickListener(v -> {
            if (!xmlTips.isEmpty()) {
                currentTipIndex = (currentTipIndex + 1) % xmlTips.size();
                renderCurrentXmlTip();
            }
        });

        return binding.getRoot();
    }

    private String getTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning,";
        if (hour < 17) return "Good Afternoon,";
        return "Good Evening,";
    }

    private void renderCurrentXmlTip() {
        if (xmlTips.isEmpty() || binding == null) {
            return;
        }
        TipsXmlParser.Tip tip = xmlTips.get(currentTipIndex);
        binding.tvTipCategory.setText("Daily Tip • " + tip.category + " (XML)");
        binding.tvTipTitle.setText(tip.title);
        binding.tvTipBody.setText(tip.body);
    }

    private void renderHeaderAndCalories() {
        double targetKcal = 2000;
        String greetingPrefix = getTimeGreeting();
        if (currentProfile != null && currentProfile.profileCompleted) {
            binding.cardIncompleteProfile.setVisibility(View.GONE);
            String name = currentProfile.name != null && !currentProfile.name.isEmpty()
                    ? currentProfile.name : "Athlete";
            binding.tvGreeting.setText(greetingPrefix + "\n" + name);
            binding.tvGoalLine.setText(currentProfile.fitnessGoal
                    + " • " + String.format(Locale.getDefault(), "%.1f kg", currentProfile.currentWeightKg));

            CalorieCalculator.Targets t = CalorieCalculator.calculate(currentProfile);
            targetKcal = t.targetCalories;
        } else {
            binding.cardIncompleteProfile.setVisibility(View.VISIBLE);
            binding.tvGreeting.setText(greetingPrefix + "\nAthlete");
            binding.tvGoalLine.setText("Daily target: 2,000 kcal");
        }

        double eatenKcal = 0;
        for (FoodLogItem item : todayFoods) {
            eatenKcal += item.log.totalCalories;
        }

        long today = DateUtil.startOfToday();
        double burnedKcal = 0;
        for (ActivityLog a : weekActivities) {
            if (a.date >= today) {
                burnedKcal += a.caloriesBurned;
            }
        }
        for (WorkoutSession s : allSessions) {
            if (s.date >= today) {
                burnedKcal += s.caloriesBurned;
            }
        }

        long remaining = Math.round(targetKcal - eatenKcal);
        binding.ringCalories.setProgress(
                (float) eatenKcal,
                (float) targetKcal,
                String.valueOf(Math.round(eatenKcal)),
                "kcal");

        binding.tvCalorieEaten.setText(String.format(Locale.getDefault(),
                "%d kcal", Math.round(eatenKcal)));
        binding.tvCalorieTarget.setText(String.format(Locale.getDefault(),
                "Goal: %d kcal", Math.round(targetKcal)));
        binding.tvCalorieBurned.setText(String.format(Locale.getDefault(),
                "Burned: %d kcal", Math.round(burnedKcal)));

        if (remaining >= 0) {
            binding.tvCalorieRemaining.setText(String.format(Locale.getDefault(),
                    "%d left", remaining));
        } else {
            binding.tvCalorieRemaining.setText(String.format(Locale.getDefault(),
                    "%d over", Math.abs(remaining)));
        }
    }

    private void renderMovementAndWeeklyChart() {
        long today = DateUtil.startOfToday();
        int todayMins = 0;
        int todaySteps = 0;
        double todayKm = 0;

        for (ActivityLog a : weekActivities) {
            if (a.date >= today) {
                todayMins += a.durationMinutes;
                todaySteps += a.steps;
                todayKm += a.distanceKm;
            }
        }
        for (WorkoutSession s : allSessions) {
            if (s.date >= today) {
                todayMins += s.durationMinutes;
            }
        }

        // Update Ring 2 (Steps) & Ring 3 (Active Time)
        binding.ringSteps.setProgress(
                todaySteps,
                DAILY_STEPS_GOAL,
                String.valueOf(todaySteps),
                "steps");
        binding.tvStepsRingValue.setText(String.format(Locale.getDefault(), "%d Steps", todaySteps));

        binding.ringActiveTime.setProgress(
                todayMins,
                DAILY_ACTIVE_MIN_GOAL,
                String.valueOf(todayMins),
                "min");
        binding.tvActiveRingValue.setText(String.format(Locale.getDefault(), "%d min", todayMins));

        binding.tvMovementStats.setText(String.format(Locale.getDefault(),
                "Today: %d min active • %.1f km • %d steps",
                todayMins, todayKm, todaySteps));

        // Build 7-day bar chart of active minutes
        List<Float> dailyMins = new ArrayList<>();
        List<String> dayLabels = new ArrayList<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());
        int activeDaysCount = 0;

        for (int i = 6; i >= 0; i--) {
            long dStart = DateUtil.addDays(today, -i);
            long dEnd = DateUtil.addDays(dStart, 1);
            int mins = 0;
            for (ActivityLog a : weekActivities) {
                if (a.date >= dStart && a.date < dEnd) {
                    mins += a.durationMinutes;
                }
            }
            for (WorkoutSession s : allSessions) {
                if (s.date >= dStart && s.date < dEnd) {
                    mins += s.durationMinutes;
                }
            }
            if (mins > 0) activeDaysCount++;
            dailyMins.add((float) mins);
            dayLabels.add(dayFmt.format(new Date(dStart)));
        }

        long weekStart = DateUtil.startOfWeek();
        int workoutsThisWeek = 0;
        for (WorkoutSession s : allSessions) {
            if (s.date >= weekStart) {
                workoutsThisWeek++;
            }
        }

        int weeklyPct = Math.min(100, Math.round((activeDaysCount * 100f) / 7f));
        binding.tvWeeklyGoalPercent.setText(weeklyPct + "%  ›");
        binding.progressWeeklyGoal.setMax(100);
        binding.progressWeeklyGoal.setProgressCompat(weeklyPct, true);

        binding.tvWeeklySummary.setText(String.format(Locale.getDefault(),
                "%d/7 active days • %d workout(s) completed this week",
                activeDaysCount, workoutsThisWeek));
        binding.chartWeeklyActive.setData(
                TrendChartView.MODE_BAR, dailyMins, dayLabels, "No activity in the last 7 days");
    }

    private void renderTodayWorkoutCard() {
        int dow = DateUtil.todayDayOfWeek();
        String todayName = DateUtil.dayName(dow);

        if (currentPlan == null) {
            binding.tvTodayWorkoutTitle.setText(todayName + " • No Active Plan");
            binding.tvTodayWorkoutSubtitle.setText(
                    "Generate a personalized 7-day schedule in the Workout tab.");
            binding.btnOpenTodayWorkout.setVisibility(View.GONE);
            return;
        }

        DaySummary todaySummary = null;
        for (DaySummary s : currentDaySummaries) {
            if (s.dayOfWeek == dow) {
                todaySummary = s;
                break;
            }
        }

        if (todaySummary == null || todaySummary.exerciseCount == 0) {
            binding.tvTodayWorkoutTitle.setText(todayName + " • Active Recovery");
            binding.tvTodayWorkoutSubtitle.setText(
                    "Rest day on " + currentPlan.planName + ". Light walking or mobility keeps recovery on track.");
            binding.btnOpenTodayWorkout.setVisibility(View.GONE);
        } else {
            binding.tvTodayWorkoutTitle.setText(todaySummary.muscles + "  •  " + todayName);
            binding.tvTodayWorkoutSubtitle.setText(String.format(Locale.getDefault(),
                    "%d exercises scheduled (%d/%d completed this week)",
                    todaySummary.exerciseCount,
                    todaySummary.completedCount,
                    todaySummary.exerciseCount));
            binding.btnOpenTodayWorkout.setVisibility(View.VISIBLE);
            binding.btnOpenTodayWorkout.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), WorkoutDayActivity.class);
                i.putExtra(WorkoutDayActivity.EXTRA_PLAN_ID, currentPlan.planId);
                i.putExtra(WorkoutDayActivity.EXTRA_DAY, dow);
                startActivity(i);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && getContext() != null) {
            Bitmap bmp = ProfilePhotoHelper.loadAvatarBitmap(requireContext());
            if (bmp != null) {
                binding.ivHomeAvatar.setPadding(0, 0, 0, 0);
                ImageViewCompat.setImageTintList(binding.ivHomeAvatar, null);
                binding.ivHomeAvatar.setImageBitmap(bmp);
            } else {
                int pad = (int) (12 * getResources().getDisplayMetrics().density);
                binding.ivHomeAvatar.setPadding(pad, pad, pad, pad);
                binding.ivHomeAvatar.setImageResource(R.drawable.ic_nav_profile);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
