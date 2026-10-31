package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "activity_logs", indices = {@Index(value = {"userId", "date"})})
public class ActivityLog {
    @PrimaryKey
    @NonNull
    public String activityId = UUID.randomUUID().toString();

    public String userId;
    public String activityType;
    public int durationMinutes;
    public double distanceKm;
    public int steps;
    public double caloriesBurned;
    public long date;

    public boolean isSynced;
    public long updatedAt;
}