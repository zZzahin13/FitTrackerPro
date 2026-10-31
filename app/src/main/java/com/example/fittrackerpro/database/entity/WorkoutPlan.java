package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "workout_plans", indices = {@Index("userId")})
public class WorkoutPlan {
    @PrimaryKey
    @NonNull
    public String planId = UUID.randomUUID().toString();

    public String userId;
    public String planName;
    public int durationMonths;   // 1, 3, 6 or 12
    public long startDate;
    public long endDate;
    public String fitnessGoal;

    public boolean isSynced;
    public long updatedAt;
}