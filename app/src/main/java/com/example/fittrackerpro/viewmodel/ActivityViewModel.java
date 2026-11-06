package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.repository.ActivityRepository;
import com.example.fittrackerpro.repository.UserRepository;
import com.example.fittrackerpro.utils.CalorieCalculator;
import com.example.fittrackerpro.utils.DateUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ActivityViewModel extends AndroidViewModel {

    private final ActivityRepository repository;
    private final UserRepository userRepository;
    private final String userId;
    private final LiveData<List<ActivityLog>> logs;
    private final LiveData<UserProfile> profile;

    public ActivityViewModel(@NonNull Application application) {
        super(application);
        repository = new ActivityRepository(application);
        userRepository = new UserRepository(application);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        // The last 14 days, including today
        long today = DateUtil.startOfToday();
        logs = repository.observeRange(userId, DateUtil.addDays(today, -13), DateUtil.addDays(today, 1));
        profile = userRepository.observeProfile(userId);
    }

    public LiveData<List<ActivityLog>> getLogs() {
        return logs;
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    // Calories and (when steps is 0) steps are estimated from the profile
    public void addLog(String type, long dateMillis, int minutes, double km, int steps,
                       Runnable onDone) {
        userRepository.getProfileOnce(userId, p -> {
            double weight = p != null ? p.currentWeightKg : 70;
            double height = p != null ? p.heightCm : 170;

            ActivityLog log = new ActivityLog();
            log.userId = userId;
            log.activityType = type;
            log.durationMinutes = minutes;
            log.distanceKm = km;
            log.steps = steps > 0 ? steps : CalorieCalculator.estimateSteps(type, km, height);
            log.caloriesBurned = CalorieCalculator.activityCalories(type, weight, minutes);
            log.date = dateMillis;
            log.isSynced = false;
            log.updatedAt = System.currentTimeMillis();
            repository.insert(log, onDone);
        });
    }

    public void delete(ActivityLog log) {
        repository.delete(log);
    }

    public void restore(ActivityLog log) {
        repository.restore(log);
    }
}