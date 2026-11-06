package com.example.fittrackerpro.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.ActivityLogDao;
import com.example.fittrackerpro.database.entity.ActivityLog;

import java.util.List;

public class ActivityRepository {

    private final ActivityLogDao dao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ActivityRepository(Context context) {
        dao = FitTrackerDatabase.getInstance(context).activityLogDao();
    }

    public LiveData<List<ActivityLog>> observeRange(String userId, long rangeStart, long rangeEnd) {
        return dao.observeRange(userId, rangeStart, rangeEnd);
    }

    public void insert(ActivityLog log, Runnable onDone) {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            dao.insert(log);
            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }

    public void delete(ActivityLog log) {
        FitTrackerDatabase.dbExecutor.execute(() -> dao.delete(log));
    }

    public void restore(ActivityLog log) {
        FitTrackerDatabase.dbExecutor.execute(() -> dao.insert(log));
    }
}