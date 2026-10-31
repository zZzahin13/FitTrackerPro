package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.database.entity.PlanExercise;
import com.example.fittrackerpro.database.entity.PlanExerciseItem;
import com.example.fittrackerpro.database.entity.WorkoutPlan;

import java.util.List;

@Dao
public interface WorkoutPlanDao {

    @Insert
    void insertPlan(WorkoutPlan plan);

    @Insert
    void insertPlanExercises(List<PlanExercise> items);

    // Cascade deletes the plan's schedule rows; workout history stays (planId becomes null)
    @Query("DELETE FROM workout_plans WHERE userId = :userId")
    void deletePlansForUser(String userId);

    @Query("SELECT * FROM workout_plans WHERE userId = :userId ORDER BY startDate DESC LIMIT 1")
    LiveData<WorkoutPlan> observeActivePlan(String userId);

    // One row per workout day, with how many exercises are done since Monday
    @Query("SELECT pe.dayOfWeek AS dayOfWeek, COUNT(*) AS exerciseCount, "
            + "GROUP_CONCAT(DISTINCT e.muscleGroup) AS muscles, "
            + "SUM(CASE WHEN pe.completedOn >= :weekStart THEN 1 ELSE 0 END) AS completedCount "
            + "FROM plan_exercises pe JOIN exercises e ON e.exerciseId = pe.exerciseId "
            + "WHERE pe.planId = :planId GROUP BY pe.dayOfWeek ORDER BY pe.dayOfWeek")
    LiveData<List<DaySummary>> observeDaySummaries(String planId, long weekStart);

    @Query("SELECT pe.planExerciseId AS planExerciseId, pe.exerciseId AS exerciseId, "
            + "pe.dayOfWeek AS dayOfWeek, pe.sets AS sets, pe.repetitions AS repetitions, "
            + "pe.restSeconds AS restSeconds, pe.completedOn AS completedOn, "
            + "e.name AS name, e.muscleGroup AS muscleGroup, e.difficulty AS difficulty "
            + "FROM plan_exercises pe JOIN exercises e ON e.exerciseId = pe.exerciseId "
            + "WHERE pe.planId = :planId AND pe.dayOfWeek = :dayOfWeek "
            + "ORDER BY e.muscleGroup, e.name")
    LiveData<List<PlanExerciseItem>> observeDayExercises(String planId, int dayOfWeek);

    @Query("UPDATE plan_exercises SET completedOn = :completedOn WHERE planExerciseId = :id")
    void setCompletedOn(String id, long completedOn);
}