package com.example.fittrackerpro.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.WorkoutSessionDao;
import com.example.fittrackerpro.database.entity.WorkoutSession;

import java.util.List;

public class SessionRepository {

    private final WorkoutSessionDao dao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SessionRepository(Context context) {
        dao = FitTrackerDatabase.getInstance(context).workoutSessionDao();
    }

    public LiveData<List<WorkoutSession>> observeSessions(String userId) {
        return dao.observeAll(userId);
    }

    public void saveSession(WorkoutSession session, Runnable onSaved) {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            try {
                dao.insert(session);
            } catch (SQLiteConstraintException e) {
                // The plan was replaced while the workout ran: keep the history anyway
                session.planId = null;
                dao.insert(session);
            }
            if (onSaved != null) {
                mainHandler.post(onSaved);
            }
        });
    }

    public void deleteSession(WorkoutSession session) {
        FitTrackerDatabase.dbExecutor.execute(() -> dao.delete(session));
    }
}