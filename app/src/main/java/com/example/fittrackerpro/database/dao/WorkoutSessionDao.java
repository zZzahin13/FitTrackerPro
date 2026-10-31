package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.WorkoutSession;

import java.util.List;

@Dao
public interface WorkoutSessionDao {

    // REPLACE lets "Undo" put a deleted session back with its old id
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkoutSession session);

    @Delete
    void delete(WorkoutSession session);

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<WorkoutSession>> observeAll(String userId);
}