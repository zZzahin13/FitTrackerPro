package com.example.fittrackerpro.database.entity;

import androidx.room.Embedded;

public class FoodLogItem {
    @Embedded
    public FoodLog log;

    public String foodName;
    public String servingSize;
    public double kcalPerServing;
    public double proteinPerServing;
    public double carbsPerServing;
    public double fatPerServing;
}