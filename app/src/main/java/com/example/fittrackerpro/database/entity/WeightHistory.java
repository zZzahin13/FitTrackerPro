package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "weight_history", indices = {@Index(value = {"userId", "date"})})
public class WeightHistory {
    @PrimaryKey
    @NonNull
    public String recordId = UUID.randomUUID().toString();

    public String userId;
    public double weightKg;
    public long date;

    public boolean isSynced;
    public long updatedAt;
}