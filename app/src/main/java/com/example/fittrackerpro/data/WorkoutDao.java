package com.example.fittrackerpro.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert
    long insert(Workout workout);

    @Delete
    void delete(Workout workout);

    @Query("SELECT * FROM workouts ORDER BY dateMillis DESC")
    LiveData<List<Workout>> getAllWorkouts();
}