package com.example.fittrackerpro.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "water_logs", indices = {@Index(value = {"userId", "date"})})
public class WaterLog {
    @PrimaryKey
    @NonNull
    public String waterId = UUID.randomUUID().toString();

    public String userId;
    public int amountMl;
    public long date;

    public boolean isSynced;
    public long updatedAt;
}