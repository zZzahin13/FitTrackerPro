package com.example.fittrackerpro.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.UserProfileDao;
import com.example.fittrackerpro.database.entity.UserProfile;

import java.util.function.Consumer;

public class UserRepository {

    private final UserProfileDao dao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public UserRepository(Context context) {
        dao = FitTrackerDatabase.getInstance(context).userProfileDao();
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
        profile.updatedAt = System.currentTimeMillis();
        profile.isSynced = false;   // the Day 8 sync will upload it
        FitTrackerDatabase.dbExecutor.execute(() -> {
            dao.upsert(profile);
            if (onSaved != null) {
                mainHandler.post(onSaved);
            }
        });
    }
}