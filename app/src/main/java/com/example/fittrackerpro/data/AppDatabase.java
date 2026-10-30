package com.example.fittrackerpro.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Workout.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WorkoutDao workoutDao();

    private static volatile AppDatabase INSTANCE;

    // Inserts and deletes must run off the main thread
    public static final ExecutorService dbExecutor = Executors.newFixedThreadPool(2);

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "fittracker.db").build();
                }
            }
        }
        return INSTANCE;
    }
}