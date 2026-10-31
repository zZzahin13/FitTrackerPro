package com.example.fittrackerpro.database.entity;

import androidx.room.Embedded;

public class ExerciseItem {
    @Embedded
    public Exercise exercise;
    public boolean isFavorite;
}