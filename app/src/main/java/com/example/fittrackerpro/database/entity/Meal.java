package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "meals")
public class Meal {
    @PrimaryKey
    @NonNull
    public String mealId = UUID.randomUUID().toString();

    public String name;
    public String servingSize;
    public double calories;
    public double protein;
    public double carbohydrates;
    public double fat;
}