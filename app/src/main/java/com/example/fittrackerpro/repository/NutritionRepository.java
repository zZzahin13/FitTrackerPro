package com.example.fittrackerpro.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.FoodSeeder;
import com.example.fittrackerpro.database.dao.FoodLogDao;
import com.example.fittrackerpro.database.dao.MealDao;
import com.example.fittrackerpro.database.dao.WaterDao;
import com.example.fittrackerpro.database.entity.FoodLog;
import com.example.fittrackerpro.database.entity.FoodLogItem;
import com.example.fittrackerpro.database.entity.Meal;
import com.example.fittrackerpro.database.entity.WaterLog;
import com.example.fittrackerpro.utils.DateUtil;

import java.util.List;

public class NutritionRepository {

    private final MealDao mealDao;
    private final FoodLogDao logDao;
    private final WaterDao waterDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NutritionRepository(Context context) {
        FitTrackerDatabase db = FitTrackerDatabase.getInstance(context);
        mealDao = db.mealDao();
        logDao = db.foodLogDao();
        waterDao = db.waterDao();
    }

    public void ensureSeeded() {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            if (mealDao.count() == 0) {
                mealDao.insertAll(FoodSeeder.createFoods());
            }
        });
    }

    public LiveData<List<Meal>> searchFoods(String query) {
        return mealDao.search(query);
    }

    public void addCustomFood(Meal meal, Runnable onDone) {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            mealDao.insert(meal);
            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }

    public LiveData<List<FoodLogItem>> observeLogs(String userId, long rangeStart, long rangeEnd) {
        return logDao.observeRange(userId, rangeStart, rangeEnd);
    }

    public void addLog(String userId, Meal food, double quantity, String mealType, long dayStart) {
        FoodLog log = new FoodLog();
        log.userId = userId;
        log.mealId = food.mealId;
        log.quantity = quantity;
        log.date = DateUtil.logTimeFor(dayStart);
        log.mealType = mealType;
        log.totalCalories = quantity * food.calories;   // saved, so old days never change
        log.isSynced = false;
        log.updatedAt = System.currentTimeMillis();
        FitTrackerDatabase.dbExecutor.execute(() -> logDao.insert(log));
    }

    public void updateQuantity(FoodLogItem item, double quantity) {
        double total = quantity * item.kcalPerServing;
        FitTrackerDatabase.dbExecutor.execute(() ->
                logDao.updateQuantity(item.log.logId, quantity, total, System.currentTimeMillis()));
    }

    public void deleteLog(FoodLog log) {
        FitTrackerDatabase.dbExecutor.execute(() -> logDao.delete(log));
    }

    public void restoreLog(FoodLog log) {
        FitTrackerDatabase.dbExecutor.execute(() -> logDao.insert(log));
    }

    // ---------- Water ----------

    public LiveData<Integer> observeWaterTotal(String userId, long rangeStart, long rangeEnd) {
        return waterDao.observeTotal(userId, rangeStart, rangeEnd);
    }

    public void addWater(String userId, int amountMl, long dayStart) {
        WaterLog w = new WaterLog();
        w.userId = userId;
        w.amountMl = amountMl;
        w.date = DateUtil.logTimeFor(dayStart);
        w.isSynced = false;
        w.updatedAt = System.currentTimeMillis();
        FitTrackerDatabase.dbExecutor.execute(() -> waterDao.insert(w));
    }

    public void undoLastWater(String userId, long dayStart) {
        FitTrackerDatabase.dbExecutor.execute(() ->
                waterDao.deleteLatest(userId, dayStart, DateUtil.addDays(dayStart, 1)));
    }
}