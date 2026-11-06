package com.example.fittrackerpro.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.UserProfileDao;
import com.example.fittrackerpro.database.dao.WeightHistoryDao;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WeightHistory;

import java.util.function.Consumer;

public class UserRepository {

    private final UserProfileDao dao;
    private final WeightHistoryDao weightDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public UserRepository(Context context) {
        FitTrackerDatabase db = FitTrackerDatabase.getInstance(context);
        dao = db.userProfileDao();
        weightDao = db.weightHistoryDao();
    }

    public LiveData<UserProfile> observeProfile(String userId) {
        return dao.observeProfile(userId);
    }

    // Reads on a background thread, then delivers the result on the main thread
    public void getProfileOnce(String userId, Consumer<UserProfile> callback) {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            UserProfile profile = dao.getProfileOnce(userId);
            mainHandler.post(() -> callback.accept(profile));
        });
    }

    public void saveProfile(UserProfile profile, Runnable onSaved) {
        long now = System.currentTimeMillis();
        profile.updatedAt = now;
        profile.isSynced = false;   // the Day 8 sync will upload it
        FitTrackerDatabase.dbExecutor.execute(() -> {
            dao.upsert(profile);

            // Seed initial weight history entry if the user has none yet
            if (profile.currentWeightKg > 0 && weightDao.countForUser(profile.userId) == 0) {
                WeightHistory w = new WeightHistory();
                w.userId = profile.userId;
                w.weightKg = profile.currentWeightKg;
                w.date = now;
                w.isSynced = false;
                w.updatedAt = now;
                weightDao.insert(w);
            }

            if (onSaved != null) {
                mainHandler.post(onSaved);
            }
        });
    }
}
