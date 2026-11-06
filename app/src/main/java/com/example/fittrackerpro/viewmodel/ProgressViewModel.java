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
import com.example.fittrackerpro.database.entity.WeightHistory;
import com.example.fittrackerpro.database.entity.WorkoutPlan;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.repository.ActivityRepository;
import com.example.fittrackerpro.repository.NutritionRepository;
import com.example.fittrackerpro.repository.SessionRepository;
import com.example.fittrackerpro.repository.UserRepository;
import com.example.fittrackerpro.repository.WeightRepository;
import com.example.fittrackerpro.repository.WorkoutRepository;
import com.example.fittrackerpro.utils.DateUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Day 7 ViewModel for ProgressActivity (Weight & BMI history, 7-day / 30-day charts,
 * workout completion & streak, and implicit Intent progress summary).
 */
public class ProgressViewModel extends AndroidViewModel {

    private final WeightRepository weightRepository;
    private final String userId;

    private final LiveData<UserProfile> profile;
    private final LiveData<List<WeightHistory>> weightsAsc;
    private final LiveData<List<WeightHistory>> weightsDesc;
    private final LiveData<List<FoodLogItem>> monthFoodLogs;
    private final LiveData<List<ActivityLog>> monthActivityLogs;
    private final LiveData<List<WorkoutSession>> workoutSessions;
    private final LiveData<List<DaySummary>> planDaySummaries;

    public ProgressViewModel(@NonNull Application application) {
        super(application);
        UserRepository userRepository = new UserRepository(application);
        weightRepository = new WeightRepository(application);
        NutritionRepository nutritionRepository = new NutritionRepository(application);
        ActivityRepository activityRepository = new ActivityRepository(application);
        SessionRepository sessionRepository = new SessionRepository(application);
        WorkoutRepository workoutRepository = new WorkoutRepository(application);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        long today = DateUtil.startOfToday();
        long tomorrow = DateUtil.addDays(today, 1);
        long thirtyDaysAgo = DateUtil.addDays(today, -29);

        profile = userRepository.observeProfile(userId);
        weightsAsc = weightRepository.observeAllAsc(userId);
        weightsDesc = weightRepository.observeAllDesc(userId);
        monthFoodLogs = nutritionRepository.observeLogs(userId, thirtyDaysAgo, tomorrow);
        monthActivityLogs = activityRepository.observeRange(userId, thirtyDaysAgo, tomorrow);
        workoutSessions = sessionRepository.observeSessions(userId);

        LiveData<WorkoutPlan> activePlan = workoutRepository.observeActivePlan(userId);
        planDaySummaries = Transformations.switchMap(activePlan, plan -> {
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

    public LiveData<List<WeightHistory>> getWeightsAsc() {
        return weightsAsc;
    }

    public LiveData<List<WeightHistory>> getWeightsDesc() {
        return weightsDesc;
    }

    public LiveData<List<FoodLogItem>> getMonthFoodLogs() {
        return monthFoodLogs;
    }

    public LiveData<List<ActivityLog>> getMonthActivityLogs() {
        return monthActivityLogs;
    }

    public LiveData<List<WorkoutSession>> getWorkoutSessions() {
        return workoutSessions;
    }

    public LiveData<List<DaySummary>> getPlanDaySummaries() {
        return planDaySummaries;
    }

    public void addWeight(double weightKg, Runnable onDone) {
        weightRepository.addWeight(userId, weightKg, System.currentTimeMillis(), onDone);
    }

    public void deleteWeight(WeightHistory record) {
        weightRepository.delete(record);
    }

    public void restoreWeight(WeightHistory record) {
        weightRepository.restore(record);
    }
}
