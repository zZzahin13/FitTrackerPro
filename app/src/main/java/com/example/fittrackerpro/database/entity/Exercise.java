package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "exercises")
public class Exercise {
    @PrimaryKey
    @NonNull
    public String exerciseId = UUID.randomUUID().toString();

    public String name;
    public String muscleGroup;
    public String description;
    public int sets;
    public int repetitions;
    public int restSeconds;
    public String difficulty;
    public String location;   // Home, Gym, or Both
    public String imageUrl;
}