package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.Exercise;
import com.example.fittrackerpro.database.entity.ExerciseItem;
import com.example.fittrackerpro.database.entity.FavoriteExercise;

import java.util.List;

@Dao
public interface ExerciseDao {

    // IGNORE = skip rows that already exist, so seeding twice never duplicates
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Exercise> exercises);

    @Query("SELECT COUNT(*) FROM exercises")
    int count();

    // One query handles search text, muscle group, and favorites-only.
    // LEFT JOIN adds the "isFavorite" column for the current user.
    @Query("SELECT e.*, (f.exerciseId IS NOT NULL) AS isFavorite "
            + "FROM exercises e "
            + "LEFT JOIN favorite_exercises f "
            + "ON f.exerciseId = e.exerciseId AND f.userId = :userId "
            + "WHERE (:muscle = 'All' OR e.muscleGroup = :muscle) "
            + "AND e.name LIKE '%' || :query || '%' "
            + "AND (:favoritesOnly = 0 OR f.exerciseId IS NOT NULL) "
            + "ORDER BY e.name")
    LiveData<List<ExerciseItem>> search(String userId, String query,
                                        String muscle, boolean favoritesOnly);

    @Query("SELECT e.*, (f.exerciseId IS NOT NULL) AS isFavorite "
            + "FROM exercises e "
            + "LEFT JOIN favorite_exercises f "
            + "ON f.exerciseId = e.exerciseId AND f.userId = :userId "
            + "WHERE e.exerciseId = :exerciseId")
    LiveData<ExerciseItem> observeItem(String userId, String exerciseId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addFavorite(FavoriteExercise favorite);

    @Query("DELETE FROM favorite_exercises WHERE userId = :userId AND exerciseId = :exerciseId")
    void removeFavorite(String userId, String exerciseId);
}