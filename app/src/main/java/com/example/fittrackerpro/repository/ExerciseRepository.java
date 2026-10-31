package com.example.fittrackerpro.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.ExerciseSeeder;
import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.ExerciseDao;
import com.example.fittrackerpro.database.entity.ExerciseItem;
import com.example.fittrackerpro.database.entity.FavoriteExercise;

import java.util.List;

public class ExerciseRepository {

    private final ExerciseDao dao;

    public ExerciseRepository(Context context) {
        dao = FitTrackerDatabase.getInstance(context).exerciseDao();
    }

    // Fills the catalog the first time the library is opened
    public void ensureSeeded() {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            if (dao.count() == 0) {
                dao.insertAll(ExerciseSeeder.createExercises());
            }
        });
    }

    public LiveData<List<ExerciseItem>> search(String userId, String query,
                                               String muscle, boolean favoritesOnly) {
        return dao.search(userId, query, muscle, favoritesOnly);
    }

    public LiveData<ExerciseItem> observeItem(String userId, String exerciseId) {
        return dao.observeItem(userId, exerciseId);
    }

    public void setFavorite(String userId, String exerciseId, boolean favorite) {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            if (favorite) {
                FavoriteExercise f = new FavoriteExercise();
                f.userId = userId;
                f.exerciseId = exerciseId;
                dao.addFavorite(f);
            } else {
                dao.removeFavorite(userId, exerciseId);
            }
        });
    }
}