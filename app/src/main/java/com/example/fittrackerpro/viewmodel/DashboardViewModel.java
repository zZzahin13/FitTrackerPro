package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.database.entity.FoodLogItem;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WorkoutPlan;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.repository.ActivityRepository;
import com.example.fittrackerpro.repository.NutritionRepository;
import com.example.fittrackerpro.repository.SessionRepository;
import com.example.fittrackerpro.repository.UserRepository;
import com.example.fittrackerpro.repository.WorkoutRepository;
import com.example.fittrackerpro.utils.DateUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Day 7 ViewModel for the Home Dashboard.
 * Combines real stored data from Room: Profile, today's FoodLogs, Water,
 * 7-day ActivityLogs, WorkoutSessions, and the active WorkoutPlan's weekday summaries.
 */
public class DashboardViewModel extends AndroidViewModel {

    private final NutritionRepository nutritionRepository;
    private final String userId;

    private final LiveData<UserProfile> profile;
    private final LiveData<List<FoodLogItem>> todayFoodLogs;
    private final LiveData<Integer> todayWaterMl;
    private final LiveData<List<ActivityLog>> weekActivityLogs;
    private final LiveData<List<WorkoutSession>> workoutSessions;
    private final LiveData<WorkoutPlan> activePlan;
    private final LiveData<List<DaySummary>> daySummaries;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        UserRepository userRepository = new UserRepository(application);
        nutritionRepository = new NutritionRepository(application);
        ActivityRepository activityRepository = new ActivityRepository(application);
        SessionRepository sessionRepository = new SessionRepository(application);
        WorkoutRepository workoutRepository = new WorkoutRepository(application);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        long today = DateUtil.startOfToday();
        long tomorrow = DateUtil.addDays(today, 1);
        long sevenDaysAgo = DateUtil.addDays(today, -6);

        profile = userRepository.observeProfile(userId);
        todayFoodLogs = nutritionRepository.observeLogs(userId, today, tomorrow);
        todayWaterMl = nutritionRepository.observeWaterTotal(userId, today, tomorrow);
        weekActivityLogs = activityRepository.observeRange(userId, sevenDaysAgo, tomorrow);
        workoutSessions = sessionRepository.observeSessions(userId);
        activePlan = workoutRepository.observeActivePlan(userId);

        daySummaries = Transformations.switchMap(activePlan, plan -> {
            if (plan == null) {
                MutableLiveData<List<DaySummary>> empty = new MutableLiveData<>();
                empty.setValue(new ArrayList<>());
                return empty;
            }
            return workoutRepository.observeDaySummaries(plan.planId, DateUtil.startOfWeek());
        });
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<List<FoodLogItem>> getTodayFoodLogs() {
        return todayFoodLogs;
    }

    public LiveData<Integer> getTodayWaterMl() {
        return todayWaterMl;
    }

    public LiveData<List<ActivityLog>> getWeekActivityLogs() {
        return weekActivityLogs;
    }

    public LiveData<List<WorkoutSession>> getWorkoutSessions() {
        return workoutSessions;
    }

    public LiveData<WorkoutPlan> getActivePlan() {
        return activePlan;
    }

    public LiveData<List<DaySummary>> getDaySummaries() {
        return daySummaries;
    }

    public void addWaterToday(int amountMl) {
        nutritionRepository.addWater(userId, amountMl, DateUtil.startOfToday());
    }
}
