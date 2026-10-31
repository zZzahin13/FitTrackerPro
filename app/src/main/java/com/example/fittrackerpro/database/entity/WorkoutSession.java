package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

// If a plan is deleted, its sessions stay (planId becomes null): history is never lost.
@Entity(tableName = "workout_sessions",
        foreignKeys = @ForeignKey(entity = WorkoutPlan.class, parentColumns = "planId",
                childColumns = "planId", onDelete = ForeignKey.SET_NULL),
        indices = {@Index("planId"), @Index(value = {"userId", "date"})})
public class WorkoutSession {
    @PrimaryKey
    @NonNull
    public String sessionId = UUID.randomUUID().toString();

    public String userId;
    @Nullable
    public String planId;
    public long date;
    public int durationMinutes;
    public double caloriesBurned;
    public boolean completed;

    public boolean isSynced;
    public long updatedAt;
}