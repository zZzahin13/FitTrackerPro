package com.example.fittrackerpro.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.ExerciseSeeder;
import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.ExerciseDao;
import com.example.fittrackerpro.database.dao.WorkoutPlanDao;
import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.database.entity.PlanExerciseItem;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WorkoutPlan;
import com.example.fittrackerpro.utils.PlanGenerator;

import java.util.List;

public class WorkoutRepository {

    private final FitTrackerDatabase db;
    private final WorkoutPlanDao planDao;
    private final ExerciseDao exerciseDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WorkoutRepository(Context context) {
        db = FitTrackerDatabase.getInstance(context);
        planDao = db.workoutPlanDao();
        exerciseDao = db.exerciseDao();
    }

    public LiveData<WorkoutPlan> observeActivePlan(String userId) {
        return planDao.observeActivePlan(userId);
    }

    public LiveData<List<DaySummary>> observeDaySummaries(String planId, long weekStart) {
        return planDao.observeDaySummaries(planId, weekStart);
    }

    public LiveData<List<PlanExerciseItem>> observeDayExercises(String planId, int dayOfWeek) {
        return planDao.observeDayExercises(planId, dayOfWeek);
    }

    public void setExerciseCompleted(String planExerciseId, boolean completed) {
        FitTrackerDatabase.dbExecutor.execute(() ->
                planDao.setCompletedOn(planExerciseId,
                        completed ? System.currentTimeMillis() : 0));
    }

    public void createPlan(UserProfile profile, int months, Runnable onDone) {
        FitTrackerDatabase.dbExecutor.execute(() -> {
            // The catalog must exist before a plan can use it
            if (exerciseDao.count() == 0) {
                exerciseDao.insertAll(ExerciseSeeder.createExercises());
            }
            PlanGenerator.Result result =
                    PlanGenerator.generate(profile, exerciseDao.getAllOnce(), months);

            // One transaction: either the whole new plan is saved, or nothing changes
            db.runInTransaction(() -> {
                planDao.deletePlansForUser(profile.userId);
                planDao.insertPlan(result.plan);
                planDao.insertPlanExercises(result.items);
            });

            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }
}