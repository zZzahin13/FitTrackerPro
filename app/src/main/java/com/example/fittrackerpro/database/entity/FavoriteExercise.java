package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "favorite_exercises",
        primaryKeys = {"userId", "exerciseId"},
        foreignKeys = @ForeignKey(entity = Exercise.class, parentColumns = "exerciseId",
                childColumns = "exerciseId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("exerciseId")})
public class FavoriteExercise {
    @NonNull
    public String userId = "";
    @NonNull
    public String exerciseId = "";
}