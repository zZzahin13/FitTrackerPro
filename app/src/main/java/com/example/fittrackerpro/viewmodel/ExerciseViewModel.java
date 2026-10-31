package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.fittrackerpro.database.entity.ExerciseItem;
import com.example.fittrackerpro.repository.ExerciseRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ExerciseViewModel extends AndroidViewModel {

    private static class Filter {
        final String query;
        final String muscle;
        final boolean favoritesOnly;

        Filter(String query, String muscle, boolean favoritesOnly) {
            this.query = query;
            this.muscle = muscle;
            this.favoritesOnly = favoritesOnly;
        }
    }

    private final ExerciseRepository repository;
    private final String userId;
    private final MutableLiveData<Filter> filter = new MutableLiveData<>();
    private final LiveData<List<ExerciseItem>> exercises;
    private Filter current = new Filter("", "All", false);

    public ExerciseViewModel(@NonNull Application application) {
        super(application);
        repository = new ExerciseRepository(application);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        repository.ensureSeeded();
        filter.setValue(current);

        // Whenever the filter changes, run the query again with the new values
        exercises = Transformations.switchMap(filter,
                f -> repository.search(userId, f.query, f.muscle, f.favoritesOnly));
    }

    public LiveData<List<ExerciseItem>> getExercises() {
        return exercises;
    }

    public LiveData<ExerciseItem> getItem(String exerciseId) {
        return repository.observeItem(userId, exerciseId);
    }

    public void setQuery(String query) {
        current = new Filter(query, current.muscle, current.favoritesOnly);
        filter.setValue(current);
    }

    public void setMuscle(String muscle) {
        current = new Filter(current.query, muscle, current.favoritesOnly);
        filter.setValue(current);
    }

    public void setFavoritesOnly(boolean favoritesOnly) {
        current = new Filter(current.query, current.muscle, favoritesOnly);
        filter.setValue(current);
    }

    public String getMuscle() {
        return current.muscle;
    }

    public boolean isFavoritesOnly() {
        return current.favoritesOnly;
    }

    public void toggleFavorite(ExerciseItem item) {
        repository.setFavorite(userId, item.exercise.exerciseId, !item.isFavorite);
    }
}