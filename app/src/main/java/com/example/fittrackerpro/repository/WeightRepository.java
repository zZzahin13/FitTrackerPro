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
import com.example.fittrackerpro.utils.SyncManager;

import java.util.List;

/**
 * Day 7 Repository for WeightHistory entries.
 * Logging a new weight also updates UserProfile.currentWeightKg and triggers background Cloud Sync.
 */
public class WeightRepository {

    private final Context appContext;
    private final WeightHistoryDao weightDao;
    private final UserProfileDao profileDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WeightRepository(Context context) {
        appContext = context.getApplicationContext();
        FitTrackerDatabase db = FitTrackerDatabase.getInstance(appContext);
        weightDao = db.weightHistoryDao();
        profileDao = db.userProfileDao();
    }

    public LiveData<List<WeightHistory>> observeAllAsc(String userId) {
        return weightDao.observeAllAsc(userId);
    }

    public LiveData<List<WeightHistory>> observeAllDesc(String userId) {
        return weightDao.observeAllDesc(userId);
    }

    public void addWeight(String userId, double weightKg, long dateMillis, Runnable onDone) {
        long now = System.currentTimeMillis();
        WeightHistory record = new WeightHistory();
        record.userId = userId;
        record.weightKg = weightKg;
        record.date = dateMillis;
        record.isSynced = false;
        record.updatedAt = now;

        FitTrackerDatabase.dbExecutor.execute(() -> {
            weightDao.insert(record);

            UserProfile p = profileDao.getProfileOnce(userId);
            if (p != null) {
                p.currentWeightKg = weightKg;
                p.isSynced = false;
                p.updatedAt = now;
                profileDao.upsert(p);
            }

            SyncManager.triggerSyncNow(appContext);

            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }

    public void delete(WeightHistory record) {
        FitTrackerDatabase.dbExecutor.execute(() -> weightDao.delete(record));
    }

    public void restore(WeightHistory record) {
        FitTrackerDatabase.dbExecutor.execute(() -> weightDao.insert(record));
    }
}
