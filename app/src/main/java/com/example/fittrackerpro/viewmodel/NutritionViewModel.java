package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.fittrackerpro.database.entity.FoodLog;
import com.example.fittrackerpro.database.entity.FoodLogItem;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.repository.NutritionRepository;
import com.example.fittrackerpro.repository.UserRepository;
import com.example.fittrackerpro.utils.DateUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class NutritionViewModel extends AndroidViewModel {

    private final NutritionRepository repository;
    private final String userId;
    private final MutableLiveData<Long> selectedDay = new MutableLiveData<>();
    private final LiveData<List<FoodLogItem>> logs;
    private final LiveData<Integer> waterTotal;
    private final LiveData<UserProfile> profile;

    // The "last 7 days" window: 6 days ago up to the end of today
    private final long weekStart;
    private final LiveData<List<FoodLogItem>> weekLogs;
    private final LiveData<Integer> weekWater;

    public NutritionViewModel(@NonNull Application application) {
        super(application);
        repository = new NutritionRepository(application);
        UserRepository userRepository = new UserRepository(application);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        repository.ensureSeeded();
        long today = DateUtil.startOfToday();
        selectedDay.setValue(today);

        // Whenever the selected day changes, load that day's diary and water
        logs = Transformations.switchMap(selectedDay,
                day -> repository.observeLogs(userId, day, DateUtil.addDays(day, 1)));
        waterTotal = Transformations.switchMap(selectedDay,
                day -> repository.observeWaterTotal(userId, day, DateUtil.addDays(day, 1)));
        profile = userRepository.observeProfile(userId);

        weekStart = DateUtil.addDays(today, -6);
        long weekEnd = DateUtil.addDays(today, 1);
        weekLogs = repository.observeLogs(userId, weekStart, weekEnd);
        weekWater = repository.observeWaterTotal(userId, weekStart, weekEnd);
    }

    public LiveData<Long> getSelectedDay() {
        return selectedDay;
    }

    public LiveData<List<FoodLogItem>> getLogs() {
        return logs;
    }

    public LiveData<Integer> getWaterTotal() {
        return waterTotal;
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<List<FoodLogItem>> getWeekLogs() {
        return weekLogs;
    }

    public LiveData<Integer> getWeekWater() {
        return weekWater;
    }

    public long getWeekStart() {
        return weekStart;
    }

    public long getSelectedDayValue() {
        Long day = selectedDay.getValue();
        return day != null ? day : DateUtil.startOfToday();
    }

    public void previousDay() {
        selectedDay.setValue(DateUtil.addDays(getSelectedDayValue(), -1));
    }

    public void nextDay() {
        if (getSelectedDayValue() < DateUtil.startOfToday()) {
            selectedDay.setValue(DateUtil.addDays(getSelectedDayValue(), 1));
        }
    }

    public void updateQuantity(FoodLogItem item, double quantity) {
        repository.updateQuantity(item, quantity);
    }

    public void delete(FoodLog log) {
        repository.deleteLog(log);
    }

    public void restore(FoodLog log) {
        repository.restoreLog(log);
    }

    public void addWater(int amountMl) {
        repository.addWater(userId, amountMl, getSelectedDayValue());
    }

    public void undoWater() {
        repository.undoLastWater(userId, getSelectedDayValue());
    }
}