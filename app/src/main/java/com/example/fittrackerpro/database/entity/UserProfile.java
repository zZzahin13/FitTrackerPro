package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey
    @NonNull
    public String userId = "";   // Firebase UID

    public String name;
    public int age;
    public String gender;
    public double heightCm;
    public double currentWeightKg;
    public double targetWeightKg;
    public String fitnessGoal;
    public String experience;
    public String activityLevel;
    public String dietaryPreference;
    public String workoutLocation;
    public int workoutMinutes;
    public int workoutDaysPerWeek;
    public String limitations;
    public boolean profileCompleted;

    public boolean isSynced;
    public long updatedAt;
}