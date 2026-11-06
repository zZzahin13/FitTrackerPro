package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.ActivityLog;

import java.util.List;

@Dao
public interface ActivityLogDao {

    // REPLACE lets "Undo" put a deleted entry back with its old id
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ActivityLog log);

    @Delete
    void delete(ActivityLog log);

    @Query("SELECT * FROM activity_logs WHERE userId = :userId "
            + "AND date >= :rangeStart AND date < :rangeEnd ORDER BY date DESC")
    LiveData<List<ActivityLog>> observeRange(String userId, long rangeStart, long rangeEnd);
}