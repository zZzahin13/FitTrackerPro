package com.example.fittrackerpro.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "workouts")
public class Workout {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type;
    public int durationMinutes;
    public int calories;
    public double distanceKm;
    public long dateMillis;

    // Room uses this one
    public Workout() {
    }

    // We use this one when creating a workout in code
    @Ignore
    public Workout(String type, int durationMinutes, int calories,
                   double distanceKm, long dateMillis) {
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.calories = calories;
        this.distanceKm = distanceKm;
        this.dateMillis = dateMillis;
    }
}