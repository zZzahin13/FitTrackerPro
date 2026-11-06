package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.WeightHistory;

import java.util.List;

@Dao
public interface WeightHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeightHistory record);

    @Delete
    void delete(WeightHistory record);

    @Query("SELECT * FROM weight_history WHERE userId = :userId ORDER BY date ASC")
    LiveData<List<WeightHistory>> observeAllAsc(String userId);

    @Query("SELECT * FROM weight_history WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<WeightHistory>> observeAllDesc(String userId);

    @Query("SELECT COUNT(*) FROM weight_history WHERE userId = :userId")
    int countForUser(String userId);

    @Query("SELECT * FROM weight_history WHERE userId = :userId AND isSynced = 0")
    List<WeightHistory> getUnsynced(String userId);

    @Query("UPDATE weight_history SET isSynced = 1 WHERE recordId = :recordId")
    void markSynced(String recordId);

    @Query("SELECT COUNT(*) FROM weight_history WHERE userId = :userId AND isSynced = 0")
    LiveData<Integer> observeUnsyncedCount(String userId);
}
