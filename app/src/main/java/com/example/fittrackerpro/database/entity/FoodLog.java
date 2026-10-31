package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

// A food that has been logged cannot be deleted from the catalog (RESTRICT).
@Entity(tableName = "food_logs",
        foreignKeys = @ForeignKey(entity = Meal.class, parentColumns = "mealId",
                childColumns = "mealId", onDelete = ForeignKey.RESTRICT),
        indices = {@Index("mealId"), @Index(value = {"userId", "date"})})
public class FoodLog {
    @PrimaryKey
    @NonNull
    public String logId = UUID.randomUUID().toString();

    public String userId;
    @NonNull
    public String mealId = "";
    public double quantity;
    public long date;
    public String mealType;     // Breakfast, Lunch, Dinner, Snack
    public double totalCalories;

    public boolean isSynced;
    public long updatedAt;
}