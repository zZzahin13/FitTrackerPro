package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "plan_exercises",
        foreignKeys = {
                // Deleting a plan removes its schedule rows (they belong to the plan)
                @ForeignKey(entity = WorkoutPlan.class, parentColumns = "planId",
                        childColumns = "planId", onDelete = ForeignKey.CASCADE),
                // An exercise that is part of a plan cannot be deleted by accident
                @ForeignKey(entity = Exercise.class, parentColumns = "exerciseId",
                        childColumns = "exerciseId", onDelete = ForeignKey.RESTRICT)
        },
        indices = {@Index("planId"), @Index("exerciseId")})
public class PlanExercise {
    @PrimaryKey
    @NonNull
    public String planExerciseId = UUID.randomUUID().toString();

    @NonNull
    public String planId = "";
    @NonNull
    public String exerciseId = "";

    public int dayOfWeek;        // 1 = Monday ... 7 = Sunday
    public int sets;
    public int repetitions;
    public int restSeconds;
    public long completedOn;     // 0 = not completed yet
}