package com.example.fittrackerpro.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.fittrackerpro.database.dao.ExerciseDao;
import com.example.fittrackerpro.database.dao.UserProfileDao;
import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.Exercise;
import com.example.fittrackerpro.database.entity.FavoriteExercise;
import com.example.fittrackerpro.database.entity.FoodLog;
import com.example.fittrackerpro.database.entity.Meal;
import com.example.fittrackerpro.database.entity.PlanExercise;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WaterLog;
import com.example.fittrackerpro.database.entity.WeightHistory;
import com.example.fittrackerpro.database.entity.WorkoutPlan;
import com.example.fittrackerpro.database.entity.WorkoutSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        UserProfile.class, Exercise.class, WorkoutPlan.class, PlanExercise.class,
        FavoriteExercise.class, WorkoutSession.class, Meal.class, FoodLog.class,
        ActivityLog.class, WeightHistory.class, WaterLog.class},
        version = 1, exportSchema = false)
public abstract class FitTrackerDatabase extends RoomDatabase {

    public abstract UserProfileDao userProfileDao();
    public abstract ExerciseDao exerciseDao();

    private static volatile FitTrackerDatabase INSTANCE;

    // All writes run here, never on the main thread
    public static final ExecutorService dbExecutor = Executors.newFixedThreadPool(2);

    public static FitTrackerDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FitTrackerDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    FitTrackerDatabase.class,
                                    "fittrackerpro.db")
                            .fallbackToDestructiveMigration()   // development only
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}